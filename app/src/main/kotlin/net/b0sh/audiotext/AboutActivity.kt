package net.b0sh.audiotext

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import net.b0sh.audiotext.ui.AboutScreen
import net.b0sh.audiotext.ui.theme.AudioToTextTheme

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val paragraphs = listOf(
            string(R.string.about_intro),
            string(R.string.about_source_code),
            string(R.string.about_issues),
            string(R.string.about_support),
        )

        setContent {
            AudioToTextTheme {
                AboutScreen(
                    paragraphs = paragraphs,
                    onReviewIntro = {
                        startActivity(
                            Intent(this@AboutActivity, IntroActivity::class.java)
                                .putExtra(IntroActivity.EXTRA_ORIGIN, IntroActivity.ORIGIN_MANUAL)
                        )
                    },
                    onClose = { finish() },
                )
            }
        }
    }

    private fun string(@StringRes resId: Int, vararg args: Any): String =
        getString(resId, *args)
}