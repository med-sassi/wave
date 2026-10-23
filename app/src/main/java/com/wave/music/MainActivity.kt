package com.wave.music

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.lang.ref.WeakReference

/**
 * Hosts the Wave interface (assets/index.html) in a WebView and gives it:
 *  - the list of songs on the phone  (WaveNative.getAudioList)
 *  - the songs themselves            (https://appassets.androidplatform.net/media/<id>)
 *  - album artwork                   (https://appassets.androidplatform.net/art/<id>)
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val HOST = "appassets.androidplatform.net"
        @Volatile private var webRef: WeakReference<WebView>? = null

        /** Sends play / pause / toggle / next / prev from the notification or lock screen to the player. */
        fun control(cmd: String) {
            val w = webRef?.get() ?: return
            w.post { w.evaluateJavascript("window.__waveCtl && window.__waveCtl('$cmd')", null) }
        }

        fun seek(seconds: Double) {
            val w = webRef?.get() ?: return
            w.post { w.evaluateJavascript("window.__waveSeek && window.__waveSeek($seconds)", null) }
        }
    }

    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        fileCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
        fileCallback = null
    }

    private lateinit var web: WebView
    private lateinit var loader: WebViewAssetLoader
    private val prefs by lazy { getSharedPreferences("wave", MODE_PRIVATE) }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { syncLibrary() }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loader = WebViewAssetLoader.Builder()
            .setDomain(HOST)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        web = WebView(this)
        webRef = WeakReference(web)
        setContentView(web)
        web.setBackgroundColor(0xFF0B121A.toInt())
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
        }
        web.addJavascriptInterface(Bridge(), "WaveNative")
        // lets the page open the picture chooser (playlist pictures)
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = filePathCallback
                imagePicker.launch("image/*")
                return true
            }
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val uri = request.url
                if (uri.host != HOST) {
                    return if (uri.scheme == "data" || uri.scheme == "blob") null else status(403, "Forbidden")
                }
                val path = uri.path ?: ""
                return when {
                    path.startsWith("/media/") -> serveMedia(path.removePrefix("/media/").toLongOrNull(), request.requestHeaders)
                    path.startsWith("/art/") -> serveArt(path.removePrefix("/art/").toLongOrNull())
                    else -> loader.shouldInterceptRequest(uri)
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
        }
        web.loadUrl("https://$HOST/assets/index.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("window.__waveBack ? window.__waveBack() : false") { result ->
                    if (result != "true") moveTaskToBack(true)
                }
            }
        })

        if (!hasAudioPermission()) askPermission()
    }

    override fun onResume() {
        super.onResume()
        syncLibrary() // pick up songs added while the app was in the background
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    // ---------- permissions ----------

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED

    private fun askPermission() {
        if (hasAudioPermission()) { syncLibrary(); return }
        val askedBefore = prefs.getBoolean("asked", false)
        if (askedBefore && !ActivityCompat.shouldShowRequestPermissionRationale(this, audioPermission())) {
            // the system won't show the dialog again: send the user to the app settings
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            return
        }
        prefs.edit().putBoolean("asked", true).apply()
        val wanted = mutableListOf(audioPermission())
        if (Build.VERSION.SDK_INT >= 33) wanted.add(Manifest.permission.POST_NOTIFICATIONS)
        permissionLauncher.launch(wanted.toTypedArray())
    }

    private fun syncLibrary() {
        if (::web.isInitialized) web.evaluateJavascript("window.__waveSync && window.__waveSync()", null)
    }

    // ---------- bridge used by the interface ----------

    inner class Bridge {
        @JavascriptInterface
        fun hasPermission(): Boolean = hasAudioPermission()

        @JavascriptInterface
        fun requestPermission() { runOnUiThread { askPermission() } }

        @JavascriptInterface
        fun getAudioList(): String = MusicLibrary.listJson(this@MainActivity)

        @JavascriptInterface
        fun setNowPlaying(playing: Boolean, title: String, artist: String, mediaId: Double, positionMs: Double, durationMs: Double) {
            PlaybackService.update(applicationContext, playing, title, artist, mediaId.toLong(), positionMs.toLong(), durationMs.toLong())
        }
    }

    // ---------- serving songs and artwork to the WebView ----------

    private fun status(code: Int, text: String) =
        WebResourceResponse("text/plain", "utf-8", code, text, emptyMap(), ByteArrayInputStream(ByteArray(0)))

    private fun serveArt(id: Long?): WebResourceResponse {
        if (id == null || !hasAudioPermission()) return status(404, "Not Found")
        val bytes = ArtCache.get(this, id) ?: return status(404, "Not Found")
        return WebResourceResponse(
            "image/jpeg", null, 200, "OK",
            mapOf("Cache-Control" to "max-age=86400"),
            ByteArrayInputStream(bytes)
        )
    }

    /** Streams a song, with HTTP Range support so seeking works. */
    private fun serveMedia(id: Long?, headers: Map<String, String>): WebResourceResponse {
        if (id == null || !hasAudioPermission()) return status(404, "Not Found")
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        try {
            val afd = contentResolver.openAssetFileDescriptor(uri, "r") ?: return status(404, "Not Found")
            val total = afd.length
            val mime = contentResolver.getType(uri) ?: "audio/mpeg"
            var start = 0L
            var end = total - 1
            var partial = false

            val range = headers["Range"] ?: headers["range"]
            if (total > 0 && range != null && range.startsWith("bytes=")) {
                val parts = range.removePrefix("bytes=").split("-")
                val s = parts.getOrNull(0)?.trim()?.toLongOrNull()
                val e = parts.getOrNull(1)?.trim()?.toLongOrNull()
                if (s == null && e != null) {
                    start = maxOf(0L, total - e)
                } else if (s != null) {
                    start = s
                    if (e != null) end = minOf(e, total - 1)
                }
                if (start >= total) {
                    afd.close()
                    return WebResourceResponse(
                        mime, null, 416, "Range Not Satisfiable",
                        mapOf("Content-Range" to "bytes */$total"), ByteArrayInputStream(ByteArray(0))
                    )
                }
                partial = true
            }

            val input = afd.createInputStream()
            skipFully(input, start)
            val h = mutableMapOf("Accept-Ranges" to "bytes", "Cache-Control" to "no-store")
            val stream: InputStream
            if (total > 0) {
                val length = end - start + 1
                h["Content-Length"] = length.toString()
                if (partial) h["Content-Range"] = "bytes $start-$end/$total"
                stream = LimitedStream(input, length)
            } else {
                stream = input
            }
            return WebResourceResponse(
                mime, null,
                if (partial) 206 else 200,
                if (partial) "Partial Content" else "OK",
                h, stream
            )
        } catch (e: Exception) {
            return status(404, "Not Found")
        }
    }

    private fun skipFully(input: InputStream, count: Long) {
        var left = count
        while (left > 0) {
            val skipped = input.skip(left)
            if (skipped <= 0) break
            left -= skipped
        }
    }

    private class LimitedStream(input: InputStream, private var left: Long) : FilterInputStream(input) {
        override fun read(): Int {
            if (left <= 0) return -1
            val r = super.read()
            if (r >= 0) left--
            return r
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (left <= 0) return -1
            val r = super.read(b, off, minOf(len.toLong(), left).toInt())
            if (r > 0) left -= r
            return r
        }
    }
}
