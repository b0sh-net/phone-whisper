package net.b0sh.audiotext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the Live capability badge rule (FR-003, FR-009, FR-011; contract C1).
 */
class LiveCapabilityBadgeTest {

    @Test
    fun streamingModelShowsMic() {
        assertEquals(LiveBadge.Mic, liveBadgeFor(true))
    }

    @Test
    fun nonStreamingModelShowsMicOff() {
        assertEquals(LiveBadge.MicOff, liveBadgeFor(false))
    }

    @Test
    fun unknownCapabilityShowsNoBadge() {
        assertEquals(LiveBadge.None, liveBadgeFor(null))
    }

    @Test
    fun everyCapabilityMapsToADistinctBadge() {
        val badges = listOf(true, false, null).map { liveBadgeFor(it) }
        assertEquals(3, badges.toSet().size)
    }

    @Test
    fun badgeMatchesLiveGateForEveryCatalogModel() {
        val streamingIds = MODEL_CATALOG.filter { it.streaming }.map { it.id }.toSet()

        for (model in MODEL_CATALOG) {
            // The model is selected and installed: the Live tab is usable exactly
            // when the badge says the model supports live transcription.
            val liveUsable = canUseLive(model.id, setOf(model.id), streamingIds)
            val badge = liveBadgeFor(model.streaming)
            assertEquals("model ${model.id}", liveUsable, badge == LiveBadge.Mic)
        }
    }

    @Test
    fun catalogCoversBothCapabilities() {
        // Both surfaces must be able to show both glyphs, otherwise the rule is
        // only exercised on one branch in the app.
        assertTrue(MODEL_CATALOG.any { liveBadgeFor(it.streaming) == LiveBadge.Mic })
        assertTrue(MODEL_CATALOG.any { liveBadgeFor(it.streaming) == LiveBadge.MicOff })
    }
}
