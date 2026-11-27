package com.christmasdream.launcher

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.animation.ValueAnimator
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class ChristmasMusicActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("christmas_dream_settings", MODE_PRIVATE) }
    private val requestCode = 4110
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var player: ChristmasPlaylistPlayer
    private lateinit var root: LinearLayout
    private var ball: TextView? = null
    private var titleView: TextView? = null
    private var timeView: TextView? = null
    private var seek: SeekBar? = null
    private var volumeSeek: SeekBar? = null
    private var seeking = false
    private var pulse: ValueAnimator? = null
    private var shuffle = false
    private var repeat = true
    private val tick = object : Runnable {
        override fun run() {
            if (!isFinishing) {
                refreshPlayer()
                handler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(66, 12, 31)
        window.navigationBarColor = Color.rgb(66, 12, 31)
        player = ChristmasPlaylistPlayer(this) { _, _, _, _ -> runOnUiThread { refreshPlayer() } }
        player.setVolume(prefs.getFloat("christmas_mp3_volume", 0.8f))
        loadTracks()
        buildScreen()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()
    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
    private fun cardBackground(): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xF04A172A.toInt(), 0xF01B1B2A.toInt(), 0xE62B2635.toInt())).apply {
            cornerRadius = dp(25).toFloat()
            setStroke(dp(1), 0x99E8C47F.toInt())
        }

    private fun buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(16), dp(22), dp(24))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF160F1B.toInt(), 0xFF3A1428.toInt(), 0xFF101A29.toInt()))
        }
        val scroller = ScrollView(this).apply { isFillViewport = true; addView(root) }
        setContentView(scroller)
        root.addView(text("✦  CHRISTMAS DREAM  ✦", 22f, 0xFFFFD98D.toInt(), true).apply { gravity = Gravity.CENTER })
        root.addView(text("РОЖДЕСТВЕНСКИЙ МУЗЫКАЛЬНЫЙ ЗАЛ", 13f, 0xFFFFEBD0.toInt(), true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, dp(15))
        })

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = cardBackground()
            setPadding(dp(18), dp(17), dp(18), dp(16))
            elevation = dp(8).toFloat()
        }
        panel.addView(text("✧    ❄    ✦    ❄    ✧", 20f, 0xFFFFD98D.toInt()).apply { gravity = Gravity.CENTER })
        titleView = text(player.currentTrack?.title ?: "Добавь свои любимые MP3", 17f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            maxLines = 2
            setPadding(dp(8), dp(12), dp(8), dp(4))
        }
        panel.addView(titleView)
        val stage = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, dp(8)) }
        ball = TextView(this).apply {
            text = "▶"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFE7B8.toInt())
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFFFF5265.toInt(), 0xFF9B1738.toInt(), 0xFF4A1028.toInt())).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(3), 0xFFFFD98D.toInt())
            }
            elevation = dp(13).toFloat()
            isFocusable = true
            isClickable = true
            contentDescription = "Воспроизвести или поставить на паузу"
            setOnClickListener { if (player.tracks.isEmpty()) pickTracks() else player.toggle() }
            setOnFocusChangeListener { v, focused ->
                v.animate().scaleX(if (focused) 1.12f else 1f).scaleY(if (focused) 1.12f else 1f).setDuration(150).start()
            }
        }
        stage.addView(ball, LinearLayout.LayoutParams(dp(112), dp(112)))
        panel.addView(stage)

        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        controls.addView(ornament("◀", "Предыдущая") { player.previous() })
        controls.addView(ornament("⤨", "Случайный порядок") {
            shuffle = !shuffle
            player.shuffle = shuffle
            android.widget.Toast.makeText(this@ChristmasMusicActivity,
                if (shuffle) "Случайный порядок включён" else "Обычный порядок", android.widget.Toast.LENGTH_SHORT).show()
        })
        controls.addView(ornament("↻", "Повтор списка") {
            repeat = !repeat
            player.repeatAll = repeat
            player.repeatOne = false
            android.widget.Toast.makeText(this@ChristmasMusicActivity,
                if (repeat) "Повтор списка включён" else "Повтор выключен", android.widget.Toast.LENGTH_SHORT).show()
        })
        controls.addView(ornament("▶|", "Следующая") {
            if (shuffle) player.shuffle = true
            player.next()
        })
        panel.addView(controls)

        seek = SeekBar(this).apply {
            max = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(0xFFE9C47E.toInt())
            thumbTintList = android.content.res.ColorStateList.valueOf(0xFFFFE7B8.toInt())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar: SeekBar?) { seeking = true }
                override fun onStopTrackingTouch(bar: SeekBar?) {
                    seeking = false
                    if (player.duration > 0) player.seekTo((bar?.progress ?: 0) * player.duration / 1000)
                }
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) { }
            })
        }
        panel.addView(seek)
        panel.addView(text("ГРОМКОСТЬ", 10f, 0xFFFFD98D.toInt(), true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        })
        volumeSeek = SeekBar(this).apply {
            max = 100
            progress = (prefs.getFloat("christmas_mp3_volume", 0.8f) * 100).toInt()
            progressTintList = android.content.res.ColorStateList.valueOf(0xFFE9C47E.toInt())
            thumbTintList = android.content.res.ColorStateList.valueOf(0xFFFFE7B8.toInt())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(bar: SeekBar?) { }
                override fun onStopTrackingTouch(bar: SeekBar?) { }
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val volume = value / 100f
                        player.setVolume(volume)
                        prefs.edit().putFloat("christmas_mp3_volume", volume).apply()
                    }
                }
            })
        }
        panel.addView(volumeSeek)
        timeView = text("00:00  /  00:00", 11f, 0xFFFFEBD0.toInt()).apply { gravity = Gravity.CENTER }
        panel.addView(timeView)
        root.addView(panel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(15) })

        root.addView(action("＋   ДОБАВИТЬ MP3-КОМПОЗИЦИИ") { pickTracks() })
        root.addView(action("♫   ИСПОЛЬЗОВАТЬ ТЕКУЩУЮ ПЕСНЮ КАК ФОНОВУЮ") {
            player.currentTrack?.let { track ->
                prefs.edit().putString("custom_music_uri", track.uri).putBoolean("jingle_music_enabled", true).apply()
                android.widget.Toast.makeText(this, "Композиция выбрана для фоновой музыки", android.widget.Toast.LENGTH_SHORT).show()
            } ?: pickTracks()
        })
        root.addView(text("МОЯ РОЖДЕСТВЕНСКАЯ КОЛЛЕКЦИЯ", 16f, 0xFFFFD98D.toInt(), true).apply {
            setPadding(dp(3), dp(18), 0, dp(9))
        })
        refreshList()
        refreshPlayer()
        handler.post(tick)
    }

    private fun ornament(symbol: String, description: String, action: () -> Unit) =
        TextView(this).apply {
            text = symbol
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFE3B0.toInt())
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xCC9C2946.toInt(), 0xCC4A1A30.toInt())).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(1), 0x99E8C47F.toInt())
            }
            contentDescription = description
            isFocusable = true
            isClickable = true
            setOnClickListener { action() }
            setOnFocusChangeListener { v, focused ->
                v.animate().scaleX(if (focused) 1.13f else 1f).scaleY(if (focused) 1.13f else 1f).setDuration(140).start()
            }
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                leftMargin = dp(6); rightMargin = dp(6); topMargin = dp(5); bottomMargin = dp(5)
            }
        }

    private fun action(caption: String, action: () -> Unit) =
        TextView(this).apply {
            text = caption
            textSize = 13f
            setTextColor(0xFFFFEBD0.toInt())
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xE63B2635.toInt(), 0xE625202D.toInt())).apply {
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), 0x77D9B77D)
            }
            isFocusable = true
            isClickable = true
            setOnClickListener { action() }
            setOnFocusChangeListener { v, focused ->
                v.animate().scaleX(if (focused) 1.02f else 1f).scaleY(if (focused) 1.02f else 1f).setDuration(130).start()
            }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
        }

    private fun pickTracks() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try { startActivityForResult(intent, requestCode) }
        catch (_: Exception) {
            android.widget.Toast.makeText(this, "Не удалось открыть выбор музыкальных файлов", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Kept for broad Android TV compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != this.requestCode || resultCode != RESULT_OK || data == null) return
        val uris = mutableListOf<android.net.Uri>()
        data.clipData?.let { clip -> for (i in 0 until clip.itemCount) clip.getItemAt(i)?.uri?.let { uris.add(it) } }
        if (uris.isEmpty()) data.data?.let { uris.add(it) }
        var added = 0
        uris.distinctBy { it.toString() }.forEach { uri ->
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (player.tracks.none { it.uri == uri.toString() }) {
                    player.tracks.add(ChristmasTrack(uri.toString(), audioName(uri)))
                    added++
                }
            } catch (_: Exception) { }
        }
        if (added > 0) {
            saveTracks()
            refreshList()
            player.publish()
            android.widget.Toast.makeText(this, "Добавлено композиций: $added", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(this, "Не удалось сохранить доступ к файлам. Попробуй выбрать их из памяти устройства.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun audioName(uri: android.net.Uri): String {
        var name: String? = null
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) name = it.getString(0)
            }
        } catch (_: Exception) { }
        return name?.takeIf { it.isNotBlank() } ?: "Рождественская композиция"
    }

    private fun loadTracks() {
        try {
            val array = JSONArray(prefs.getString("christmas_mp3_playlist", "[]"))
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val uri = item.optString("uri")
                if (uri.isNotBlank()) player.tracks.add(ChristmasTrack(uri, item.optString("title", "Музыка")))
            }
        } catch (_: Exception) { }
    }

    private fun saveTracks() {
        val array = JSONArray()
        player.tracks.forEach { array.put(JSONObject().put("uri", it.uri).put("title", it.title)) }
        prefs.edit().putString("christmas_mp3_playlist", array.toString()).apply()
    }

    private fun refreshList() {
        if (!::root.isInitialized) return
        val start = root.indexOfChild(root.findViewWithTag<View>("playlist_list"))
        if (start >= 0) root.removeViewAt(start)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            tag = "playlist_list"
        }
        if (player.tracks.isEmpty()) {
            list.addView(text("Пока пусто. Добавь MP3 с устройства или USB-накопителя.", 13f, 0xFFFFEBD0.toInt()).apply {
                setPadding(dp(15), dp(16), dp(15), dp(16))
                background = cardBackground()
            })
        } else {
            player.tracks.toList().forEachIndexed { index, track ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(0xCC432738.toInt(), 0xCC202330.toInt())).apply {
                        cornerRadius = dp(14).toFloat()
                        setStroke(dp(1), 0x66D9B77D)
                    }
                    setPadding(dp(12), dp(7), dp(7), dp(7))
                    isFocusable = true
                    isClickable = true
                    setOnClickListener { player.play(index) }
                }
                row.addView(text(if (index == player.currentIndex) "♫" else "♪", 22f, 0xFFFFD98D.toInt(), true))
                row.addView(text(track.title, 13f, 0xFFFFEBD0.toInt(), true).apply { maxLines = 2 },
                    LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(12) })
                row.addView(TextView(this).apply {
                    text = "✕"
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setTextColor(0xFFFFC7C7.toInt())
                    isFocusable = true
                    isClickable = true
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    setOnClickListener {
                        player.remove(index)
                        saveTracks()
                        refreshList()
                        refreshPlayer()
                    }
                })
                list.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(6) })
            }
        }
        root.addView(list)
    }

    private fun refreshPlayer() {
        if (!::player.isInitialized) return
        val playing = player.isPlaying
        ball?.text = if (playing) "Ⅱ" else "▶"
        titleView?.text = player.currentTrack?.title ?: "Добавь свои любимые MP3"
        if (!seeking) seek?.progress = if (player.duration > 0) (player.position * 1000 / player.duration).coerceIn(0, 1000) else 0
        val pos = player.position.coerceAtLeast(0)
        val dur = player.duration.coerceAtLeast(0)
        timeView?.text = String.format(Locale.getDefault(), "%02d:%02d  /  %02d:%02d",
            pos / 60000, (pos / 1000) % 60, dur / 60000, (dur / 1000) % 60)
        if (playing) {
            if (pulse?.isStarted != true) {
                pulse = ValueAnimator.ofFloat(0.94f, 1.075f).apply {
                    duration = 1350
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener {
                        val scale = it.animatedValue as Float
                        ball?.scaleX = scale
                        ball?.scaleY = scale
                    }
                    start()
                }
            }
        } else {
            pulse?.cancel()
            pulse = null
            ball?.scaleX = 1f
            ball?.scaleY = 1f
        }
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        player.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        pulse?.cancel()
        player.release()
        super.onDestroy()
    }
}
