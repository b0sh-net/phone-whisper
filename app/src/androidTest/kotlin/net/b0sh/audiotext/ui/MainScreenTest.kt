package net.b0sh.audiotext.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.b0sh.audiotext.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the Live capability badge in the home screen
 * (US2, contract C2/C4/C6).
 */
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun str(id: Int): String = context.getString(id)

    private val streamingRow = InstalledModelRow(
        id = "kroko-128l-it",
        name = "Kroko - Italiano",
        sizeText = "154 MB",
        sizeMb = 154,
        isActive = true,
        supportsLive = true,
    )
    private val plainRow = InstalledModelRow(
        id = "whisper-base-en",
        name = "Whisper Base - English",
        sizeText = "145 MB",
        sizeMb = 145,
        isActive = false,
        supportsLive = false,
    )

    private fun render(
        rows: List<InstalledModelRow>,
        switchingModelId: String? = null,
        onSelectModel: (String) -> Unit = {},
        onDeleteModel: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            MainScreen(
                state = MainScreenUiState(
                    infoText = "info",
                    statusText = "stato",
                    statusIconRes = R.drawable.ic_info,
                    installedModels = rows,
                ),
                switchingModelId = switchingModelId,
                onSelectModel = onSelectModel,
                onDeleteModel = onDeleteModel,
            )
        }
    }

    @Test
    fun eachInstalledRowShowsItsOwnBadge() {
        render(listOf(streamingRow, plainRow))
        composeRule.onAllNodesWithContentDescription(str(R.string.live_badge_supported))
            .assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription(str(R.string.live_badge_unsupported))
            .assertCountEquals(1)
    }

    @Test
    fun badgeSitsAfterTheSizeText() {
        render(listOf(streamingRow))
        // The row is clickable, so Compose merges its children semantics: use the
        // unmerged tree to measure the badge itself instead of the whole row.
        val text = composeRule.onNodeWithText("154 MB", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val badge = composeRule
            .onNodeWithContentDescription(str(R.string.live_badge_supported), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        assertTrue(
            "badge (${badge.left}) should be right of the size text (${text.right})",
            badge.left.value >= text.right.value,
        )
        assertTrue(
            "badge should stay on the same line as the size text",
            badge.top.value < text.bottom.value,
        )
    }

    @Test
    fun badgeDoesNotDependOnTheActiveModel() {
        // The active model is the non-streaming one: its badge must still say so.
        render(listOf(plainRow.copy(isActive = true), streamingRow.copy(isActive = false)))
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_unsupported)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_supported)).assertExists()
    }

    @Test
    fun rowIsStillSelectableThroughTheBadgeArea() {
        var selected: String? = null
        render(listOf(streamingRow), onSelectModel = { selected = it })

        composeRule.onNodeWithText(streamingRow.name).performClick()

        assertEquals(streamingRow.id, selected)
    }

    @Test
    fun deleteButtonRemainsReachable() {
        render(listOf(streamingRow))
        composeRule.onAllNodesWithContentDescription(str(R.string.action_delete))
            .assertCountEquals(1)
    }

    @Test
    fun noBadgesWithoutInstalledModels() {
        render(emptyList())
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_supported)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_unsupported)).assertDoesNotExist()
    }
}
