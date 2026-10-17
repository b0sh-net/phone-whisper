package net.b0sh.audiotext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the transcript archive contract (FR-011, FR-020), exercised on the
 * in-memory fake that shares the contract with the SQLite implementation.
 */
class TranscriptRepositoryTest {

    @Test
    fun addAssignsIncreasingIds() {
        val repo = InMemoryTranscriptRepository()
        val first = repo.add(1_000, 5_000, "a")
        val second = repo.add(2_000, 5_000, "b")
        assertTrue(second > first)
    }

    @Test
    fun listOrdersMostRecentFirst() {
        val repo = InMemoryTranscriptRepository()
        repo.add(1_000, 5_000, "old")
        repo.add(3_000, 5_000, "new")
        repo.add(2_000, 5_000, "mid")

        assertEquals(listOf("new", "mid", "old"), repo.list().map { it.text })
    }

    @Test
    fun sameTimestampBreaksTieByIdDescending() {
        val repo = InMemoryTranscriptRepository()
        repo.add(1_000, 5_000, "first")
        repo.add(1_000, 5_000, "second")

        assertEquals(listOf("second", "first"), repo.list().map { it.text })
    }

    @Test
    fun deleteRemovesEntryById() {
        val repo = InMemoryTranscriptRepository()
        val id = repo.add(1_000, 1_000, "x")

        repo.delete(id)

        assertTrue(repo.list().isEmpty())
    }

    @Test
    fun deleteUnknownIdIsNoOp() {
        val repo = InMemoryTranscriptRepository()
        repo.add(1_000, 1_000, "x")

        repo.delete(999)

        assertEquals(1, repo.list().size)
    }

    @Test
    fun entryRoundTripsFields() {
        val repo = InMemoryTranscriptRepository()
        repo.add(1_700_000_000_000, 65_000, "ciao")

        val entry = repo.list().single()
        assertEquals(1_700_000_000_000, entry.createdAtMillis)
        assertEquals(65_000, entry.durationMillis)
        assertEquals("ciao", entry.text)
    }

    @Test
    fun updateReplacesDurationAndTextKeepingId() {
        val repo = InMemoryTranscriptRepository()
        val id = repo.add(1_000, 5_000, "parziale")

        repo.update(id, 12_000, "parziale completo")

        val entry = repo.list().single()
        assertEquals(id, entry.id)
        assertEquals(12_000, entry.durationMillis)
        assertEquals("parziale completo", entry.text)
        assertEquals(1, repo.list().size)
    }

    @Test
    fun updateUnknownIdIsNoOp() {
        val repo = InMemoryTranscriptRepository()
        repo.add(1_000, 5_000, "a")

        repo.update(999, 1_000, "b")

        assertEquals("a", repo.list().single().text)
    }
}
