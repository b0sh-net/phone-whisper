package net.b0sh.audiotext

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import net.b0sh.audiotext.ui.IntroScreen
import net.b0sh.audiotext.ui.theme.AudioToTextTheme

class IntroActivity : AppCompatActivity() {

    companion object {
        /** Chiave extra per l'origine della visualizzazione. */
        val EXTRA_ORIGIN = "intro_origin"

        /** Origini: `first_open` aggiorna il flag, `manual` no. */
        val ORIGIN_FIRST_OPEN = "first_open"
        val ORIGIN_MANUAL = "manual"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Se mostrata per la prima volta, segna il flag per non riproporla in seguito.
        if (getIntent().getStringExtra(EXTRA_ORIGIN) == ORIGIN_FIRST_OPEN) {
            IntroFlag.markShown(prefs())
        }

        val images = (1..3).map { page -> resolveIntroImage(page) }

        setContent {
            AudioToTextTheme {
                IntroScreen(imageRes = images, onClose = { finish() })
            }
        }
    }

    /** Asset dell'introduzione per la lingua attiva (fallback inglese). */
    @DrawableRes
    private fun resolveIntroImage(page: Int): Int =
        resources.getIdentifier(IntroAssets.name(locale(), page), "drawable", packageName)

    private fun locale() = resources.configuration.getLocales().get(0)

    private fun prefs() = getSharedPreferences(IntroFlag.PREFS_NAME, MODE_PRIVATE)
}