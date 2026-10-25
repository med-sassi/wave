package com.wave.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Shows the music controls (previous / play-pause / next) in the notification
 * shade and on the lock screen, handles headset buttons, and keeps playback
 * alive when the screen is off or you switch apps.
 */
class PlaybackService : Service() {

    companion object {
        const val ACTION_PREV = "com.wave.music.PREV"
        const val ACTION_TOGGLE = "com.wave.music.TOGGLE"
        const val ACTION_NEXT = "com.wave.music.NEXT"
        private const val CHANNEL = "wave_playback_controls"
        private const val NOTIF_ID = 1

        private val handler = Handler(Looper.getMainLooper())
        @Volatile private var instance: PlaybackService? = null
        private var title = ""
        private var artist = ""
        private var playing = false
        private var posMs = 0L
        private var durMs = 0L
        private var art: Bitmap? = null
        private var artKey = ""
        private var stopTask: Runnable? = null

        private fun downloadBitmap(url: String): Bitmap? {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            return try { conn.inputStream.use { BitmapFactory.decodeStream(it) } } finally { conn.disconnect() }
        }

        fun update(ctx: Context, isPlaying: Boolean, newTitle: String, newArtist: String, id: Long, pos: Long, dur: Long, coverUrl: String) {
            val app = ctx.applicationContext
            var bmp: Bitmap? = null
            val key = if (id > 0) "m$id" else coverUrl
            val reload = key != artKey
            if (reload) {
                bmp = try {
                    if (id > 0) ArtCache.get(app, id)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    else if (coverUrl.startsWith("https://")) downloadBitmap(coverUrl)
                    else null
                } catch (e: Exception) { null }
            }
            handler.post {
                title = newTitle; artist = newArtist; playing = isPlaying; posMs = pos; durMs = dur
                if (reload) { art = bmp; artKey = key }
                stopTask?.let { handler.removeCallbacks(it) }
                stopTask = null
                val svc = instance
                if (isPlaying) {
                    if (svc == null) {
                        try { ContextCompat.startForegroundService(app, Intent(app, PlaybackService::class.java)) } catch (e: Exception) { }
                    } else svc.refresh()
                } else {
                    svc?.refresh()
                    val task = Runnable { app.stopService(Intent(app, PlaybackService::class.java)) }
                    stopTask = task
                    handler.postDelayed(task, 180_000)
                }
            }
        }
    }

    private lateinit var session: MediaSession
    private var focusRequest: AudioFocusRequest? = null
    private var resumeOnGain = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> { resumeOnGain = false; MainActivity.control("pause") }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> { if (playing) { resumeOnGain = true; MainActivity.control("pause") } }
            AudioManager.AUDIOFOCUS_GAIN -> { if (resumeOnGain) { resumeOnGain = false; MainActivity.control("play") } }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Music controls", NotificationManager.IMPORTANCE_LOW))
        session = MediaSession(this, "Wave")
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { MainActivity.control("play") }
            override fun onPause() { MainActivity.control("pause") }
            override fun onSkipToNext() { MainActivity.control("next") }
            override fun onSkipToPrevious() { MainActivity.control("prev") }
            override fun onSeekTo(pos: Long) { MainActivity.seek(pos / 1000.0) }
        })
        session.isActive = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREV -> MainActivity.control("prev")
            ACTION_TOGGLE -> MainActivity.control("toggle")
            ACTION_NEXT -> MainActivity.control("next")
        }
        if (playing) requestFocus()
        updateSession()
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

    fun refresh() {
        if (playing) requestFocus()
        updateSession()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ID, buildNotification())
    }

    private fun requestFocus() {
        if (focusRequest != null) return
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusListener, handler)
            .build()
        focusRequest = req
        am.requestAudioFocus(req)
    }

    private fun updateSession() {
        val md = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, durMs)
        art?.let { md.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it) }
        session.setMetadata(md.build())
        val actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, posMs, if (playing) 1f else 0f)
                .build()
        )
    }

    private fun actionIntent(action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            this, code, Intent(this, PlaybackService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val b = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (title.isBlank()) "Wave" else title)
            .setContentText(artist)
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        art?.let { b.setLargeIcon(it) }
        b.addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_prev), "Previous", actionIntent(ACTION_PREV, 1)).build())
        b.addAction(
            Notification.Action.Builder(
                Icon.createWithResource(this, if (playing) R.drawable.ic_pause else R.drawable.ic_play),
                if (playing) "Pause" else "Play", actionIntent(ACTION_TOGGLE, 2)
            ).build()
        )
        b.addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_next), "Next", actionIntent(ACTION_NEXT, 3)).build())
        return b.build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }

    override fun onDestroy() {
        instance = null
        try {
            focusRequest?.let { (getSystemService(AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it) }
            focusRequest = null
            session.isActive = false
            session.release()
        } catch (e: Exception) { }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
