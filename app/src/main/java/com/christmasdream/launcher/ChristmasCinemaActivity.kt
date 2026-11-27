package com.christmasdream.launcher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Christmas Cinema — festive video player for local files, USB documents and supported streams.
 * Codec support is provided by Android's available decoders; no claim is made that every codec
 * can be decoded on every television.
 */
class ChristmasCinemaActivity : Activity() {
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var titleView: TextView
    private var createdPlayer = false
    private val requestCode = 4120

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(22, 10, 20)
        window.navigationBarColor = Color.rgb(22, 10, 20)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        buildScreen()
        initializePlayer()
        val incoming = collectIncomingUris(intent)
        if (incoming.isNotEmpty()) playUris(incoming) else openPicker()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun buildScreen() {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        playerView = PlayerView(this).apply {
            useController = true
            controllerAutoShow = true
            controllerHideOnTouch = true
            controllerShowTimeoutMs = 4500
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            setKeepContentOnPlayerReset(true)
            contentDescription = "Видеопроигрыватель Christmas Cinema"
        }
        root.addView(playerView, FrameLayout.LayoutParams(-1, -1))

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(10), dp(18), dp(10))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xE8170D18.toInt(), 0x99170D18.toInt(), 0x00170D18)
            )
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(TextView(this).apply {
            text = "✦ CHRISTMAS CINEMA ✦"
            textSize = 17f
            setTextColor(0xFFFFD98D.toInt())
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        })
        titleView = TextView(this).apply {
            text = "РОЖДЕСТВЕНСКИЙ КИНОЗАЛ"
            textSize = 10f
            setTextColor(0xFFFFEBD0.toInt())
            maxLines = 1
        }
        brand.addView(titleView)
        top.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        val open = festiveButton("ОТКРЫТЬ ВИДЕО") { openPicker() }
        top.addView(open, LinearLayout.LayoutParams(-2, dp(44)))
        val close = festiveButton("✕") { finish() }
        top.addView(close, LinearLayout.LayoutParams(dp(48), dp(44)).apply { leftMargin = dp(8) })
        root.addView(top, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))

        val footer = TextView(this).apply {
            text = "❄  USB • MKV • MP4 • AVI • MOV • WEBM  ❄"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFDDA8.toInt())
            setPadding(dp(10), dp(7), dp(10), dp(7))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0x00170D18.toInt(), 0xCC2B1225.toInt(), 0xCC2B1225.toInt(), 0x00170D18.toInt())
            )
            isClickable = false
        }
        root.addView(footer, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(root)
    }

    private fun festiveButton(caption: String, action: () -> Unit): TextView =
        TextView(this).apply {
            text = caption
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFEBD0.toInt())
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xE65C1D35.toInt(), 0xE62B1A2B.toInt())
            ).apply {
                cornerRadius = dp(13).toFloat()
                setStroke(dp(1), 0xFFE7C17D.toInt())
            }
            isFocusable = true
            isClickable = true
            setOnClickListener { action() }
            setOnFocusChangeListener { view, focused ->
                view.scaleX = if (focused) 1.06f else 1f
                view.scaleY = if (focused) 1.06f else 1f
            }
        }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        createdPlayer = true
        playerView.player = player
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val name = mediaItem?.mediaMetadata?.title?.toString()
                titleView.text = name?.takeIf { it.isNotBlank() } ?: "РОЖДЕСТВЕНСКИЙ КИНОЗАЛ"
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    Toast.makeText(this@ChristmasCinemaActivity, "Фильм завершён", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun collectIncomingUris(source: Intent?): List<Uri> {
        if (source == null) return emptyList()
        val result = mutableListOf<Uri>()
        source.data?.let { result.add(it) }
        source.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) clip.getItemAt(i)?.uri?.let { result.add(it) }
        }
        if (result.isEmpty()) {
            @Suppress("DEPRECATION")
            val stream = source.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            stream?.let { result.add(it) }
        }
        return result.distinctBy { it.toString() }
    }

    private fun playUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val items = uris.mapIndexed { index, uri ->
            val title = queryDisplayName(uri) ?: "Видео ${index + 1}"
            MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
                .build()
        }
        player.setMediaItems(items, true)
        player.prepare()
        player.playWhenReady = true
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

    private fun openPicker() {
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            startActivityForResult(picker, requestCode)
        } catch (_: Exception) {
            Toast.makeText(this, "Не удалось открыть выбор видео. Попробуй файловый менеджер телевизора.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Kept for broad Android TV compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != this.requestCode || resultCode != RESULT_OK || data == null) return
        val uris = collectIncomingUris(data)
        if (uris.isEmpty()) {
            Toast.makeText(this, "Видео не выбрано", Toast.LENGTH_SHORT).show()
            return
        }
        uris.forEach { uri ->
            try {
                val granted = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                if (granted != 0) contentResolver.takePersistableUriPermission(uri, granted)
            } catch (_: Exception) {
                // Some USB document providers grant temporary read access only; playback can still work now.
            }
        }
        playUris(uris)
    }

    override fun onPause() {
        if (createdPlayer) player.pause()
        super.onPause()
    }

    override fun onDestroy() {
        if (createdPlayer) {
            playerView.player = null
            player.release()
            createdPlayer = false
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}
