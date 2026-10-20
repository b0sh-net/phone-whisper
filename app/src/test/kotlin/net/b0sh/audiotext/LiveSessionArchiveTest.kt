package net.b0sh.audiotext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * One continuous live session (foreground + background + screen-locked phases)
 * MUST map to exactly one archive row, whose duration covers the whole session
 * and whose text is the cumulative transcript (FR-008/FR-009, SC-004).
 */
class LiveSessionArchiveTest {

    private val createdAt = 1_700_000_000_000L

    @Test
    fun `a session without text creates no archive entry`() {
        val repository = InMemoryTranscriptRepository()
        val archive = LiveSessionArchive(repository, createdAt)

        archive.save(durationMillis = 10_000L, text = "")
        archive.save(durationMillis = 20_000L, text = "   ")

        assertTrue(repository.list().isEmpty())
        assertEquals(0L, archive.sessionId)
    }

    @Test
    fun `the first text inserts a row with the session start time`() {
        val repository = InMemoryTranscriptRepository()
        val archive = LiveSessionArchive(repository, createdAt)

        archive.save(durationMillis = 12_000L, text = "ciao")

        val items = repository.list()
        assertEquals(1, items.size)
        assertEquals(createdAt, items[0].createdAtMillis)
        assertEquals(12_000L, items[0].durationMillis)
        assertEquals("ciao", items[0].text)
        assertEquals(items[0].id, archive.sessionId)
    }

    @Test
    fun `autosave and final stop update the same row`() {
        val repository = InMemoryTranscriptRepository()
        val archive = LiveSessionArchive(repository, createdAt)

        archive.save(durationMillis = 10_000L, text = "uno")
        archive.save(durationMillis = 20_000L, text = "uno due")
        archive.save(durationMillis = 30_000L, text = "uno due tre")

        val items = repository.list()
        assertEquals(1, items.size)
        assertEquals(30_000L, items[0].durationMillis)
        assertEquals("uno due tre", items[0].text)
        assertEquals(createdAt, items[0].createdAtMillis)
    }

    @Test
    fun `a session crossing a background phase keeps one row with the total duration`() {
        val repository = InMemoryTranscriptRepository()
        val archive = LiveSessionArchive(repository, createdAt)

        // Foreground: first autosave.
        archive.save(durationMillis = 10_000L, text = "prima del blocco")
        // Background / screen locked: capture continues, no new row is created.
        archive.save(durationMillis = 70_000L, text = "prima del blocco e durante il blocco")
        // Back in foreground, then explicit stop.
        archive.save(durationMillis = 180_000L, text = "prima del blocco, durante il blocco e dopo")

        val items = repository.list()
        assertEquals(1, items.size)
        assertEquals(180_000L, items[0].durationMillis)
        assertEquals("prima del blocco, durante il blocco e dopo", items[0].text)
    }

    @Test
    fun `blank text never overwrites a stored transcript`() {
        val repository = InMemoryTranscriptRepository()
        val archive = LiveSessionArchive(repository, createdAt)

        archive.save(durationMillis = 10_000L, text = "testo utile")
        archive.save(durationMillis = 11_000L, text = "")

        val items = repository.list()
        assertEquals(1, items.size)
        assertEquals("testo utile", items[0].text)
        assertEquals(10_000L, items[0].durationMillis)
    }

    @Test
    fun `a later session gets its own row`() {
        val repository = InMemoryTranscriptRepository()

        val first = LiveSessionArchive(repository, createdAt)
        first.save(durationMillis = 5_000L, text = "prima sessione")

        val second = LiveSessionArchive(repository, createdAt + 60_000L)
        second.save(durationMillis = 8_000L, text = "seconda sessione")

        val items = repository.list()
        assertEquals(2, items.size)
        // Most recent session first.
        assertEquals("seconda sessione", items[0].text)
        assertEquals("prima sessione", items[1].text)
        assertEquals(2L, second.sessionId)
    }
}
