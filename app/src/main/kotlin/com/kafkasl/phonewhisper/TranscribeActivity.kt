package com.kafkasl.phonewhisper

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
            text = "Transcribing Audio"
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(16))
        }
        root.addView(header)

        // Status Label
        statusLabel = TextView(this).apply {
            text = "Initializing..."
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
            text = "Preparing..."
        }
        resultContainer.addView(resultText)

        val copyBtn = MaterialButton(this).apply {
            text = "Copy to Clipboard"
            setOnClickListener {
                val clip = ClipData.newPlainText("transcription", resultText.text)
                (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                Toast.makeText(this@TranscribeActivity, "Copied!", Toast.LENGTH_SHORT).show()
            }
        }
        resultContainer.addView(copyBtn)
        root.addView(resultContainer)

        // Close Button
        val closeBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Close"
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
            statusLabel.text = "No audio file received"
            progressIndicator.visibility = View.GONE
            resultText.text = "Please share an audio file from another app."
        }
    }

    private fun startTranscription(uri: Uri) {
        statusLabel.text = "Processing audio..."
        resultText.text = "Decoding audio..."
        progressIndicator.visibility = View.VISIBLE

        thread {
            val samples = AudioDecoder.decodeToPcm(this, uri)
            if (samples == null) {
                runOnUiThread {
                    statusLabel.text = "Decoding failed"
                    resultText.text = "Error decoding audio file. Make sure it's a valid audio format."
                    progressIndicator.visibility = View.GONE
                }
                return@thread
            }

            runOnUiThread { 
                statusLabel.text = "Transcribing..."
                resultText.text = "In progress..." 
            }

            val useLocal = prefs().getBoolean("use_local", true)
            if (useLocal) {
                val transcriber = TranscriberManager.getOrCreateTranscriber(this)

                if (transcriber != null) {
                    val text = transcriber.transcribe(samples)
                    handleTranscriptionResult(text)
                } else {
                    runOnUiThread {
                        statusLabel.text = "Local model error"
                        resultText.text = "Local model not ready. Check that a model is downloaded in the app settings."
                        progressIndicator.visibility = View.GONE
                    }
                }
            } else {
                // Cloud transcription
                val pcm = ByteArray(samples.size * 2)
                for (i in samples.indices) {
                    val s = (samples[i] * 32767).toInt().coerceIn(-32768, 32767).toShort()
                    pcm[i * 2] = (s.toInt() and 0xFF).toByte()
                    pcm[i * 2 + 1] = (s.toInt() shr 8 and 0xFF).toByte()
                }
                val wav = WavWriter.encode(pcm)
                val apiKey = prefs().getString("api_key", "") ?: ""

                if (apiKey.isBlank()) {
                    runOnUiThread {
                        statusLabel.text = "Configuration error"
                        resultText.text = "OpenAI API Key missing. Please set it in the app settings."
                        progressIndicator.visibility = View.GONE
                    }
                    return@thread
                }

                TranscriberClient.transcribe(wav, apiKey) { result ->
                    runOnUiThread {
                        if (result.text != null) handleTranscriptionResult(result.text)
                        else {
                            statusLabel.text = "API Error"
                            resultText.text = result.error ?: "Unknown cloud error"
                            progressIndicator.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun handleTranscriptionResult(text: String) {
        val usePostProcessing = prefs().getBoolean("use_post_processing", false)
        val apiKey = prefs().getString("api_key", "") ?: ""

        if (usePostProcessing && apiKey.isNotBlank()) {
            runOnUiThread { 
                statusLabel.text = "Post-processing..."
                resultText.text = "Cleaning up transcript..." 
            }
            val prompt = prefs().getString("post_processing_prompt", PostProcessor.DEFAULT_PROMPT) ?: PostProcessor.DEFAULT_PROMPT
            PostProcessor.process(text, prompt, apiKey) { result ->
                runOnUiThread {
                    statusLabel.text = "Finished"
                    resultText.text = result.text ?: text
                    progressIndicator.visibility = View.GONE
                }
            }
        } else {
            runOnUiThread {
                statusLabel.text = "Finished"
                resultText.text = text
                progressIndicator.visibility = View.GONE
            }
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
    
    private fun prefs() = getSharedPreferences("phonewhisper", MODE_PRIVATE)
}
