package net.b0sh.audiotext

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * While recording, the other app sections must be unreachable: the only way out of
 * the Live screen is the explicit stop (FR-008, SC-007).
 */
class NavigationLockTest {

    @Test
    fun `navigation is locked while recording`() {
        assertTrue(navigationLocked(LivePhase.Recording))
    }

    @Test
    fun `navigation is free when no session runs`() {
        assertFalse(navigationLocked(LivePhase.Idle))
    }

    @Test
    fun `navigation is free after an error`() {
        assertFalse(navigationLocked(LivePhase.Error))
    }

    @Test
    fun `only the recording phase locks navigation`() {
        LivePhase.entries.forEach { phase ->
            assertEqualsLock(phase, phase == LivePhase.Recording)
        }
    }

    private fun assertEqualsLock(phase: LivePhase, expected: Boolean) {
        if (expected) {
            assertTrue("phase $phase", navigationLocked(phase))
        } else {
            assertFalse("phase $phase", navigationLocked(phase))
        }
    }
}
