package net.b0sh.audiotext

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Losing visibility ends the session, a configuration change does not (FR-004/FR-005,
 * FR-011): the host `onStop` is delivered in both cases, so the rule must separate
 * "the app is gone" from "the activity is being recreated".
 */
class HostVisibilityStopTest {

    @Test
    fun `a recording session ends when the host is no longer visible`() {
        assertTrue(stopsOnHostStopped(LivePhase.Recording, isChangingConfigurations = false))
    }

    @Test
    fun `a recording session survives a configuration change`() {
        assertFalse(stopsOnHostStopped(LivePhase.Recording, isChangingConfigurations = true))
    }

    @Test
    fun `an idle session is unaffected by visibility loss`() {
        assertFalse(stopsOnHostStopped(LivePhase.Idle, isChangingConfigurations = false))
        assertFalse(stopsOnHostStopped(LivePhase.Idle, isChangingConfigurations = true))
    }

    @Test
    fun `an errored session is unaffected by visibility loss`() {
        assertFalse(stopsOnHostStopped(LivePhase.Error, isChangingConfigurations = false))
        assertFalse(stopsOnHostStopped(LivePhase.Error, isChangingConfigurations = true))
    }

    @Test
    fun `only a visible-to-invisible transition while recording stops the session`() {
        LivePhase.entries.forEach { phase ->
            val expected = phase == LivePhase.Recording
            assertTrue("phase $phase", stopsOnHostStopped(phase, false) == expected)
            assertFalse("phase $phase", stopsOnHostStopped(phase, true))
        }
    }
}
