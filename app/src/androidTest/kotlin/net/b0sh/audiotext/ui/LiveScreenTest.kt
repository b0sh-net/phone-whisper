package net.b0sh.audiotext.ui

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.b0sh.audiotext.LivePhase
import net.b0sh.audiotext.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [LiveScreen] (contract C7, Constitution IV): the screen is
 * driven purely by state + callbacks, so no microphone is involved.
 */
@RunWith(AndroidJUnit4::class)
class LiveScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun str(id: Int): String = context.getString(id)

    private fun render(
        phase: LivePhase,
        currentText: String = "",
        archiveAvailable: Boolean = true,
        interruptionNoticeRes: Int? = null,
        onDismissInterruption: () -> Unit = {},
        onOpenArchive: () -> Unit = {},
    ) {
        composeRule.setContent {
            LiveScreen(
                phase = phase,
                currentText = currentText,
                archiveAvailable = archiveAvailable,
                errorMessageRes = null,
                interruptionNoticeRes = interruptionNoticeRes,
                onStart = {},
                onStop = {},
                onDismissInterruption = onDismissInterruption,
                onOpenArchive = onOpenArchive,
            )
        }
    }

    @Test
    fun showsTitle() {
        render(LivePhase.Idle)
        composeRule.onNodeWithText(str(R.string.live_title)).assertExists()
    }

    @Test
    fun idleShowsStartAndNotStop() {
        render(LivePhase.Idle)
        composeRule.onNodeWithContentDescription(str(R.string.live_start)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.live_stop)).assertDoesNotExist()
    }

    @Test
    fun recordingShowsStopAndNotStart() {
        render(LivePhase.Recording)
        composeRule.onNodeWithContentDescription(str(R.string.live_stop)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.live_start)).assertDoesNotExist()
    }

    @Test
    fun showsCurrentText() {
        render(LivePhase.Recording, currentText = "ciao mondo")
        composeRule.onNodeWithText("ciao mondo").assertExists()
    }

    @Test
    fun archiveDisabledWhileRecording() {
        render(LivePhase.Recording, archiveAvailable = false)
        composeRule.onNodeWithText(str(R.string.live_archive)).assertIsNotEnabled()
    }

    @Test
    fun archiveEnabledAndClickableWhenIdle() {
        var clicked = false
        render(LivePhase.Idle, archiveAvailable = true, onOpenArchive = { clicked = true })
        composeRule.onNodeWithText(str(R.string.live_archive)).assertIsEnabled().performClick()
        assertTrue(clicked)
    }

    @Test
    fun showsInterruptionNoticeWithoutBlockingStart() {
        render(LivePhase.Idle, interruptionNoticeRes = R.string.live_interrupted_background)
        composeRule.onNodeWithText(str(R.string.live_interrupted_background)).assertExists()
        composeRule.onNodeWithText(str(R.string.live_start)).assertIsEnabled()
    }

    @Test
    fun hidesInterruptionNoticeWhenNoInterruptionHappened() {
        render(LivePhase.Idle)
        composeRule.onNodeWithText(str(R.string.live_interrupted_background)).assertDoesNotExist()
    }

    @Test
    fun dismissInterruptionInvokesCallbackOncePerClick() {
        var dismissed = 0
        render(
            LivePhase.Idle,
            interruptionNoticeRes = R.string.live_interrupted_background,
            onDismissInterruption = { dismissed++ },
        )
        composeRule.onNodeWithText(str(R.string.live_interrupted_dismiss)).performClick()
        assertEquals(1, dismissed)
    }
}
