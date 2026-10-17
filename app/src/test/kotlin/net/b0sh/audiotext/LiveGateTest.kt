package net.b0sh.audiotext

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the Live gate and the "save only non-empty" rule (FR-002, FR-016, FR-020).
 */
class LiveGateTest {

    private val installed = setOf("kroko-128l-it", "sherpa-onnx-whisper-base.en")
    private val streaming = setOf("kroko-128l-it")

    @Test
    fun enabledOnlyForSelectedInstalledStreaming() {
        assertTrue(canUseLive("kroko-128l-it", installed, streaming))
    }

    @Test
    fun disabledWhenSelectedModelIsNotStreaming() {
        assertFalse(canUseLive("sherpa-onnx-whisper-base.en", installed, streaming))
    }

    @Test
    fun disabledWhenSelectedModelIsNotInstalled() {
        assertFalse(canUseLive("kroko-128l-it", setOf("sherpa-onnx-whisper-base.en"), streaming))
    }

    @Test
    fun disabledWhenSelectionIsNullOrBlank() {
        assertFalse(canUseLive(null, installed, streaming))
        assertFalse(canUseLive("", installed, streaming))
        assertFalse(canUseLive("   ", installed, streaming))
    }

    @Test
    fun disabledWhenSelectedModelIsUnknown() {
        assertFalse(canUseLive("does-not-exist", installed, streaming))
    }

    @Test
    fun shouldArchiveRejectsBlankText() {
        assertFalse(shouldArchive(""))
        assertFalse(shouldArchive("   "))
        assertFalse(shouldArchive("\n\t "))
    }

    @Test
    fun shouldArchiveAcceptsText() {
        assertTrue(shouldArchive("ciao"))
        assertTrue(shouldArchive("  ciao  "))
    }
}
