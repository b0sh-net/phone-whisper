package net.b0sh.audiotext.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.b0sh.audiotext.ArchivedTranscript
import net.b0sh.audiotext.R
import net.b0sh.audiotext.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for [LiveArchiveScreen] (contract C7, Constitution IV):
 * empty state, entries with duration, back, review, share and delete confirmation.
 */
@RunWith(AndroidJUnit4::class)
class LiveArchiveScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun str(id: Int): String = context.getString(id)

    private val items = listOf(
        ArchivedTranscript(1, 1_700_000_000_000, 62_000, "prima trascrizione"),
        ArchivedTranscript(2, 1_700_000_500_000, 5_000, "seconda trascrizione"),
    )

    private fun renderArchive(
        list: List<ArchivedTranscript> = items,
        onBack: () -> Unit = {},
        onShare: (ArchivedTranscript) -> Unit = {},
        onDelete: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            LiveArchiveScreen(
                items = list,
                onBack = onBack,
                onShare = onShare,
                onDelete = onDelete,
            )
        }
    }

    @Test
    fun emptyStateIsShown() {
        renderArchive(list = emptyList())
        composeRule.onNodeWithText(str(R.string.live_archive_empty)).assertIsDisplayed()
    }

    @Test
    fun showsEntriesWithTheirDuration() {
        renderArchive()
        composeRule.onNodeWithText(formatDuration(62_000)).assertIsDisplayed()
        composeRule.onNodeWithText(formatDuration(5_000)).assertIsDisplayed()
    }

    @Test
    fun backInvokesCallback() {
        var backInvoked = false
        renderArchive(onBack = { backInvoked = true })

        composeRule.onNodeWithText(str(R.string.back_button)).performClick()

        assertTrue(backInvoked)
    }

    @Test
    fun reviewShowsFullText() {
        renderArchive()

        composeRule.onAllNodesWithContentDescription(str(R.string.live_action_review))[0].performClick()

        composeRule.onNodeWithText("prima trascrizione").assertIsDisplayed()
    }

    @Test
    fun shareInvokesCallbackWithEntry() {
        var shared: ArchivedTranscript? = null
        renderArchive(onShare = { shared = it })

        composeRule.onAllNodesWithContentDescription(str(R.string.live_action_share))[0].performClick()

        assertEquals("prima trascrizione", shared?.text)
    }

    @Test
    fun deleteAsksConfirmationThenInvokes() {
        var deleted: Long? = null
        renderArchive(onDelete = { deleted = it })

        composeRule.onAllNodesWithContentDescription(str(R.string.live_action_delete))[0].performClick()
        composeRule.onNodeWithText(str(R.string.live_delete_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.live_action_delete)).performClick()

        assertEquals(1L, deleted)
    }
}
