package net.b0sh.audiotext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.b0sh.audiotext.ui.TranscribeScreen
import net.b0sh.audiotext.ui.TranscriptUiState
import net.b0sh.audiotext.ui.theme.AudioToTextTheme
import kotlin.concurrent.thread

class TranscribeActivity : AppCompatActivity() {

    private var uiState by mutableStateOf(TranscriptUiState("", "", true))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Il Context è agganciato: solo ora si possono risolvere le risorse.
        uiState = initialState()

        setContent {
            AudioToTextTheme {
                TranscribeScreen(
                    state = uiState,
                    onCopy = ::copyToClipboard,
                    onClose = { finish() },
                )
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun initialState() = TranscriptUiState(
        statusText = string(R.string.status_initializing),
        resultText = string(R.string.result_preparing),
        showProgress = true,
    )

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                startTranscription(uri)
            }
        } else if (intent?.action == Intent.ACTION_SEND_MULTIPLE && intent.type?.startsWith("audio/") == true) {
            // Take the first one for now
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.firstOrNull()?.let { uri ->
                startTranscription(uri)
            }
        } else {
            uiState = TranscriptUiState(
                statusText = string(R.string.error_no_audio_received),
                resultText = string(R.string.error_share_audio),
                showProgress = false,
            )
        }
    }

    private fun startTranscription(uri: Uri) {
        uiState = TranscriptUiState(
            statusText = string(R.string.status_processing_audio),
            resultText = string(R.string.result_decoding_audio),
            showProgress = true,
        )

        thread {
            val samples = AudioDecoder.decodeToPcm(this, uri)
            if (samples == null) {
                uiState = TranscriptUiState(
                    statusText = string(R.string.status_decoding_failed),
                    resultText = string(R.string.error_decoding_audio),
                    showProgress = false,
                )
                return@thread
            }

            uiState = TranscriptUiState(
                statusText = string(R.string.status_transcribing),
                resultText = string(R.string.result_in_progress),
                showProgress = true,
            )

            val transcriber = TranscriberManager.getOrCreateTranscriber(this)

            if (transcriber != null) {
                val text = transcriber.transcribe(samples)
                uiState = TranscriptUiState(
                    statusText = string(R.string.status_finished),
                    resultText = text,
                    showProgress = false,
                )
            } else {
                uiState = TranscriptUiState(
                    statusText = string(R.string.status_local_model_error),
                    resultText = string(R.string.error_local_model_not_ready),
                    showProgress = false,
                )
            }
        }
    }

    private fun copyToClipboard() {
        val clip = ClipData.newPlainText("transcription", uiState.resultText)
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
        Toast.makeText(this, string(R.string.toast_copied), Toast.LENGTH_SHORT).show()
    }

    private fun string(@StringRes resId: Int, vararg args: Any): String =
        getString(resId, *args)
}