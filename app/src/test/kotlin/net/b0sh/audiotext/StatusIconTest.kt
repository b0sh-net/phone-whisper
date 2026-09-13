package net.b0sh.audiotext

import net.b0sh.audiotext.ui.statusDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusIconTest {

    private val statusList = listOf(
        R.string.status_ready,
        R.string.status_initializing_model,
        R.string.status_local_model_ready,
        R.string.status_no_local_model,
        R.string.status_active_model,
        R.string.status_installing_model,
        R.string.status_model_installed,
        R.string.status_model_ready,
        R.string.status_model_load_failed,
        R.string.status_download_failed,
        R.string.status_removing_model,
        R.string.status_model_removed,
    )

    @Test
    fun `first status maps to ready icon`() {
        assertEquals(R.drawable.status_ready, statusDrawable(R.string.status_ready))
    }

    @Test fun `every known status maps to a non-zero drawable`() {
        statusList.forEach { res ->
            val icon = statusDrawable(res)
            assertNotEquals("status res $res must not map to 0", 0, icon)
            assertTrue("status res $res must be a valid drawable", icon > 0)
        }
    }

    @Test fun `unknown status falls back to ready icon`() {
        assertEquals(R.drawable.status_ready, statusDrawable(R.string.status_label))
    }
}