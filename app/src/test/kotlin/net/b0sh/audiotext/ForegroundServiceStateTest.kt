package net.b0sh.audiotext

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The `microphone` foreground service (and therefore its ongoing notification)
 * must exist exactly while a session is recording: it disappears on stop and on
 * error, so no orphan notice can survive (FR-005/FR-010, SC-005).
 */
class ForegroundServiceStateTest {

    @Test
    fun `a recording session requires the foreground service`() {
        assertEquals(
            ForegroundServiceState.Running,
            foregroundServiceState(LivePhase.Recording),
        )
    }

    @Test
    fun `an idle session has no foreground service`() {
        assertEquals(ForegroundServiceState.Stopped, foregroundServiceState(LivePhase.Idle))
    }

    @Test
    fun `an errored session has no foreground service`() {
        assertEquals(ForegroundServiceState.Stopped, foregroundServiceState(LivePhase.Error))
    }

    @Test
    fun `only the recording phase keeps the service alive`() {
        LivePhase.entries.forEach { phase ->
            val expected =
                if (phase == LivePhase.Recording) {
                    ForegroundServiceState.Running
                } else {
                    ForegroundServiceState.Stopped
                }
            assertEquals("phase $phase", expected, foregroundServiceState(phase))
        }
    }
}
