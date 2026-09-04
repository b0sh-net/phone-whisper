package net.b0sh.audiotext

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

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

        val root = vertical(dp(24), dp(48))

        val header = TextView(this).apply {
            text = string(R.string.intro_title)
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(header)

        // Carousel orizzontale: le tre immagini affiancate in uno ScrollView con snap a pagina.
        val pages = ScrollView(this).apply {
            isFillViewport = false
        }
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val maxImgW = dp(320)
        for (i in 1..3) {
            val drawableId = resources.getIdentifier(IntroAssets.name(locale(), i), "drawable", getPackageName())
            strip.addView(ImageView(this).apply {
                setImageResource(drawableId)
                adjustViewBounds = true
                setMaxWidth(maxImgW)
            })
        }
        pages.addView(strip)
        root.addView(pages)

        // Indicatore di pagina (stile punti).
        val dots = TextView(this).apply {
            text = "● ○ ○"
            textSize = 12f
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(dots)

        val closeBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = string(R.string.action_close)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24) }
            setOnClickListener { finish() }
        }
        root.addView(closeBtn)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(attrColor(android.R.attr.colorBackground))
            addView(root)
        })
    }

    /** Lingua attiva del dispositivo, da cui deriva il set di immagini. */
    private fun locale() =
        resources.configuration.getLocales().get(0)

    private fun vertical(padH: Int, padV: Int = padH) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(padH, padV, padH, padV)
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun attrColor(attr: Int): Int {
        val ta = obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, 0); ta.recycle(); return color
    }

    private fun string(resId: Int, vararg args: Any): String =
        getString(resId, *args)

    private fun prefs() = getSharedPreferences(IntroFlag.PREFS_NAME, MODE_PRIVATE)
}