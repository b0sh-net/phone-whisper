package net.b0sh.audiotext

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Foreground service (type `microphone`) that keeps a live transcription session
 * capturing while the app is not visible.
 *
 * Without a foreground service of this type, Android 14+ mutes microphone capture
 * for apps in the background and the system can freeze or kill the process, so the
 * session would die as soon as the screen locks. The service is started only from
 * the visible Live screen (starting it from the background is forbidden by the
 * while-in-use restrictions) and lives exactly as long as the session: it is a
 * thin lifetime shell around [LiveTranscriptionController], which owns the capture
 * thread, the recognizer and the text.
 *
 * The ongoing notification also makes the microphone usage visible and offers an
 * explicit way to stop the session without reopening the app.
 */
class LiveSessionService : Service() {

    companion object {
        private const val TAG = "LiveSession"

        /** Notification channel: silent, no vibration — it stays up for the whole session. */
        private const val CHANNEL_ID = "live_transcription"

        /** Notification id, distinct from the per-model download notifications. */
        private const val NOTIFICATION_ID = 0x1A2B3C

        private const val ACTION_START = "net.b0sh.audiotext.action.LIVE_START"

        /** Action of the notification button: explicit user stop (FR-006). */
        private const val ACTION_STOP = "net.b0sh.audiotext.action.LIVE_STOP"

        /**
         * Start the foreground service. MUST be called while an activity of the app
         * is visible and the RECORD_AUDIO permission is granted (FR-001).
         */
        fun start(context: Context) {
            val intent = Intent(context, LiveSessionService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        /** Stop the foreground service, removing its notification. Idempotent. */
        fun stop(context: Context) {
            context.stopService(Intent(context, LiveSessionService::class.java))
        }
    }

    private var foregroundStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundNow()
            ACTION_STOP -> stopSession()
            else -> {
                // Unknown or missing action: never keep an empty foreground service alive.
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        // Never restart by itself: the session cannot be resumed without the user,
        // and a restarted service would leave an orphan notification (FR-010/FR-011).
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // Safety net: if the system destroys the service while a session is still
        // running, end the session in a controlled way instead of leaving the
        // microphone open and the archive half-written (FR-010).
        if (LiveTranscriptionController.phase == LivePhase.Recording) {
            LiveTranscriptionController.stop(applicationContext)
        }
        super.onDestroy()
    }

    private fun startForegroundNow() {
        if (foregroundStarted) return
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
        )
        foregroundStarted = true
    }

    /**
     * Explicit user stop from the notification: end the session, then drop the
     * foreground service and its notice. Idempotent when no session is running.
     */
    private fun stopSession() {
        LiveTranscriptionController.stop(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.live_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Ongoing, silent notification that the live transcription is running.
     * It never contains the transcribed text or any other user content.
     *
     * Tapping it reopens the app; the "Stop" action ends the session without
     * reopening the app, so the user can always interrupt explicitly (FR-006).
     */
    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.live_notification_title))
            .setContentText(getString(R.string.live_notification_text))
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(openAppIntent())
            .addAction(
                R.drawable.ic_stop_circle,
                getString(R.string.live_notification_stop),
                stopIntent(),
            )
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    /** Reopen the app on tap, without extras and without any deep link. */
    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Deliver ACTION_STOP to this service (Android 12+ requires an immutable PendingIntent). */
    private fun stopIntent(): PendingIntent =
        PendingIntent.getService(
            this,
            1,
            Intent(this, LiveSessionService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
