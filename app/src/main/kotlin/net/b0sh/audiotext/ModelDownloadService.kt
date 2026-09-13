package net.b0sh.audiotext

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import net.b0sh.audiotext.ui.DownloadPhase
import kotlin.concurrent.thread

/** Stato di avanzamento dei download condiviso tra il service e le Activity.
 *  È uno stato Compose osservabile: la catalog activity lo legge per mostrare
 *  la barra di avanzamento, anche dopo essere stata riaperta. */
object ModelDownloadRepository {
    val downloads = mutableStateMapOf<String, DownloadPhase>()
    fun mark(id: String, phase: DownloadPhase) { downloads[id] = phase }
}

/** Scarica i modelli come foreground service (tipo dataSync): il download
 *  continua in background anche chiudendo il catalogo, senza essere terminato
 *  dal sistema (MIUI/Doze). La notifica mostra l'avanzamento. A download
 *  concluso seleziona il modello e pre-carica il riconoscitore. */
class ModelDownloadService : Service() {

    companion object {
        private const val EXTRA_MODEL_ID = "model_id"
        private const val CHANNEL_ID = "model_downloads"

        fun start(context: Context, model: Model) {
            val intent = Intent(context, ModelDownloadService::class.java)
                .putExtra(EXTRA_MODEL_ID, model.id)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.section_downloadable_models),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modelId = intent?.getStringExtra(EXTRA_MODEL_ID) ?: return START_NOT_STICKY
        val model = MODEL_CATALOG.find { it.id == modelId } ?: return START_NOT_STICKY
        val notifId = modelId.hashCode()

        ModelDownloadRepository.mark(modelId, DownloadPhase.Downloading(0f, null))
        startForeground(notifId, notification(model.name, null))

        // ModelDownloader gestisce il proprio thread: qui lo avviamo e
        // aggiorniamo notifica/stato dai callback.
        thread {
            ModelDownloader.download(this, model) { state ->
                handler.post { onDownloadState(model, notifId, state) }
            }
        }
        return START_NOT_STICKY
    }

    private fun onDownloadState(model: Model, notifId: Int, state: DownloadState) {
        when (state) {
            is DownloadState.Downloading -> {
                val phase = DownloadPhase.Downloading(state.progress, state.currentFile)
                ModelDownloadRepository.mark(model.id, phase)
                updateNotification(model, phase)
            }
            is DownloadState.Extracting -> {
                val phase = DownloadPhase.Extracting(state.currentFile)
                ModelDownloadRepository.mark(model.id, phase)
                updateNotification(model, phase)
            }
            is DownloadState.Done -> {
                ModelDownloadRepository.mark(model.id, DownloadPhase.Idle)
                prefs().edit().putString("model_name", model.id).apply()
                TranscriberManager.reset()
                Toast.makeText(this, string(R.string.toast_model_installed, model.name), Toast.LENGTH_SHORT).show()
                thread { TranscriberManager.getOrCreateTranscriber(applicationContext) }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            is DownloadState.Error -> {
                ModelDownloadRepository.mark(model.id, DownloadPhase.Idle)
                Toast.makeText(this, state.message ?: "Unknown error", Toast.LENGTH_LONG).show()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun updateNotification(model: Model, phase: DownloadPhase) {
        val text = when (phase) {
            is DownloadPhase.Downloading -> string(
                R.string.subtitle_downloading_file,
                (phase.progress * 100).toInt().coerceIn(0, 100),
                phase.currentFile ?: "",
            )
            is DownloadPhase.Extracting -> string(R.string.subtitle_installing_file, phase.currentFile)
            DownloadPhase.Idle -> model.name
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(model.id.hashCode(), notification(model.name, text))
    }

    private fun notification(title: String, text: String?): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text ?: string(R.string.status_initializing))
            .setSmallIcon(R.drawable.ic_stat_download)
            .setOngoing(true)
            .build()

    private fun string(@StringRes resId: Int, vararg args: Any): String =
        getString(resId, *args)

    private fun prefs() = getSharedPreferences("audiotext", MODE_PRIVATE)
}