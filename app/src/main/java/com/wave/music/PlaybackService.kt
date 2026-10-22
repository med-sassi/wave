package com.wave.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Keeps the app alive (with a small notification) while music plays,
 * so playback continues when the screen is off or you switch apps.
 */
class PlaybackService : Service() {

    companion object {
        private const val CHANNEL = "wave_playback"
        private const val NOTIF_ID = 1
        private val handler = Handler(Looper.getMainLooper())
        private var title = ""
        private var artist = ""
        private var stopTask: Runnable? = null

        fun update(ctx: Context, playing: Boolean, newTitle: String, newArtist: String) {
            val app = ctx.applicationContext
            handler.post {
                title = newTitle
                artist = newArtist
                stopTask?.let { handler.removeCallbacks(it) }
                stopTask = null
                if (playing) {
                    try {
                        ContextCompat.startForegroundService(app, Intent(app, PlaybackService::class.java))
                    } catch (e: Exception) { /* not allowed right now */ }
                } else {
                    // stay alive briefly so the next song can start in the background
                    val task = Runnable { app.stopService(Intent(app, PlaybackService::class.java)) }
                    stopTask = task
                    handler.postDelayed(task, 120_000)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Playback", NotificationManager.IMPORTANCE_LOW))
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, buildNotification(),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
            )
        } catch (e: Exception) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (title.isBlank()) "Wave" else title)
            .setContentText(artist)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
