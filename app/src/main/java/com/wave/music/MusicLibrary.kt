package com.wave.music

import android.content.Context
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject

/** Reads every song the phone has indexed (the same list other music apps use). */
object MusicLibrary {
    fun listJson(ctx: Context): String {
        val out = JSONArray()
        val m = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        try {
            ctx.contentResolver.query(
                m, projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val iName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                while (c.moveToNext()) {
                    var title = clean(c.getString(iTitle))
                    if (title.isBlank()) title = clean(c.getString(iName)).substringBeforeLast('.')
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

    private fun clean(s: String?): String = if (s == null || s == "<unknown>") "" else s
}
