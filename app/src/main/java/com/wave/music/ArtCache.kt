package com.wave.music

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import java.io.ByteArrayOutputStream
import java.io.File

/** Finds album artwork for a song and keeps a small JPEG copy in the app cache. */
object ArtCache {
    private const val SIZE = 480

    fun get(ctx: Context, id: Long): ByteArray? {
        val dir = File(ctx.cacheDir, "art2")
        dir.mkdirs()
        val hit = File(dir, "$id.jpg")
        val miss = File(dir, "$id.none")
        if (hit.exists()) return hit.readBytes()
        if (miss.exists()) return null

        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        var bmp: Bitmap? = null
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                bmp = ctx.contentResolver.loadThumbnail(uri, Size(SIZE, SIZE), null)
            } catch (e: Exception) { /* no thumbnail */ }
        }
        if (bmp == null) bmp = fromTags(ctx, uri)
        if (bmp == null) {
            try { miss.createNewFile() } catch (e: Exception) { }
            return null
        }
        val out = ByteArrayOutputStream()
        square(bmp).compress(Bitmap.CompressFormat.JPEG, 88, out)
        val bytes = out.toByteArray()
        try { hit.writeBytes(bytes) } catch (e: Exception) { }
        return bytes
    }

    /** Centre-crops to a square and resizes, so every cover has exactly the same size. */
    private fun square(src: Bitmap): Bitmap {
        val side = minOf(src.width, src.height)
        if (side <= 0) return src
        val cropped = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        return if (side == SIZE) cropped else Bitmap.createScaledBitmap(cropped, SIZE, SIZE, true)
    }

    private fun fromTags(ctx: Context, uri: Uri): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, uri)
            val pic = r.embeddedPicture ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(pic, 0, pic.size, bounds)
            var sample = 1
            while (bounds.outWidth / sample > 720 || bounds.outHeight / sample > 720) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(pic, 0, pic.size, opts)
        } catch (e: Exception) {
            null
        } finally {
            try { r.release() } catch (e: Exception) { }
        }
    }
}
