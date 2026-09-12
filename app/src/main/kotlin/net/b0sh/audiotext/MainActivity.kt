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
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var statusIcon: ImageView
    private lateinit var statusSubtitle: TextView
    private lateinit var rootLayout: LinearLayout
    private lateinit var modelContainer: LinearLayout
    private lateinit var localHeader: TextView
    private lateinit var localScroll: MaxHeightScrollView
    private lateinit var localSection: LinearLayout
    private lateinit var infoSection: TextView
    private lateinit var aboutButton: MaterialButton
    private lateinit var downloadButton: MaterialButton

    private val modelRows = mutableMapOf<String, ModelRowViews>()
    private var lastInstalledIds: Set<String>? = null

    private data class ModelRowViews(
        val row: View,
        val radio: MaterialRadioButton,
        val subtitle: TextView,
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

        // Installed models live here, in one internally-scrolling box so the page
        // itself stays fixed; downloads happen in the catalog page.
        modelContainer = vertical(0)
        localHeader = sectionHeader(string(R.string.section_local_models_available))
        modelContainer.addView(localHeader)
        localScroll = MaxHeightScrollView(this, Int.MAX_VALUE).apply {
            addView(vertical(0).also { localSection = it })
        }
        modelContainer.addView(localScroll)
        for (m in MODEL_CATALOG) buildModelRow(m)
        root.addView(modelContainer)

        // Download button: opens the downloadable-models catalog page.
        downloadButton = MaterialButton(this).apply {
            text = string(R.string.models_download_button)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(16)
                leftMargin = dp(24)
                rightMargin = dp(24)
            }
            setOnClickListener { startActivity(Intent(this@MainActivity, ModelCatalogActivity::class.java)) }
        }
        root.addView(downloadButton)

        // About button
        aboutButton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = string(R.string.about_button)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                topMargin = dp(12)
                leftMargin = dp(24)
                rightMargin = dp(24)
            }
            setOnClickListener { startActivity(Intent(this@MainActivity, AboutActivity::class.java)) }
        }
        root.addView(aboutButton)

        // Fixed page: no outer scrolling — only the installed-models box
        // scrolls, inside its own area. Bottom inset keeps the buttons above
        // the navigation bar.
        rootLayout = root
        root.setBackgroundColor(attrColor(android.R.attr.colorBackground))
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            // computeCaps() reads the insets itself (rootWindowInsets) and
            // applies the bottom padding; here we only trigger a recompute.
            scheduleCapsRecompute()
            insets
        }

        // Onboarding: proponi l'introduzione solo alla prima apertura.
        if (IntroFlag.shouldShow(prefs())) {
            startActivity(Intent(this, IntroActivity::class.java).putExtra(IntroActivity.EXTRA_ORIGIN, IntroActivity.ORIGIN_FIRST_OPEN))
            IntroFlag.markShown(prefs())
        }

        // No model installed yet: bring the user straight to the catalog.
        if (MODEL_CATALOG.none { ModelDownloader.isInstalled(this, it) }) {
            startActivity(Intent(this, ModelCatalogActivity::class.java))
        }

        // Load model in background if needed
        thread { initLocalModel() }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the catalog page: pick up installed/removed models.
        refresh()
        thread { initLocalModel() }
    }

    private fun initLocalModel(): Boolean {
        val modelName = prefs().getString("model_name", "")
        runOnUiThread { setStatus(R.string.status_initializing_model) }

        val t = TranscriberManager.getOrCreateTranscriber(this)
        if (t != null) {
            val name = MODEL_CATALOG.find { it.id == modelName }?.name ?: modelName ?: "Unknown"
            runOnUiThread { setStatus(R.string.status_local_model_ready, name) }
            return true
        }

        runOnUiThread { setStatus(R.string.status_no_local_model) }
        return false
    }

    // --- UI Logic (mostly unchanged but adapted) ---

    private fun buildModelRow(model: Model): View {
        val radio = MaterialRadioButton(this).apply { isClickable = false }
        val delBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialIconButtonStyle).apply {
            text = "🗑"
            visibility = View.GONE
            setOnClickListener { onModelDelete(model) }
        }
        val rightContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(delBtn); addView(radio)
        }
        val row = settingsRow(model.name, string(R.string.model_size_mb, string(model.qualityRes), model.sizeMb), rightContainer) { selectModel(model) }
        val textContainer = row.getChildAt(0) as LinearLayout
        modelRows[model.id] = ModelRowViews(row, radio, textContainer.findViewWithTag("subtitle"), delBtn)
        return row
    }

    /** Select the installed model to use for transcription. */
    private fun selectModel(model: Model) {
        prefs().edit().putString("model_name", model.id).apply()
        TranscriberManager.reset()
        thread {
            val success = initLocalModel()
            runOnUiThread {
                if (success) setStatus(R.string.status_active_model, model.name)
                refresh()
            }
        }
    }

    private fun refresh() {
        val activeModel = prefs().getString("model_name", "")
        val installedIds = MODEL_CATALOG
            .filter { ModelDownloader.isInstalled(this, it) }
            .map { it.id }.toSet()
        if (installedIds != lastInstalledIds) {
            lastInstalledIds = installedIds
            repartition(installedIds)
        }
        MODEL_CATALOG.forEach { m ->
            val views = modelRows[m.id] ?: return@forEach
            views.radio.isChecked = activeModel == m.id
            views.delBtn.visibility = if (m.id in installedIds) View.VISIBLE else View.GONE
        }
        val showLocal = installedIds.isNotEmpty()
        localHeader.visibility = if (showLocal) View.VISIBLE else View.GONE
        localScroll.visibility = localHeader.visibility
        updateInfoSection()
        scheduleCapsRecompute()
    }

    /** Show only the installed models in the local box, sorted alphabetically
     *  by display name. Row views are reused across repartitions. */
    private fun repartition(installedIds: Set<String>) {
        localSection.removeAllViews()
        val installed = MODEL_CATALOG.filter { it.id in installedIds }.sortedBy { it.name }
        for (m in installed) localSection.addView(modelRows.getValue(m.id).row)
        localScroll.scrollTo(0, 0)
    }

    /** Recompute the list-box heights once the pending layout pass has run. */
    private fun scheduleCapsRecompute() {
        rootLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                rootLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (rootLayout.rootWindowInsets == null) {
                    // System insets not dispatched yet — try again next pass.
                    rootLayout.post { scheduleCapsRecompute() }
                    return
                }
                computeCaps()
            }
        })
    }

    /** Keep the page fixed (never scrolling): the installed-models box takes the
     *  height left between the status area and the buttons, always above the
     *  system navigation bar. It wraps its content when short and scrolls
     *  internally when long. */
    private fun computeCaps() {
        val bars = rootLayout.rootWindowInsets
            ?.let { WindowInsetsCompat.toWindowInsetsCompat(it) }
            ?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
        if (rootLayout.paddingBottom != bars) rootLayout.setPadding(0, 0, 0, bars)

        val minArea = dp(96)
        val dlLp = downloadButton.layoutParams as ViewGroup.MarginLayoutParams
        val aboutLp = aboutButton.layoutParams as ViewGroup.MarginLayoutParams
        val bottomBlock = downloadButton.height + dlLp.topMargin +
            aboutButton.height + aboutLp.topMargin
        val available = rootLayout.height - bars - modelContainer.top -
            bottomBlock - localHeader.height
        val localH = (0 until localSection.childCount).sumOf { localSection.getChildAt(it).height }
        localScroll.maxHeightPx = if (localH <= available) Int.MAX_VALUE
            else maxOf(available, minArea)
        localScroll.requestLayout()
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
        val views = modelRows[model.id] ?: return
        val wasActive = prefs().getString("model_name", "") == model.id
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

/** ScrollView that grows with its content up to maxHeightPx, then scrolls
 *  internally instead of pushing the rest of the page down. The child is
 *  measured at its full content height (UNSPECIFIED) so the scroll range is
 *  not lost when the content exceeds the cap. */
private class MaxHeightScrollView(context: Context, var maxHeightPx: Int) :
    ScrollView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val child = getChildAt(0)
        if (child == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        child.measure(
            ViewGroup.getChildMeasureSpec(
                widthMeasureSpec, paddingLeft + paddingRight, child.layoutParams.width
            ),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val contentHeight = child.measuredHeight + paddingTop + paddingBottom
        val capped = minOf(contentHeight, maxHeightPx.coerceAtMost((1 shl 30) - 1))
        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(capped, MeasureSpec.getSize(heightMeasureSpec))
            else -> capped
        }
        setMeasuredDimension(
            View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            View.resolveSizeAndState(height, heightMeasureSpec, 0)
        )
    }
}