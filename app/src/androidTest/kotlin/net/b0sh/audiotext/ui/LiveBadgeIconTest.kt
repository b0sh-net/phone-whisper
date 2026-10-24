package net.b0sh.audiotext.ui

import android.content.Context
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.b0sh.audiotext.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [LiveBadgeIcon] (contract C3/C7): the indicator is
 * informative, localized and never interactive.
 */
@RunWith(AndroidJUnit4::class)
class LiveBadgeIconTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun str(id: Int): String = context.getString(id)

    private fun render(supportsLive: Boolean?) {
        composeRule.setContent { LiveBadgeIcon(supportsLive = supportsLive) }
    }

    @Test
    fun streamingModelShowsMicWithLabel() {
        render(true)
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_supported)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_unsupported)).assertDoesNotExist()
    }

    @Test
    fun nonStreamingModelShowsMicOffWithLabel() {
        render(false)
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_unsupported)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_supported)).assertDoesNotExist()
    }

    @Test
    fun unknownCapabilityShowsNothing() {
        render(null)
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_supported)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_unsupported)).assertDoesNotExist()
    }

    @Test
    fun badgeIsNotClickable() {
        render(true)
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_supported)).assertHasNoClickAction()
    }

    @Test
    fun micOffBadgeIsNotClickable() {
        render(false)
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_unsupported)).assertHasNoClickAction()
    }
}
