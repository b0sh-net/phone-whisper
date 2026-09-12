package net.b0sh.audiotext

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlin.concurrent.thread

/** Downloadable model catalog: one card per model still to install, with
 *  download progress. The page scrolls as a whole; installed models leave
 *  the list and are managed on the main page. */
class ModelCatalogActivity : AppCompatActivity() {

    private data class Row(
        val card: LinearLayout,
        val progress: LinearProgressIndicator,
        val subtitle: TextView,
        val dlBtn: MaterialButton,
    )

    private val rows = mutableMapOf<String, Row>()
    private lateinit var listContainer: LinearLayout
    private lateinit var emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(attrColor(android.R.attr.colorBackground))
            setPadding(dp(8), dp(40), dp(8), dp(8))
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        topBar.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle).apply {
            text = "←"
            contentDescription = string(R.string.back_button)
            setOnClickListener { finish() }
        })
        topBar.addView(TextView(this).apply {
            text = string(R.string.section_downloadable_models)
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(8), 0, 0, 0)
        })
        root.addView(topBar)

        root.addView(TextView(this).apply {
            text = string(R.string.models_catalog_hint)
            textSize = 14f
            setTextColor(attrColor(android.R.attr.textColorSecondary))
            setPadding(dp(24), dp(4), dp(24), dp(12))
        })

        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        emptyView = TextView(this).apply {
            text = string(R.string.models_catalog_empty)
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(attrColor(android.R.attr.textColorSecondary))
            setPadding(dp(24), dp(32), dp(24), dp(32))
            visibility = View.GONE
        }
        listContainer.addView(emptyView)
        for (m in MODEL_CATALOG) listContainer.addView(buildModelCard(m))

        val scroll = ScrollView(this)
        scroll.addView(listContainer)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.setPadding(dp(8), dp(40), dp(8), bottom + dp(8))
            insets
        }
        refresh()
    }

    private fun buildModelCard(model: Model): View {
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        textCol.addView(TextView(this).apply { text = model.name; textSize = 18f })
        textCol.addView(TextView(this).apply {
            tag = "subtitle"
            text = string(R.string.model_size_mb, string(model.qualityRes), model.sizeMb)
            textSize = 14f
            setTextColor(attrColor(android.R.attr.textColorSecondary))
        })

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(8), dp(12))
        }
        row.addView(textCol)
        val dlBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle).apply {
            text = "↓"
            setOnClickListener { download(model) }
        }
        row.addView(dlBtn)

        val progress = LinearProgressIndicator(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@ModelCatalogActivity, R.drawable.bg_card)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                leftMargin = dp(8); rightMargin = dp(8); topMargin = dp(6); bottomMargin = dp(6)
            }
        }
        card.addView(row)
        card.addView(progress)

        rows[model.id] = Row(card, progress, textCol.findViewWithTag("subtitle"), dlBtn)
        return card
    }

    private fun download(model: Model) {
        val views = rows[model.id] ?: return
        views.dlBtn.isEnabled = false
        views.progress.visibility = View.VISIBLE
        views.progress.isIndeterminate = false
        views.subtitle.text = string(R.string.subtitle_downloading, 0)
        ModelDownloader.download(this, model) { state ->
            runOnUiThread {
                when (state) {
                    is DownloadState.Downloading -> {
                        views.progress.progress = (state.progress * 100).toInt()
                        views.subtitle.text = if (state.currentFile.isNullOrBlank())
                            string(R.string.subtitle_downloading, (state.progress * 100).toInt())
                        else
                            string(R.string.subtitle_downloading_file, (state.progress * 100).toInt(), state.currentFile)
                    }
                    is DownloadState.Extracting -> {
                        views.progress.isIndeterminate = true
                        views.subtitle.text = if (state.currentFile.isBlank())
                            string(R.string.subtitle_installing)
                        else
                            string(R.string.subtitle_installing_file, state.currentFile)
                    }
                    is DownloadState.Done -> {
                        views.progress.isIndeterminate = false
                        views.progress.visibility = View.GONE
                        prefs().edit().putString("model_name", model.id).apply()
                        TranscriberManager.reset()
                        Toast.makeText(this, string(R.string.toast_model_installed, model.name), Toast.LENGTH_SHORT).show()
                        refresh()
                        // Preload the recognizer so transcription is ready
                        // as soon as the user goes back.
                        thread { TranscriberManager.getOrCreateTranscriber(this) }
                    }
                    is DownloadState.Error -> {
                        views.progress.isIndeterminate = false
                        views.progress.visibility = View.GONE
                        views.subtitle.text = string(R.string.model_size_mb, string(model.qualityRes), model.sizeMb)
                        views.dlBtn.isEnabled = true
                        Toast.makeText(this, state.message ?: "Unknown error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun refresh() {
        MODEL_CATALOG.forEach { m ->
            val views = rows[m.id] ?: return@forEach
            val installed = ModelDownloader.isInstalled(this, m)
            views.card.visibility = if (installed) View.GONE else View.VISIBLE
            if (installed) views.progress.visibility = View.GONE
        }
        val anyLeft = MODEL_CATALOG.any { !ModelDownloader.isInstalled(this, it) }
        emptyView.visibility = if (anyLeft) View.GONE else View.VISIBLE
    }

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun attrColor(attr: Int): Int {
        val ta = obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, 0); ta.recycle(); return color
    }
    private fun string(resId: Int, vararg args: Any): String = getString(resId, *args)
    private fun prefs() = getSharedPreferences("audiotext", MODE_PRIVATE)
}
