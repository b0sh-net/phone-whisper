package net.b0sh.audiotext

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The interruption notice is shown only when the session ended because the app was
 * no longer visible (FR-007): a user stop is self-explanatory and an error has its
 * own localized message.
 */
class InterruptionNoticeRuleTest {

    @Test
    fun `an interruption for lost visibility shows the notice`() {
        assertTrue(interruptionNoticeShown(StopCause.HostNotVisible))
    }

    @Test
    fun `an explicit user stop shows no notice`() {
        assertFalse(interruptionNoticeShown(StopCause.User))
    }

    @Test
    fun `an error shows no notice`() {
        assertFalse(interruptionNoticeShown(StopCause.Error))
    }

    @Test
    fun `only the visibility cause raises the notice`() {
        StopCause.entries.forEach { cause ->
            val expected = cause == StopCause.HostNotVisible
            assertTrue("cause $cause", interruptionNoticeShown(cause) == expected)
        }
    }
}
