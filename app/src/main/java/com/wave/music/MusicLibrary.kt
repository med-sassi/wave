package com.wave.music

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads the songs the phone has indexed, keeping real music only:
 * voice messages, call/voice recordings, ringtones, alarms, notification
 * sounds, podcasts and audiobooks are left out.
 */
object MusicLibrary {

    private const val MIN_SONG_MS = 40_000L

    private val BLOCKED_FOLDER_WORDS = listOf(
        "whatsapp", "recording", "recorder", "voice note", "voice message", "voice memo",
        "voicemail", "voice recorder", "ringtone", "notification", "alarm", "audiobook",
        "podcast", "viber", "messenger", "snapchat", "call record", "callrecord"
    )
    private val BLOCKED_FOLDER_EXACT = setOf("voice", "voices", "call", "calls", "recordings")
    private val BLOCKED_NAME = Regex(
        "^(ptt|aud|vn|rec|record|recording|new recording|voice|voice memo|voice note|call|audio message)[-_ ]?\\(?\\d.*|.*-wa\\d+.*",
        RegexOption.IGNORE_CASE
    )
    private val BLOCKED_MIME = setOf("audio/amr", "audio/amr-wb", "audio/3gpp", "audio/3gpp2", "audio/x-amr")

    private fun flagOff(col: String) = "($col IS NULL OR $col = 0)"

    @Suppress("DEPRECATION")
    fun listJson(ctx: Context): String {
        val out = JSONArray()
        val m = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val where = StringBuilder()
        where.append("${MediaStore.Audio.Media.IS_MUSIC} != 0")
        where.append(" AND ${flagOff(MediaStore.Audio.Media.IS_RINGTONE)}")
        where.append(" AND ${flagOff(MediaStore.Audio.Media.IS_NOTIFICATION)}")
        where.append(" AND ${flagOff(MediaStore.Audio.Media.IS_ALARM)}")
        where.append(" AND ${flagOff(MediaStore.Audio.Media.IS_PODCAST)}")
        where.append(" AND ${MediaStore.Audio.Media.DURATION} >= $MIN_SONG_MS")
        if (Build.VERSION.SDK_INT >= 29) where.append(" AND ${flagOff(MediaStore.Audio.Media.IS_AUDIOBOOK)}")
        if (Build.VERSION.SDK_INT >= 31) where.append(" AND ${flagOff(MediaStore.Audio.Media.IS_RECORDING)}")

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE
        )
        try {
            ctx.contentResolver.query(
                m, projection, where.toString(), null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val iName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val iPath = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val iMime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                while (c.moveToNext()) {
                    val fileName = clean(c.getString(iName))
                    if (isNotMusic(c.getString(iPath), fileName, c.getString(iMime))) continue
                    var title = clean(c.getString(iTitle))
                    if (title.isBlank()) title = fileName.substringBeforeLast('.')
                    val o = JSONObject()
                    o.put("id", c.getLong(iId))
                    o.put("title", title)
                    o.put("artist", clean(c.getString(iArtist)))
                    o.put("album", clean(c.getString(iAlbum)))
                    o.put("duration", c.getLong(iDur))
                    o.put("added", c.getLong(iAdded))
                    out.put(o)
                }
            }
        } catch (e: Exception) {
            // permission missing or storage unavailable: return what we have
        }
        return out.toString()
    }

    /** Extra safety net for voice notes and recordings that phones flag as "music". */
    private fun isNotMusic(path: String?, fileName: String, mime: String?): Boolean {
        if (mime != null && mime.lowercase() in BLOCKED_MIME) return true
        if (BLOCKED_NAME.matches(fileName.substringBeforeLast('.').trim())) return true
        if (path != null) {
            val folders = path.lowercase().split('/').dropLast(1)
            for (f in folders) {
                if (f in BLOCKED_FOLDER_EXACT) return true
                if (BLOCKED_FOLDER_WORDS.any { f.contains(it) }) return true
            }
        }
        return false
    }

    private fun clean(s: String?): String = if (s == null || s == "<unknown>") "" else s
}
