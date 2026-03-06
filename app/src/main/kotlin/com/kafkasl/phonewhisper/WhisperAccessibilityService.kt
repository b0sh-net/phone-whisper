package com.kafkasl.phonewhisper

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

class WhisperAccessibilityService : AccessibilityService() {

    companion object {
        var instance: WhisperAccessibilityService? = null
        private const val SAMPLE_RATE = 16000
    }

    private enum class State { IDLE, RECORDING, TRANSCRIBING }

    private var state = State.IDLE
    private var overlayView: View? = null
    private var audioRecord: AudioRecord? = null
    private var pcmStream: ByteArrayOutputStream? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        instance = this
        showOverlay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        removeOverlay()
        super.onDestroy()
    }

    // --- Overlay (TYPE_ACCESSIBILITY_OVERLAY — no extra permission needed) ---

    private fun showOverlay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val dp = resources.displayMetrics.density
        val size = (56 * dp).toInt()

        val button = View(this).apply {
            background = circle(Color.GREEN)
            alpha = 0.85f
            setOnClickListener { onTap() }
        }

        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = (16 * dp).toInt()
        }

        wm.addView(button, params)
        overlayView = button
    }

    private fun removeOverlay() {
        overlayView?.let {
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
            overlayView = null
        }
    }

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL; setColor(color)
    }

    private fun setColor(color: Int) {
        handler.post { overlayView?.background = circle(color) }
    }

    // --- State machine ---

    private fun onTap() {
        when (state) {
            State.IDLE -> startRecording()
            State.RECORDING -> stopAndTranscribe()
            State.TRANSCRIBING -> {} // ignore
        }
    }

    private fun startRecording() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            toast("Grant audio permission in Phone Whisper app"); return
        }

        val bufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize
            )
        } catch (e: SecurityException) { toast("Audio permission denied"); return }

        pcmStream = ByteArrayOutputStream()
        audioRecord!!.startRecording()
        state = State.RECORDING
        setColor(Color.RED)

        thread {
            val buf = ByteArray(bufSize)
            while (state == State.RECORDING) {
                val n = audioRecord?.read(buf, 0, buf.size) ?: break
                if (n > 0) pcmStream?.write(buf, 0, n)
            }
        }
    }

    private fun stopAndTranscribe() {
        state = State.TRANSCRIBING
        setColor(Color.parseColor("#2196F3")) // blue

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val pcm = pcmStream?.toByteArray() ?: ByteArray(0)
        pcmStream = null

        if (pcm.isEmpty()) { reset("No audio captured"); return }

        val wav = WavWriter.encode(pcm)
        val apiKey = prefs().getString("api_key", "") ?: ""
        if (apiKey.isBlank()) { reset("Set API key in Phone Whisper app"); return }

        TranscriberClient.transcribe(wav, apiKey) { result ->
            handler.post {
                if (result.text != null && result.text.isNotBlank()) {
                    injectText(result.text)
                } else {
                    toast("Error: ${result.error ?: "empty transcript"}")
                }
                state = State.IDLE
                setColor(Color.GREEN)
            }
        }
    }

    private fun reset(msg: String) {
        toast(msg); state = State.IDLE; setColor(Color.GREEN)
    }

    // --- Text injection via clipboard + paste ---

    private fun injectText(text: String) {
        val clip = ClipData.newPlainText("phonewhisper", text)
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)

        val root = rootInActiveWindow
        val focused = root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (focused != null) {
            val ok = focused.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            focused.recycle()
            if (!ok) toast("Copied to clipboard (paste failed)")
        } else {
            toast("Copied to clipboard")
        }
        root?.recycle()
    }

    private fun prefs() = getSharedPreferences("phonewhisper", MODE_PRIVATE)
    private fun toast(msg: String) { handler.post { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() } }
}
