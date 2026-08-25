package net.b0sh.audiotext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import kotlin.concurrent.thread

class TranscribeActivity : AppCompatActivity() {

    private lateinit var resultContainer: LinearLayout
    private lateinit var resultText: TextView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var statusLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = vertical(dp(24), dp(32))

        // Header
        val header = TextView(this).apply {
            text = string(R.string.title_transcribing)
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(16))
        }
        root.addView(header)

        // Status Label
        statusLabel = TextView(this).apply {
            text = string(R.string.status_initializing)
            textSize = 16f
            setPadding(0, 0, 0, dp(24))
        }
        root.addView(statusLabel)

        // Result Container
        resultContainer = vertical(dp(16)).apply {
            background = ContextCompat.getDrawable(this@TranscribeActivity, android.R.drawable.editbox_dropdown_light_frame)
        }

        progressIndicator = LinearProgressIndicator(this).apply {
            isIndeterminate = true
            visibility = View.VISIBLE
        }
        resultContainer.addView(progressIndicator)

        resultText = TextView(this).apply {
            textSize = 16f
            setTextColor(attrColor(android.R.attr.textColorPrimary))
            setPadding(0, dp(8), 0, dp(16))
            text = string(R.string.result_preparing)
        }
        resultContainer.addView(resultText)

        val copyBtn = MaterialButton(this).apply {
            text = string(R.string.action_copy_to_clipboard)
            setOnClickListener {
                val clip = ClipData.newPlainText("transcription", resultText.text)
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                Toast.makeText(this@TranscribeActivity, string(R.string.toast_copied), Toast.LENGTH_SHORT).show()
            }
        }
        resultContainer.addView(copyBtn)
        root.addView(resultContainer)

        // Close Button
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

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true) {
            (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uri ->
                startTranscription(uri)
            }
        } else if (intent?.action == Intent.ACTION_SEND_MULTIPLE && intent.type?.startsWith("audio/") == true) {
            // Take the first one for now
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.firstOrNull()?.let { uri ->
                startTranscription(uri)
            }
        } else {
            statusLabel.text = string(R.string.error_no_audio_received)
            progressIndicator.visibility = View.GONE
            resultText.text = string(R.string.error_share_audio)
        }
    }

    private fun startTranscription(uri: Uri) {
        statusLabel.text = string(R.string.status_processing_audio)
        resultText.text = string(R.string.result_decoding_audio)
        progressIndicator.visibility = View.VISIBLE

        thread {
            val samples = AudioDecoder.decodeToPcm(this, uri)
            if (samples == null) {
                runOnUiThread {
                    statusLabel.text = string(R.string.status_decoding_failed)
                    resultText.text = string(R.string.error_decoding_audio)
                    progressIndicator.visibility = View.GONE
                }
                return@thread
            }

            runOnUiThread {
                statusLabel.text = string(R.string.status_transcribing)
                resultText.text = string(R.string.result_in_progress)
            }

            val transcriber = TranscriberManager.getOrCreateTranscriber(this)

            if (transcriber != null) {
                val text = transcriber.transcribe(samples)
                handleTranscriptionResult(text)
            } else {
                runOnUiThread {
                    statusLabel.text = string(R.string.status_local_model_error)
                    resultText.text = string(R.string.error_local_model_not_ready)
                    progressIndicator.visibility = View.GONE
                }
            }
        }
    }

    private fun handleTranscriptionResult(text: String) {
        runOnUiThread {
            statusLabel.text = string(R.string.status_finished)
            resultText.text = text
            progressIndicator.visibility = View.GONE
        }
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