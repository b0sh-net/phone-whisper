package net.b0sh.audiotext

/**
 * In-memory [TranscriptRepository] used by JVM tests (no Android, no device).
 * Mirrors the observable behaviour of the SQLite implementation.
 */
class InMemoryTranscriptRepository : TranscriptRepository {

    private val items = mutableListOf<ArchivedTranscript>()
    private var nextId = 1L

    override fun add(createdAtMillis: Long, durationMillis: Long, text: String): Long {
        val id = nextId++
        items.add(ArchivedTranscript(id, createdAtMillis, durationMillis, text))
        return id
    }

    override fun update(id: Long, durationMillis: Long, text: String) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) {
            val existing = items[index]
            items[index] = existing.copy(durationMillis = durationMillis, text = text)
        }
    }

    override fun list(): List<ArchivedTranscript> =
        items.sortedWith(
            compareByDescending<ArchivedTranscript> { it.createdAtMillis }
                .thenByDescending { it.id },
        )

    override fun delete(id: Long) {
        items.removeAll { it.id == id }
    }
}
