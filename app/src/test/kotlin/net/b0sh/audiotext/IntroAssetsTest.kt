package net.b0sh.audiotext

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class IntroAssetsTest {

    @Test fun `resolves italian locale to it set`() {
        assertEquals("it", IntroAssets.resolve(Locale.ITALIAN))
        assertEquals("intro_it_1", IntroAssets.name(Locale.ITALIAN, 1))
    }

    @Test fun `resolves english locale to en set`() {
        assertEquals("en", IntroAssets.resolve(Locale.ENGLISH))
        assertEquals("intro_en_2", IntroAssets.name(Locale.ENGLISH, 2))
    }

    @Test fun `unsupported locale falls back to english`() {
        assertEquals("en", IntroAssets.resolve(Locale.GERMAN))
        assertEquals("intro_en_3", IntroAssets.name(Locale.GERMAN, 3))
    }
}