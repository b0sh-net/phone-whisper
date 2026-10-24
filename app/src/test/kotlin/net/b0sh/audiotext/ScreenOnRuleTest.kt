package net.b0sh.audiotext

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The screen must stay awake exactly while a live session records, and the device
 * must go back to its normal timeout as soon as the session ends (FR-001/FR-002).
 */
class ScreenOnRuleTest {

    @Test
    fun `a recording session keeps the screen awake`() {
        assertEquals(ScreenOnState.KeptOn, screenOnState(LivePhase.Recording))
    }

    @Test
    fun `an idle session leaves the screen to the system timeout`() {
        assertEquals(ScreenOnState.Normal, screenOnState(LivePhase.Idle))
    }

    @Test
    fun `an errored session leaves the screen to the system timeout`() {
        assertEquals(ScreenOnState.Normal, screenOnState(LivePhase.Error))
    }

    @Test
    fun `only the recording phase keeps the screen awake`() {
        LivePhase.entries.forEach { phase ->
            val expected = if (phase == LivePhase.Recording) ScreenOnState.KeptOn else ScreenOnState.Normal
            assertEquals("phase $phase", expected, screenOnState(phase))
        }
    }
}
