package com.kafkasl.phonewhisper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        layout.addView(TextView(this).apply { text = "🎤 Phone Whisper"; textSize = 24f })

        status = TextView(this).apply { textSize = 14f }
        layout.addView(status)

        val apiInput = EditText(this).apply {
            hint = "OpenAI API Key"
            isSingleLine = true
            setText(prefs().getString("api_key", ""))
        }
        layout.addView(apiInput)

        layout.addView(Button(this).apply {
            text = "Save API Key"
            setOnClickListener {
                prefs().edit().putString("api_key", apiInput.text.toString().trim()).apply()
                toast("Saved"); updateStatus()
            }
        })

        layout.addView(Button(this).apply {
            text = "Open Accessibility Settings"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        setContentView(layout)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onResume() { super.onResume(); updateStatus() }

    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        updateStatus()
    }

    private fun updateStatus() {
        val audio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        val acc = WhisperAccessibilityService.instance != null
        val key = (prefs().getString("api_key", "") ?: "").isNotBlank()

        status.text = buildString {
            appendLine()
            appendLine("Audio permission: ${if (audio) "✅" else "❌"}")
            appendLine("Accessibility service: ${if (acc) "✅" else "❌"}")
            appendLine("API key: ${if (key) "✅" else "❌"}")
            appendLine()
            if (audio && acc && key) appendLine("Ready! Tap the green dot to dictate.")
            else appendLine("Complete the setup above.")
        }
    }

    private fun prefs() = getSharedPreferences("phonewhisper", MODE_PRIVATE)
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
