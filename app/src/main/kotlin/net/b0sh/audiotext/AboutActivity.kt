package net.b0sh.audiotext

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = vertical(dp(24)).apply { setPadding(dp(24), dp(48), dp(24), dp(32)) }

        val header = TextView(this).apply {
            text = string(R.string.about_title)
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(16))
        }
        root.addView(header)

        root.addView(paragraph(R.string.about_intro))
        root.addView(paragraph(R.string.about_source_code))
        root.addView(paragraph(R.string.about_issues))
        root.addView(paragraph(R.string.about_support))

        val reviewBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = string(R.string.intro_review_button)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) }
            setOnClickListener {
                startActivity(Intent(this@AboutActivity, IntroActivity::class.java)
                    .putExtra(IntroActivity.EXTRA_ORIGIN, IntroActivity.ORIGIN_MANUAL))
            }
        }
        root.addView(reviewBtn)

        val closeBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = string(R.string.action_close)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(32) }
            setOnClickListener { finish() }
        }
        root.addView(closeBtn)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(attrColor(android.R.attr.colorBackground))
            addView(root)
        })
    }

    private fun paragraph(resId: Int) = TextView(this).apply {
        text = Html.fromHtml(string(resId), Html.FROM_HTML_MODE_LEGACY)
        textSize = 14f
        setTextColor(attrColor(android.R.attr.textColorSecondary))
        setPadding(0, 0, 0, dp(16))
        movementMethod = LinkMovementMethod.getInstance()
        setLinkTextColor(attrColor(com.google.android.material.R.attr.colorPrimary))
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
}