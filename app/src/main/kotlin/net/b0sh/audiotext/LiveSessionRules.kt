package net.b0sh.audiotext

/**
 * Pure (Android-free) rules for the live transcription session lifecycle.
 *
 * Kept free of any `android.*` import so the decisions that can silently regress
 * — is the screen kept awake, does losing visibility stop the session, is the
 * navigation locked, when is the interruption notice shown, which archive row is
 * written — are unit-testable in plain JVM without a device or emulator (FR-014).
 */

/** Phase of the live transcription session. */
enum class LivePhase { Idle, Recording, Error }

/** What [LiveSessionArchive] must do with the current transcription text. */
sealed interface ArchiveWrite {
    /** Nothing to persist: the session produced no text yet and has no row. */
    object None : ArchiveWrite

    /** First non-empty text of the session: create the archive row. */
    object Insert : ArchiveWrite

    /** Autosave or final save on the row already created for this session. */
    data class Update(val id: Long) : ArchiveWrite
}

/** Required state of the device screen while a live session runs (FR-001/FR-002). */
enum class ScreenOnState { KeptOn, Normal }

/**
 * Why a live session ended.
 *
 * The cause only decides whether the user gets the interruption notice: `User`
 * (explicit stop) and `Error` (permission revoked, microphone busy, model gone)
 * have their own feedback, while `HostNotVisible` (screen locked, Home, another
 * app) needs an explanation on return (FR-006/FR-007).
 */
enum class StopCause { User, HostNotVisible, Error }

/**
 * Whether the screen must be kept awake: exactly while the session records, so the
 * device never goes to standby mid-session and returns to the normal timeout as
 * soon as the session ends (FR-001/FR-002).
 */
fun screenOnState(phase: LivePhase): ScreenOnState =
    if (phase == LivePhase.Recording) ScreenOnState.KeptOn else ScreenOnState.Normal

/**
 * Whether the other app sections must be unreachable: while recording, the only way
 * out of the Live screen is the explicit stop (FR-008).
 */
fun navigationLocked(phase: LivePhase): Boolean = phase == LivePhase.Recording

/**
 * Whether the host no longer being visible must end the session (FR-004/FR-005).
 *
 * `Activity.onStop` delivers this signal both when the app is really gone (screen
 * locked, Home, another app full screen) and when the activity is recreated for a
 * configuration change. Only the first case ends the session: rotating the device
 * must not interrupt the transcription (FR-011).
 */
fun stopsOnHostStopped(phase: LivePhase, isChangingConfigurations: Boolean): Boolean =
    phase == LivePhase.Recording && !isChangingConfigurations

/**
 * Whether ending the session for [cause] must show the interruption notice to the
 * user when they come back (FR-007). Only a session interrupted because the app was
 * no longer visible needs the explanation; a user stop is self-explanatory and an
 * error already has its own localized message.
 */
fun interruptionNoticeShown(cause: StopCause): Boolean = cause == StopCause.HostNotVisible

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

/**
 * Owns the archive row of one live session.
 *
 * A session keeps a single [sessionId] and a single `createdAtMillis` even when it
 * is interrupted, so duration and text always refer to the whole session (FR-006).
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
