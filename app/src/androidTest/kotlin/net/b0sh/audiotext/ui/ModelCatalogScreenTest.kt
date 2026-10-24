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
 * Compose UI tests for the Live capability badge in the model catalog
 * (US1, contract C2/C4/C6).
 */
@RunWith(AndroidJUnit4::class)
class ModelCatalogScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun str(id: Int, vararg args: Any): String = context.getString(id, *args)

    private val streamingRow = CatalogModelRow(
        id = "kroko-128l-it",
        name = "Kroko - Italiano",
        sizeText = "154 MB",
        supportsLive = true,
    )
    private val plainRow = CatalogModelRow(
        id = "whisper-base-en",
        name = "Whisper Base - English",
        sizeText = "145 MB",
        supportsLive = false,
    )

    private fun render(
        rows: List<CatalogModelRow>,
        phases: Map<String, DownloadPhase> = emptyMap(),
        onDownload: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            ModelCatalogScreen(
                state = CatalogScreenState(rows, phases),
                onDownload = onDownload,
            )
        }
    }

    @Test
    fun streamingRowShowsMicBadge() {
        render(listOf(streamingRow))
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_supported)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_unsupported)).assertDoesNotExist()
    }

    @Test
    fun nonStreamingRowShowsMicOffBadge() {
        render(listOf(plainRow))
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_unsupported)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_supported)).assertDoesNotExist()
    }

    @Test
    fun eachRowShowsItsOwnBadge() {
        render(listOf(streamingRow, plainRow))
        composeRule.onAllNodesWithContentDescription(str(R.string.live_badge_supported))
            .assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription(str(R.string.live_badge_unsupported))
            .assertCountEquals(1)
    }

    @Test
    fun badgeSitsAfterTheSizeText() {
        render(listOf(streamingRow))
        val text = composeRule.onNodeWithText("154 MB").getUnclippedBoundsInRoot()
        val badge = composeRule
            .onNodeWithContentDescription(str(R.string.live_badge_supported))
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
    fun badgeStaysVisibleWhileDownloading() {
        render(
            listOf(streamingRow),
            phases = mapOf(streamingRow.id to DownloadPhase.Downloading(0.42f, null)),
        )
        composeRule.onNodeWithText(str(R.string.subtitle_downloading, 42)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_supported)).assertExists()
    }

    @Test
    fun badgeStaysVisibleWhileInstalling() {
        render(
            listOf(streamingRow),
            phases = mapOf(streamingRow.id to DownloadPhase.Extracting("")),
        )
        composeRule.onNodeWithText(str(R.string.subtitle_installing)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_supported)).assertExists()
    }

    @Test
    fun badgeDoesNotBlockTheDownloadButton() {
        var downloaded: String? = null
        render(listOf(streamingRow), onDownload = { downloaded = it })

        composeRule.onNodeWithContentDescription(str(R.string.action_download)).performClick()

        assertEquals(streamingRow.id, downloaded)
    }

    @Test
    fun emptyCatalogKeepsItsEmptyState() {
        render(emptyList())
        composeRule.onNodeWithText(str(R.string.models_catalog_empty)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_supported)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(str(R.string.live_badge_unsupported)).assertDoesNotExist()
    }
}
