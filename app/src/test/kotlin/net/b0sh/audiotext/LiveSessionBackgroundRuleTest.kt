package net.b0sh.audiotext

import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Regression guard for FR-001/FR-002/FR-003: moving the app to the background
 * (screen lock, app switch) MUST NOT end a live session — only an explicit user
 * stop or one of the documented error conditions may.
 */
class LiveSessionBackgroundRuleTest {

    @Test
    fun `an idle session is not stopped by the host going to background`() {
        assertFalse(stopsOnHostBackground(LivePhase.Idle))
    }

    @Test
    fun `a recording session keeps running when the host goes to background`() {
        assertFalse(stopsOnHostBackground(LivePhase.Recording))
    }

    @Test
    fun `an errored session is not changed by the host going to background`() {
        assertFalse(stopsOnHostBackground(LivePhase.Error))
    }

    @Test
    fun `every phase survives the host going to background`() {
        LivePhase.entries.forEach { phase ->
            assertFalse("phase $phase must survive background", stopsOnHostBackground(phase))
        }
    }
}
