package net.b0sh.audiotext

import org.junit.Assert.*
import org.junit.Test

class IntroFlagTest {

    @Test fun `should be shown on first run`() {
        assertTrue(IntroFlag.shouldShow(hasBeenShown = false))
    }

    @Test fun `should not be shown after marking`() {
        assertFalse(IntroFlag.shouldShow(hasBeenShown = true))
    }
}