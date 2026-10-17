package net.b0sh.audiotext

/**
 * Pure (Android-free) contract and helpers for the Live transcription archive.
 *
 * Kept free of any `android.*` import so the archive logic (repository contract,
 * duration formatting, save rule, live gate) is unit-testable in plain JVM,
 * without a device or emulator (see FR-020 and research.md R4).
 */

/** A persisted live transcription. `id == 0` means "not yet persisted". */
data class ArchivedTranscript(
    val id: Long,
    val createdAtMillis: Long,
    val durationMillis: Long,
    val text: String,
)

/**
 * Archive of live transcriptions. Implementations: [SqliteTranscriptRepository]
 * (Android) and the in-memory fake used by tests.
 */
interface TranscriptRepository {
    /** Insert an entry; returns the assigned id. */
    fun add(createdAtMillis: Long, durationMillis: Long, text: String): Long

    /** Update an existing entry's duration and text (used by periodic autosave and final stop). */
    fun update(id: Long, durationMillis: Long, text: String)

    /** All entries, most recent first (createdAtMillis DESC, then id DESC). */
    fun list(): List<ArchivedTranscript>

    /** Remove the entry with the given id (no-op when absent). */
    fun delete(id: Long)
}

/**
 * Format a duration in minutes and seconds, e.g. `0 -> "0:00"`, `62_000 -> "1:02"`,
 * `3_725_000 -> "62:05"`. Rounds to the nearest second; negative values clamp to zero.
 * Minutes are not wrapped at 60 (a 62-minute session shows `62:05`).
 */
fun formatDuration(durationMillis: Long): String {
    val clamped = if (durationMillis < 0L) 0L else durationMillis
    val totalSeconds = (clamped + 500L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

/** True if and only if the text is not empty after trimming. */
fun shouldArchive(text: String): Boolean = text.trim().isNotEmpty()

/**
 * Gate for the Live tab: enabled only when the selected model exists, is installed
 * and supports streaming (FR-002 / SC-008).
 */
fun canUseLive(
    selectedId: String?,
    installedIds: Set<String>,
    streamingIds: Set<String>,
): Boolean =
    !selectedId.isNullOrBlank() &&
        selectedId in installedIds &&
        selectedId in streamingIds
