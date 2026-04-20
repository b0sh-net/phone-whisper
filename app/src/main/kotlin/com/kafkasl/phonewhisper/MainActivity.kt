package com.kafkasl.phonewhisper

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.radiobutton.MaterialRadioButton
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var statusSubtitle: TextView
    private lateinit var modelContainer: LinearLayout
    private lateinit var promptContainer: LinearLayout

    private val modelRows = mutableMapOf<String, ModelRowViews>()
    private val promptRows = mutableMapOf<String, PromptRowViews>()

    private data class ModelRowViews(
        val radio: MaterialRadioButton,
        val progress: LinearProgressIndicator,
        val subtitle: TextView,
        val dlBtn: MaterialButton
    )

    private data class PromptRowViews(
        val radio: MaterialRadioButton,
        val subtitle: TextView
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = vertical(0, 0)

        // Top large header
        val header = TextView(this).apply {
            text = "Phone Whisper"
            textSize = 32f
            setPadding(dp(24), dp(64), dp(24), dp(24))
        }
        root.addView(header)

        // Status row
        val statusRow = settingsRow("Status", "Ready")
        statusSubtitle = statusRow.findViewWithTag("subtitle")
        root.addView(statusRow)

        // --- Engine Section ---
        root.addView(sectionHeader("Engine"))
        
        val isCloud = !prefs().getBoolean("use_local", true)
        val cloudSwitch = MaterialSwitch(this).apply {
            isChecked = isCloud
            isClickable = false
        }
        root.addView(settingsRow("Use cloud transcription", "Requires OpenAI API key", cloudSwitch) {
            val newCloud = !cloudSwitch.isChecked
            prefs().edit().putBoolean("use_local", !newCloud).apply()
            cloudSwitch.isChecked = newCloud
            TranscriberManager.reset()
            refresh()
        })

        // Local Models section
        modelContainer = vertical(0)
        modelContainer.addView(sectionHeader("Local models"))
        for (m in MODEL_CATALOG) modelContainer.addView(buildModelRow(m))
        root.addView(modelContainer)

        // --- Post-Processing Section ---
        root.addView(sectionHeader("Post-Processing"))
        
        val isPostProcessing = prefs().getBoolean("use_post_processing", false)
        val postProcessSwitch = MaterialSwitch(this).apply {
            isChecked = isPostProcessing
            isClickable = false
        }
        root.addView(settingsRow("Cleanup transcript", "Uses OpenAI Chat API to fix grammar", postProcessSwitch) {
            val newVal = !postProcessSwitch.isChecked
            prefs().edit().putBoolean("use_post_processing", newVal).apply()
            postProcessSwitch.isChecked = newVal
            refresh()
        })

        promptContainer = vertical(0)
        for (preset in promptPresets()) promptContainer.addView(buildPromptRow(preset))
        root.addView(promptContainer)

        // --- Settings Section ---
        root.addView(sectionHeader("Settings"))
        root.addView(settingsRow("OpenAI API Key", "Tap to set") { promptApiKey() })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(attrColor(android.R.attr.colorBackground))
            addView(root)
        })

        // Load model in background if needed
        thread { initLocalModel() }

        refresh()
    }

    private fun initLocalModel(): Boolean {
        val modelName = prefs().getString("model_name", "")
        runOnUiThread { statusSubtitle.text = "Initializing model..." }

        val t = TranscriberManager.getOrCreateTranscriber(this)
        if (t != null) {
            val name = MODEL_CATALOG.find { it.archive == modelName }?.name ?: modelName ?: "Unknown"
            runOnUiThread { statusSubtitle.text = "Local model ready: $name" }
            return true
        }

        runOnUiThread { statusSubtitle.text = "No local model installed" }
        return false
    }

    // --- UI Logic (mostly unchanged but adapted) ---

    private fun buildModelRow(model: Model): View {
        val radio = MaterialRadioButton(this).apply { isClickable = false }
        val dlBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle).apply { text = "↓" }
        val progress = LinearProgressIndicator(this).apply { visibility = View.GONE }
        val rightContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(dlBtn); addView(radio)
        }
        val row = settingsRow(model.name, "${model.quality} · ${model.sizeMb} MB", rightContainer) { onModelAction(model) }
        val textContainer = row.getChildAt(0) as LinearLayout
        textContainer.addView(progress)
        modelRows[model.archive] = ModelRowViews(radio, progress, textContainer.findViewWithTag("subtitle"), dlBtn)
        return row
    }

    private fun onModelAction(model: Model) {
        if (ModelDownloader.isInstalled(this, model)) {
            prefs().edit().putString("model_name", model.archive).apply()
            TranscriberManager.reset()
            thread {
                val success = initLocalModel()
                runOnUiThread {
                    if (success) statusSubtitle.text = "Active model: ${model.name}"
                    refresh()
                }
            }
            return
        }
        val views = modelRows[model.archive] ?: return
        views.dlBtn.isEnabled = false
        views.progress.visibility = View.VISIBLE
        ModelDownloader.download(this, model) { state ->
            runOnUiThread {
                when (state) {
                    is DownloadState.Downloading -> views.progress.progress = (state.progress * 100).toInt()
                    is DownloadState.Extracting -> views.progress.isIndeterminate = true
                    is DownloadState.Done -> {
                        views.progress.visibility = View.GONE
                        statusSubtitle.text = "Model installed: ${model.name}"
                        prefs().edit().putString("model_name", model.archive).apply()
                        TranscriberManager.reset()
                        // Wait for model to actually load before refreshing UI
                        thread {
                            val success = initLocalModel()
                            runOnUiThread {
                                if (success) statusSubtitle.text = "Model ready: ${model.name}"
                                else statusSubtitle.text = "Failed to load model"
                                refresh()
                            }
                        }
                    }

                    is DownloadState.Error -> {
                        views.progress.visibility = View.GONE
                        views.dlBtn.isEnabled = true
                        statusSubtitle.text = "Download failed"
                    }
                }
            }
        }
    }

    private fun buildPromptRow(preset: PromptPreset): View {
        val radio = MaterialRadioButton(this).apply { isClickable = false }
        val row = settingsRow(preset.title, preset.subtitle, radio) {
            prefs().edit().putString("post_processing_prompt", preset.prompt).apply()
            refresh()
        }
        promptRows[preset.key] = PromptRowViews(radio, row.findViewWithTag("subtitle"))
        return row
    }

    private fun refresh() {
        val useLocal = prefs().getBoolean("use_local", true)
        val usePostProcessing = prefs().getBoolean("use_post_processing", false)
        modelContainer.visibility = if (useLocal) View.VISIBLE else View.GONE
        promptContainer.visibility = if (usePostProcessing) View.VISIBLE else View.GONE
        
        val activeModel = prefs().getString("model_name", "")
        MODEL_CATALOG.forEach { m ->
            val views = modelRows[m.archive] ?: return@forEach
            val installed = ModelDownloader.isInstalled(this, m)
            views.radio.isChecked = activeModel == m.archive
            views.radio.visibility = if (installed) View.VISIBLE else View.GONE
            views.dlBtn.visibility = if (installed) View.GONE else View.VISIBLE
        }
        
        val currentPrompt = prefs().getString("post_processing_prompt", PostProcessor.DEFAULT_PROMPT)
        promptPresets().forEach { p ->
            promptRows[p.key]?.radio?.isChecked = currentPrompt == p.prompt
        }
    }

    private fun promptApiKey() {
        val input = EditText(this).apply { setText(prefs().getString("api_key", "")) }
        android.app.AlertDialog.Builder(this).setTitle("OpenAI API Key").setView(input).setPositiveButton("Save") { _, _ ->
            prefs().edit().putString("api_key", input.text.toString().trim()).apply(); refresh()
        }.show()
    }

    private fun settingsRow(title: String, subtitle: String, widget: View? = null, onClick: (() -> Unit)? = null) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(24), dp(16), dp(24), dp(16))
        if (onClick != null) {
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
            setOnClickListener { onClick() }
        }
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
    private fun prefs() = getSharedPreferences("phonewhisper", MODE_PRIVATE)
    
    private data class PromptPreset(val key: String, val title: String, val subtitle: String, val prompt: String)
    private fun promptPresets() = listOf(
        PromptPreset("dev", "Dev cleanup", "Best for coding", PostProcessor.DEV_PROMPT),
        PromptPreset("simple", "Simple cleanup", "Grammar & punctuation", PostProcessor.SIMPLE_PROMPT)
    )
}
