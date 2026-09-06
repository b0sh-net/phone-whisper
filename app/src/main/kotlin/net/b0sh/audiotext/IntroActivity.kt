package net.b0sh.audiotext

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.HorizontalScrollView
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

        // Carousel orizzontale: le tre immagini affiancate in uno HorizontalScrollView con snap a pagina.
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

        // Indicatore di pagina (stile punti), aggiornato durante lo scorrimento.
        val dots = TextView(this).apply {
            textSize = 12f
            setPadding(0, dp(8), 0, 0)
        }

        val pages = HorizontalScrollView(this).apply {
            addView(strip)
            setOnTouchListener { _, event ->
                // Aggiorna i punti durante il trascinamento e aggancia alla pagina più vicina al rilascio.
                if (event.actionMasked == MotionEvent.ACTION_MOVE || event.actionMasked == MotionEvent.ACTION_UP) {
                    updateDots(dots, strip, activePage(this, strip))
                }
                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    smoothScrollTo(strip.getChildAt(activePage(this, strip)).left, 0)
                }
                false
            }
        }
        updateDots(dots, strip, 0)
        root.addView(pages)
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

    /** Indice (0-based) dell'immagine più vicina al centro della viewport. */
    private fun activePage(hsv: HorizontalScrollView, strip: LinearLayout): Int {
        if (strip.childCount == 0) return 0
        val centerX = hsv.scrollX + hsv.width / 2
        return (0 until strip.childCount)
            .minByOrNull { Math.abs(centerX - (strip.getChildAt(it).left + strip.getChildAt(it).width / 2)) } ?: 0
    }

    /** Aggiorna l'indicatore di pagina (● = attiva, ○ = altre). */
    private fun updateDots(dots: TextView, strip: LinearLayout, active: Int) {
        dots.text = (0 until strip.childCount).joinToString(" ") { if (it == active) "●" else "○" }
    }

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