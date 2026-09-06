package net.b0sh.audiotext

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.radiobutton.MaterialRadioButton
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var statusIcon: ImageView
    private lateinit var statusSubtitle: TextView
    private lateinit var modelContainer: LinearLayout
    private lateinit var infoSection: TextView
    private var downloading = false

    private val modelRows = mutableMapOf<String, ModelRowViews>()

    private data class ModelRowViews(
        val radio: MaterialRadioButton,
        val progress: LinearProgressIndicator,
        val subtitle: TextView,
        val dlBtn: MaterialButton,
        val delBtn: MaterialButton
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = vertical(0, 0)

        // Top large header
        val header = TextView(this).apply {
            text = "Audio To Text"
            textSize = 32f
            setPadding(dp(24), dp(64), dp(24), dp(24))
        }
        root.addView(header)

        // Informational section with variable content based on model state
        infoSection = TextView(this).apply {
            textSize = 14f
            setPadding(dp(24), 0, dp(24), dp(16))
            setTextColor(attrColor(android.R.attr.textColorSecondary))
        }
        root.addView(infoSection)

        // Status row
        statusIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { rightMargin = dp(16) }
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setBackgroundResource(R.drawable.status_icon_bg)
            setImageResource(R.drawable.status_ready)
        }
        val statusRow = settingsRow(string(R.string.status_label), string(R.string.status_ready), leading = statusIcon)
        statusSubtitle = statusRow.findViewWithTag("subtitle")
        root.addView(statusRow)

        // Local Models section
        modelContainer = vertical(0)
        modelContainer.addView(sectionHeader(string(R.string.section_local_models)))
        for (m in MODEL_CATALOG) modelContainer.addView(buildModelRow(m))
        root.addView(modelContainer)

        // About button
        root.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = string(R.string.about_button)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(16)
                leftMargin = dp(24)
                rightMargin = dp(24)
            }
            setOnClickListener { startActivity(Intent(this@MainActivity, AboutActivity::class.java)) }
        })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(attrColor(android.R.attr.colorBackground))
            addView(root)
        })

        // Onboarding: proponi l'introduzione solo alla prima apertura.
        if (IntroFlag.shouldShow(prefs())) {
            startActivity(Intent(this, IntroActivity::class.java).putExtra(IntroActivity.EXTRA_ORIGIN, IntroActivity.ORIGIN_FIRST_OPEN))
            IntroFlag.markShown(prefs())
        }

        // Load model in background if needed
        thread { initLocalModel() }

        refresh()
    }

    private fun initLocalModel(): Boolean {
        val modelName = prefs().getString("model_name", "")
        runOnUiThread { setStatus(R.string.status_initializing_model) }

        val t = TranscriberManager.getOrCreateTranscriber(this)
        if (t != null) {
            val name = MODEL_CATALOG.find { it.archive == modelName }?.name ?: modelName ?: "Unknown"
            runOnUiThread { setStatus(R.string.status_local_model_ready, name) }
            return true
        }

        runOnUiThread { setStatus(R.string.status_no_local_model) }
        return false
    }

    // --- UI Logic (mostly unchanged but adapted) ---

    private fun buildModelRow(model: Model): View {
        val radio = MaterialRadioButton(this).apply { isClickable = false }
        val dlBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle).apply { text = "↓" }
        val delBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle).apply {
            text = "🗑"
            visibility = View.GONE
            setOnClickListener { onModelDelete(model) }
        }
        val progress = LinearProgressIndicator(this).apply { visibility = View.GONE }
        val rightContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(delBtn); addView(dlBtn); addView(radio)
        }
        val row = settingsRow(model.name, string(R.string.model_size_mb, string(model.qualityRes), model.sizeMb), rightContainer) { onModelAction(model) }
        val textContainer = row.getChildAt(0) as LinearLayout
        textContainer.addView(progress)
        modelRows[model.archive] = ModelRowViews(radio, progress, textContainer.findViewWithTag("subtitle"), dlBtn, delBtn)
        return row
    }

    private fun onModelAction(model: Model) {
        if (ModelDownloader.isInstalled(this, model)) {
            prefs().edit().putString("model_name", model.archive).apply()
            TranscriberManager.reset()
            thread {
                val success = initLocalModel()
                runOnUiThread {
                    if (success) setStatus(R.string.status_active_model, model.name)
                    refresh()
                }
            }
            return
        }
        val views = modelRows[model.archive] ?: return
        views.dlBtn.isEnabled = false
        views.progress.visibility = View.VISIBLE
        views.progress.isIndeterminate = false
        views.subtitle.text = string(R.string.subtitle_downloading, 0)
        downloading = true
        refresh()
        ModelDownloader.download(this, model) { state ->
            runOnUiThread {
                when (state) {
                    is DownloadState.Downloading -> {
                        views.progress.progress = (state.progress * 100).toInt()
                        views.subtitle.text = string(R.string.subtitle_downloading, (state.progress * 100).toInt())
                    }
                    is DownloadState.Extracting -> {
                        views.progress.isIndeterminate = true
                        views.subtitle.text = if (state.currentFile.isBlank())
                            string(R.string.subtitle_installing)
                        else
                            string(R.string.subtitle_installing_file, state.currentFile)
                        setStatus(R.string.status_installing_model, model.name)
                    }
                    is DownloadState.Done -> {
                        downloading = false
                        updateInfoSection()
                        views.progress.isIndeterminate = false
                        views.progress.visibility = View.GONE
                        views.subtitle.text = string(R.string.subtitle_installed)
                        setStatus(R.string.status_model_installed, model.name)
                        prefs().edit().putString("model_name", model.archive).apply()
                        TranscriberManager.reset()
                        // Wait for model to actually load before refreshing UI
                        thread {
                            val success = initLocalModel()
                            runOnUiThread {
                                if (success) setStatus(R.string.status_model_ready, model.name)
                                else setStatus(R.string.status_model_load_failed)
                                refresh()
                            }
                        }
                    }

                    is DownloadState.Error -> {
                        downloading = false
                        views.progress.isIndeterminate = false
                        views.progress.visibility = View.GONE
                        views.subtitle.text = string(R.string.model_size_mb, string(model.qualityRes), model.sizeMb)
                        views.dlBtn.isEnabled = true
                        setStatus(R.string.status_download_failed, state.message)
                        updateInfoSection()
                    }
                }
            }
        }
    }

    private fun refresh() {
        val activeModel = prefs().getString("model_name", "")
        MODEL_CATALOG.forEach { m ->
            val views = modelRows[m.archive] ?: return@forEach
            val installed = ModelDownloader.isInstalled(this, m)
            views.radio.isChecked = activeModel == m.archive
            views.radio.visibility = if (installed) View.VISIBLE else View.GONE
            views.dlBtn.visibility = if (installed) View.GONE else View.VISIBLE
            views.delBtn.visibility = if (installed) View.VISIBLE else View.GONE
        }
        updateInfoSection()
    }

    private fun onModelDelete(model: Model) {
        MaterialAlertDialogBuilder(this)
            .setTitle(string(R.string.delete_model_title))
            .setMessage(string(R.string.delete_model_message, model.name, model.sizeMb))
            .setPositiveButton(string(R.string.action_delete)) { _, _ -> deleteModel(model) }
            .setNegativeButton(string(R.string.action_cancel), null)
            .show()
    }

    private fun deleteModel(model: Model) {
        val views = modelRows[model.archive] ?: return
        val wasActive = prefs().getString("model_name", "") == model.archive
        if (wasActive) {
            prefs().edit().remove("model_name").apply()
            TranscriberManager.reset()
        }
        views.delBtn.isEnabled = false
        setStatus(R.string.status_removing_model, model.name)
        thread {
            ModelDownloader.delete(this, model)
            runOnUiThread {
                views.delBtn.isEnabled = true
                views.subtitle.text = string(R.string.model_size_mb, string(model.qualityRes), model.sizeMb)
                setStatus(R.string.status_model_removed, model.name)
                refresh()
            }
        }
    }

    private fun updateInfoSection() {
        infoSection.text = when {
            downloading -> string(R.string.info_downloading)
            MODEL_CATALOG.any { ModelDownloader.isInstalled(this, it) } -> string(R.string.info_model_ready)
            else -> string(R.string.info_no_model)
        }
    }

    private fun setStatus(@StringRes res: Int, vararg args: Any) {
        statusSubtitle.text = string(res, *args)
        statusIcon.setImageResource(statusDrawable(res))
    }

    private fun statusDrawable(@StringRes res: Int): Int = when (res) {
        R.string.status_ready -> R.drawable.status_ready
        R.string.status_initializing_model -> R.drawable.status_initializing_model
        R.string.status_local_model_ready -> R.drawable.status_local_model_ready
        R.string.status_no_local_model -> R.drawable.status_no_local_model
        R.string.status_active_model -> R.drawable.status_active_model
        R.string.status_installing_model -> R.drawable.status_installing_model
        R.string.status_model_installed -> R.drawable.status_model_installed
        R.string.status_model_ready -> R.drawable.status_model_ready
        R.string.status_model_load_failed -> R.drawable.status_model_load_failed
        R.string.status_download_failed -> R.drawable.status_download_failed
        R.string.status_removing_model -> R.drawable.status_removing_model
        R.string.status_model_removed -> R.drawable.status_model_removed
        else -> R.drawable.status_ready
    }

    private fun settingsRow(title: String, subtitle: String, widget: View? = null, leading: View? = null, onClick: (() -> Unit)? = null) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(24), dp(16), dp(24), dp(16))
        if (onClick != null) {
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
            setOnClickListener { onClick() }
        }
        if (leading != null) addView(leading)
        val textContainer = vertical(0).apply { layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        textContainer.addView(TextView(this@MainActivity).apply { text = title; textSize = 18f })
        textContainer.addView(TextView(this@MainActivity).apply { tag = "subtitle"; text = subtitle; textSize = 14f })
        addView(textContainer)
        if (widget != null) addView(widget)
    }

    private fun sectionHeader(title: String) = TextView(this).apply {
        text = title; textSize = 14f; setTypeface(null, Typeface.BOLD)
        setTextColor(attrColor(com.google.android.material.R.attr.colorPrimary))
        setPadding(dp(24), dp(24), dp(24), dp(8))
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
    private fun prefs() = getSharedPreferences("audiotext", MODE_PRIVATE)
}