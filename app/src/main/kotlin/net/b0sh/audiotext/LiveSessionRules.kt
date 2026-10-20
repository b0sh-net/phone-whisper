package net.b0sh.audiotext

/**
 * Pure (Android-free) rules for the live transcription session lifecycle.
 *
 * Kept free of any `android.*` import so the decisions that can silently regress
 * — does going to the background stop the session, which archive row is written,
 * is the microphone foreground service required — are unit-testable in plain JVM
 * without a device or emulator (FR-017).
 */

/** Phase of the live transcription session. */
enum class LivePhase { Idle, Recording, Error }

/** Required state of the `microphone` foreground service for a given phase. */
enum class ForegroundServiceState { Running, Stopped }

/** What [LiveSessionArchive] must do with the current transcription text. */
sealed interface ArchiveWrite {
    /** Nothing to persist: the session produced no text yet and has no row. */
    object None : ArchiveWrite

    /** First non-empty text of the session: create the archive row. */
    object Insert : ArchiveWrite

    /** Autosave or final save on the row already created for this session. */
    data class Update(val id: Long) : ArchiveWrite
}

/**
 * Whether the host (Activity) moving to the background must stop the session.
 *
 * Always `false`: a session ends only on an explicit user stop or on one of the
 * documented error conditions, never because the screen locked or the user
 * switched app (FR-001/FR-002/FR-003).
 */
fun stopsOnHostBackground(phase: LivePhase): Boolean = false

/**
 * Decide how to persist the session text, keeping one row per session: the first
 * non-empty text inserts, every later save (periodic autosave and final stop)
 * updates the same row, so the archive never splits a session (FR-008/FR-009).
 *
 * Blank text never writes: a session that produced no speech creates no row, and
 * an already stored transcript is never overwritten with nothing.
 */
fun archiveWritePlan(sessionId: Long, text: String): ArchiveWrite = when {
    !shouldArchive(text) -> ArchiveWrite.None
    sessionId == 0L -> ArchiveWrite.Insert
    else -> ArchiveWrite.Update(sessionId)
}

/** The `microphone` foreground service must run exactly while the session records. */
fun foregroundServiceState(phase: LivePhase): ForegroundServiceState =
    if (phase == LivePhase.Recording) {
        ForegroundServiceState.Running
    } else {
        ForegroundServiceState.Stopped
    }

/**
 * Owns the archive row of one continuous live session.
 *
 * A session that spans foreground, background and screen-locked phases keeps a
 * single [sessionId] and a single `createdAtMillis`, so duration and text always
 * refer to the whole session (FR-008).
 */
class LiveSessionArchive(
    private val repository: TranscriptRepository,
    private val createdAtMillis: Long,
) {

    /** Id of the archive row for this session (0 until the first non-empty text). */
    var sessionId: Long = 0L
        private set

    /** Persist the current progress; safe to call any number of times. */
    fun save(durationMillis: Long, text: String) {
        when (val plan = archiveWritePlan(sessionId, text)) {
            ArchiveWrite.None -> Unit
            ArchiveWrite.Insert -> sessionId = repository.add(createdAtMillis, durationMillis, text)
            is ArchiveWrite.Update -> repository.update(plan.id, durationMillis, text)
        }
    }
}
