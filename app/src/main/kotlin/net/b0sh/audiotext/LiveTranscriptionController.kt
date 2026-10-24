package net.b0sh.audiotext

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.concurrent.thread

/**
 * Process-wide controller for live (microphone) transcription.
 *
 * Being a process singleton (like [TranscriberManager]/`ModelDownloadRepository`),
 * the active session survives configuration changes (FR-023): rotating the device
 * does not interrupt capture or lose text. The microphone is used **only while the
 * Live screen is visible**: locking the screen, pressing Home or switching app ends
 * the session, saves the transcript and shows a non-blocking notice on return
 * (FR-004/FR-005/FR-007).
 *
 * Capture uses [AudioRecord] at 16 kHz mono PCM16 (VoiceRecognition source) on a
 * dedicated thread, feeding the existing streaming recognizer via
 * [LocalTranscriber.newLiveSession].
 */
object LiveTranscriptionController {

    private const val TAG = "LiveTranscription"
    private const val SAMPLE_RATE = 16000

    /** Periodic autosave interval, so an abrupt process kill loses at most this much. */
    private const val AUTOSAVE_INTERVAL_MS = 10_000L

    var phase by mutableStateOf(LivePhase.Idle)
        private set
    var currentText by mutableStateOf("")
        private set
    var errorMessageRes by mutableStateOf<Int?>(null)
        private set

    /**
     * Localized explanation shown on return after the session was interrupted
     * because the app was no longer visible (FR-007). Lives in memory only: it
     * survives moving between sections but not closing and reopening the app.
     */
    var interruptionNoticeRes by mutableStateOf<Int?>(null)
        private set

    /** The archive button is usable only when no session is running (FR-010). */
    val archiveAvailable: Boolean get() = phase != LivePhase.Recording

    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null

    @Volatile
    private var stopRequested = false

    private var startedAtElapsed = 0L
    private var createdAtMillis = 0L

    @Volatile
    private var repositoryOverride: TranscriptRepository? = null

    private val repoLock = Any()

    /**
     * Archive row owner of the current session (null until the session starts).
     * One instance per session: periodic autosave and the final stop always write
     * the same row, even when the session spans background/screen-locked phases.
     */
    @Volatile
    private var archive: LiveSessionArchive? = null

    /** Override the archive repository (used by tests). */
    fun setRepository(repo: TranscriptRepository?) {
        repositoryOverride = repo
    }

    private fun repo(ctx: Context): TranscriptRepository {
        repositoryOverride?.let { return it }
        synchronized(repoLock) {
            repositoryOverride?.let { return it }
            return SqliteTranscriptRepository(ctx.applicationContext)
                .also { repositoryOverride = it }
        }
    }

    /**
     * Persist the current progress. The first call creates the session's archive
     * entry; later calls (periodic autosave and final stop) update the same entry,
     * so one session maps to exactly one row (FR-009). Empty text is never saved.
     */
    private fun persistProgress(ctx: Context, durationMillis: Long) {
        val session = archive ?: return
        runCatching {
            synchronized(repoLock) {
                session.save(durationMillis, currentText)
            }
        }.onFailure { Log.e(TAG, "Failed to persist transcript", it) }
    }

    /** Saved transcriptions, most recent first (FR-011). */
    fun transcripts(ctx: Context): List<ArchivedTranscript> = repo(ctx).list()

    /** Delete a saved transcription (FR-012). */
    fun deleteTranscript(ctx: Context, id: Long) {
        repo(ctx).delete(id)
    }

    /** Live gate (FR-002/SC-008), computed from the currently selected model. */
    fun canUseLive(ctx: Context): Boolean {
        val prefs = ctx.getSharedPreferences("audiotext", Context.MODE_PRIVATE)
        val selected = prefs.getString("model_name", "")
        val installed = MODEL_CATALOG.filter { ModelDownloader.isInstalled(ctx, it) }.map { it.id }.toSet()
        val streaming = MODEL_CATALOG.filter { it.streaming }.map { it.id }.toSet()
        return net.b0sh.audiotext.canUseLive(selected, installed, streaming)
    }

    /** Start a live session. Assumes the RECORD_AUDIO permission is already granted. */
    @Synchronized
    fun start(ctx: Context) {
        if (phase == LivePhase.Recording) return

        if (!canUseLive(ctx)) {
            setError(R.string.live_error_no_streaming_model)
            return
        }

        val session = TranscriberManager.getOrCreateTranscriber(ctx)?.newLiveSession()
        if (session == null) {
            setError(R.string.live_error_no_streaming_model)
            return
        }

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) {
            session.close()
            setError(R.string.live_error_mic_busy)
            return
        }

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 4,
            )
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord creation failed", e)
            null
        }

        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            try {
                record?.release()
            } catch (_: Exception) {
            }
            session.close()
            setError(R.string.live_error_mic_busy)
            return
        }

        currentText = ""
        errorMessageRes = null
        interruptionNoticeRes = null
        audioRecord = record
        createdAtMillis = System.currentTimeMillis()
        startedAtElapsed = SystemClock.elapsedRealtime()
        stopRequested = false
        archive = LiveSessionArchive(repo(ctx), createdAtMillis)
        phase = LivePhase.Recording

        try {
            record.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "startRecording failed", e)
            releaseAudio()
            session.close()
            setError(R.string.live_error_mic_busy)
            return
        }

        worker = thread(name = "live-transcription") {
            val buffer = ShortArray(minBuffer)
            var lastAutoSave = SystemClock.elapsedRealtime()
            try {
                while (!stopRequested) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    val samples = FloatArray(read) { buffer[it] / 32768f }
                    currentText = session.accept(samples)

                    // Periodic autosave (~every 10 s) so an abrupt process kill
                    // does not lose the transcript accumulated so far.
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastAutoSave >= AUTOSAVE_INTERVAL_MS) {
                        lastAutoSave = now
                        persistProgress(ctx, now - startedAtElapsed)
                    }
                }
                currentText = session.finish()
            } catch (e: Exception) {
                Log.e(TAG, "Live capture failed", e)
                releaseAudio()
                if (stopRequested) {
                    phase = LivePhase.Idle
                } else {
                    persistProgress(ctx, SystemClock.elapsedRealtime() - startedAtElapsed)
                    setError(R.string.live_error_generic)
                }
            } finally {
                try {
                    session.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Stop the session, releasing the microphone. Idempotent.
     *
     * [cause] only decides whether the user gets the interruption notice when they
     * come back: the session ends because they pressed stop, because the app was no
     * longer visible, or because of an error (FR-006/FR-007).
     */
    @Synchronized
    fun stop(ctx: Context, cause: StopCause = StopCause.User) {
        if (phase != LivePhase.Recording) return
        val durationMillis = SystemClock.elapsedRealtime() - startedAtElapsed
        stopRequested = true
        releaseAudio()
        worker?.let { runCatching { it.join(2000) } }
        worker = null

        // Final upsert: creates the entry if no autosave happened yet, otherwise
        // refreshes the same row with the final text/duration (FR-009/FR-016).
        persistProgress(ctx, durationMillis)

        phase = LivePhase.Idle
        interruptionNoticeRes =
            if (interruptionNoticeShown(cause)) R.string.live_interrupted_background else null
    }

    /** Discard the interruption notice without starting a new session (FR-007). */
    fun dismissInterruption() {
        interruptionNoticeRes = null
    }

    /**
     * Called from `MainActivity.onStop`, i.e. whenever the host activity is no
     * longer visible: screen locked, app switched, Home pressed.
     *
     * In all those cases the session ends: the microphone is never used outside the
     * visible app (FR-004/FR-005). A configuration change also reaches `onStop` but
     * must not interrupt the transcription (FR-011).
     */
    fun onHostStopped(ctx: Context, isChangingConfigurations: Boolean) {
        if (stopsOnHostStopped(phase, isChangingConfigurations)) {
            stop(ctx, StopCause.HostNotVisible)
        }
    }

    fun setError(@StringRes resId: Int) {
        errorMessageRes = resId
        // Un errore ha già il suo messaggio localizzato: l'avviso di interruzione
        // precedente non deve convivere con esso (FR-007).
        interruptionNoticeRes = null
        phase = LivePhase.Error
    }

    fun resetError() {
        if (phase == LivePhase.Error) {
            errorMessageRes = null
            phase = LivePhase.Idle
        }
    }

    private fun releaseAudio() {
        val record = audioRecord
        audioRecord = null
        try {
            if (record?.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
        } catch (_: Exception) {
        }
        try {
            record?.release()
        } catch (_: Exception) {
        }
    }
}
