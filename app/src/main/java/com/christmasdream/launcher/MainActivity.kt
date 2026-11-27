package com.christmasdream.launcher

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.media.AudioTrack
import android.media.AudioFormat
import android.media.AudioManager
import java.util.Calendar
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : Activity() {
    private val red = Color.rgb(174, 22, 43)
    private val deepRed = Color.rgb(91, 8, 25)
    private val wine = Color.rgb(122, 12, 34)
    private val white = Color.rgb(255, 255, 250)
    private val ivory = Color.rgb(255, 247, 235)
    private val gold = Color.rgb(255, 211, 125)
    private val sections = listOf("Главная", "Галерея", "Фильмы", "ТВ", "Игры", "Приложения", "Музыка", "Карточки", "Настройки")
    private val symbols = mapOf("Главная" to "⌂", "Фильмы" to "🎬", "ТВ" to "📺", "Игры" to "🎮", "Приложения" to "▦", "Галерея" to "▧", "Карточки" to "＋", "Музыка" to "♫", "Настройки" to "⚙")
    private var selected = "Главная"
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private lateinit var snow: SnowView
    private lateinit var wallpaperView: ImageView
    private val wallpaperPrefs by lazy { getSharedPreferences("christmas_dream_settings", MODE_PRIVATE) }
    private val wallpaperRequestCode = 4107
    private val musicRequestCode = 4108
    private lateinit var rootFrame: FrameLayout
    private val newYearHandler = Handler(Looper.getMainLooper())
    private var newYearOverlay: NewYearCelebrationView? = null
    private var santaTts: TextToSpeech? = null
    private var ttsReady = false
    private val musicPlayer by lazy { JingleMusicPlayer(applicationContext) }
    private var iceClock: IceClockView? = null
    private var wifiQuickButton: TextView? = null
    private var wifiReceiverRegistered = false
    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { refreshWifiQuickButton() }
    }
    private var usbReceiverRegistered = false
    private var usbDialogShown = false
    private val usbStorageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_MEDIA_MOUNTED -> {
                    newYearHandler.postDelayed({
                        if (!isFinishing && isUsbStorageMounted()) showUsbMusicDialog()
                    }, 700L)
                }
                Intent.ACTION_MEDIA_UNMOUNTED, Intent.ACTION_MEDIA_EJECT, Intent.ACTION_MEDIA_BAD_REMOVAL -> {
                    usbDialogShown = false
                }
            }
        }
    }
    private val newYearCheck = object : Runnable {
        override fun run() {
            checkNewYearMoment()
            if (!isFinishing) newYearHandler.postDelayed(this, 500L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = deepRed
        window.navigationBarColor = deepRed
        buildUi()
        santaTts = TextToSpeech(this) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) santaTts?.language = Locale("ru", "RU")
        }
    }

    override fun onResume() {
        super.onResume()
        newYearHandler.removeCallbacks(newYearCheck)
        newYearHandler.post(newYearCheck)
        if (wallpaperPrefs.getBoolean("jingle_music_enabled", true)) musicPlayer.start(wallpaperPrefs.getString("custom_music_uri", null))
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(wifiStateReceiver, filter)
        }
        wifiReceiverRegistered = true
        val usbFilter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addAction(Intent.ACTION_MEDIA_BAD_REMOVAL)
            addDataScheme("file")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbStorageReceiver, usbFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(usbStorageReceiver, usbFilter)
        }
        usbReceiverRegistered = true
        refreshWifiQuickButton()
    }

    override fun onStop() {
        if (wifiReceiverRegistered) {
            try { unregisterReceiver(wifiStateReceiver) } catch (_: Exception) { }
            wifiReceiverRegistered = false
        }
        if (usbReceiverRegistered) {
            try { unregisterReceiver(usbStorageReceiver) } catch (_: Exception) { }
            usbReceiverRegistered = false
        }
        super.onStop()
    }


    override fun onPause() {
        newYearHandler.removeCallbacks(newYearCheck)
        musicPlayer.pause()
        super.onPause()
    }

    override fun onDestroy() {
        newYearHandler.removeCallbacks(newYearCheck)
        santaTts?.stop()
        santaTts?.shutdown()
        santaTts = null
        musicPlayer.release()
        super.onDestroy()
    }

    private fun isUsbStorageMounted(): Boolean {
        return try {
            val manager = getSystemService(Context.STORAGE_SERVICE) as android.os.storage.StorageManager
            manager.storageVolumes.any { volume ->
                volume.isRemovable && volume.state == android.os.Environment.MEDIA_MOUNTED
            }
        } catch (_: Exception) { false }
    }

    private fun showUsbMusicDialog() {
        if (usbDialogShown || isFinishing) return
        usbDialogShown = true
        AlertDialog.Builder(this)
            .setTitle("🎄 USB-флешка подключена")
            .setMessage("На флешке может быть ваша праздничная музыка. Открыть рождественский MP3-плеер и выбрать песни?")
            .setPositiveButton("Открыть музыку") { _, _ ->
                startActivity(Intent(this, ChristmasMusicActivity::class.java)
                    .putExtra(ChristmasMusicActivity.EXTRA_OPEN_USB_PICKER, true))
            }
            .setNegativeButton("Позже", null)
            .setOnDismissListener { usbDialogShown = false }
            .show()
    }

    private fun checkNewYearMoment() {
        val now = Calendar.getInstance()
        // Run at 12:00 local TV time on 31 December 2026.
        val isNewYearMoment = now.get(Calendar.YEAR) == 2026 &&
            now.get(Calendar.MONTH) == Calendar.DECEMBER &&
            now.get(Calendar.DAY_OF_MONTH) == 31 &&
            now.get(Calendar.HOUR_OF_DAY) == 12 &&
            now.get(Calendar.MINUTE) == 0
        val key = "new_year_celebration_2026"
        if (isNewYearMoment && wallpaperPrefs.getBoolean("new_year_enabled", true) && !wallpaperPrefs.getBoolean(key, false)) {
            wallpaperPrefs.edit().putBoolean(key, true).apply()
            startNewYearCelebration()
        }
    }

    private fun playIceBell(durationMs: Int) {
        try {
            val sampleRate = 22050
            val count = sampleRate * durationMs / 1000
            val pcm = ShortArray(count) { index ->
                val t = index.toDouble() / sampleRate
                val decay = kotlin.math.exp(-4.2 * t)
                val fundamental = kotlin.math.sin(2.0 * Math.PI * 880.0 * t)
                val overtone = 0.52 * kotlin.math.sin(2.0 * Math.PI * 1320.0 * t)
                val shimmer = 0.24 * kotlin.math.sin(2.0 * Math.PI * 1760.0 * t)
                ((fundamental + overtone + shimmer) / 1.76 * decay * 0.48 * Short.MAX_VALUE).toInt().toShort()
            }
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, pcm.size * 2, AudioTrack.MODE_STATIC
            )
            track.write(pcm, 0, pcm.size)
            track.play()
            newYearHandler.postDelayed({
                try { track.stop() } catch (_: Exception) { }
                track.release()
            }, durationMs.toLong() + 250L)
        } catch (_: Exception) { }
    }

    private fun startNewYearCelebration() {
        if (newYearOverlay != null) return
        newYearOverlay = NewYearCelebrationView(this)
        rootFrame.addView(newYearOverlay, FrameLayout.LayoutParams(-1, -1))
        newYearOverlay?.bringToFront()
        // Three distinct bell chimes, followed by a cheerful spoken greeting.
        newYearHandler.postDelayed({ playIceBell(520) }, 0L)
        newYearHandler.postDelayed({ playIceBell(520) }, 850L)
        newYearHandler.postDelayed({ playIceBell(760) }, 1700L)
        newYearHandler.postDelayed({
            if (ttsReady) {
                val params = android.os.Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.82f)
                }
                santaTts?.setPitch(0.78f)
                santaTts?.setSpeechRate(0.88f)
                santaTts?.speak("Хо-хо-хо! С Новым годом!", TextToSpeech.QUEUE_FLUSH, params, "christmas_new_year_greeting")
            }
        }, 2600L)
        newYearHandler.postDelayed({
            newYearOverlay?.let { rootFrame.removeView(it) }
            newYearOverlay = null
        }, 20_000L)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 22, stroke: Int = 0x22FFFFFF): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != 0) setStroke(dp(1), stroke)
        }

    private fun gradient(colors: IntArray, radius: Int): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply { cornerRadius = dp(radius).toFloat() }

    private fun glass(radius: Int = 20, focused: Boolean = false): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            if (focused)
                intArrayOf(0xF56A2635.toInt(), 0xF0441D2B.toInt(), 0xF52A1725.toInt())
            else
                intArrayOf(0xD9362938.toInt(), 0xD02A2433.toInt(), 0xD51A202D.toInt())
        ).apply {
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(if (focused) 2 else 1), if (focused) 0xFFFFD98A.toInt() else 0x66E6C58F)
        }

    private fun label(value: String, size: Float, color: Int = white, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun buildUi() {
        val frame = FrameLayout(this)
        rootFrame = frame
        wallpaperView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Фоновое изображение лаунчера"
            val defaultResource = if (resources.configuration.screenWidthDp >= 600)
                R.drawable.christmas_background_tv else R.drawable.christmas_background_phone
            val saved = wallpaperPrefs.getString("custom_wallpaper_uri", null)
            if (saved != null) {
                try { setImageURI(android.net.Uri.parse(saved)) }
                catch (_: Exception) { setImageResource(defaultResource) }
            } else setImageResource(defaultResource)
        }
        frame.addView(wallpaperView, FrameLayout.LayoutParams(-1, -1))
        frame.addView(View(this).apply { setBackgroundColor(0x77070D18) }, FrameLayout.LayoutParams(-1, -1))
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(12))
            setBackgroundColor(Color.TRANSPARENT)
        }
        frame.addView(shell, FrameLayout.LayoutParams(-1, -1))

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        brand.addView(label("✦ CHRISTMAS DREAM ✦", 20f, gold, true))
        brand.addView(label("РОЖДЕСТВО НАЧИНАЕТСЯ ДОМА", 9f, ivory, true))
        header.addView(brand)
        val date = SimpleDateFormat("d MMMM", Locale("ru")).format(Date())
        header.addView(label("❄  $date  ❄", 13f, white, true))
        iceClock = IceClockView(this, wallpaperPrefs.getInt("ice_clock_skin", 0))
        header.addView(iceClock, LinearLayout.LayoutParams(dp(66), dp(66)).apply { leftMargin = dp(8) })
        wifiQuickButton = label("Wi-Fi\n…", 9f, white, true).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            isFocusable = true
            isClickable = true
            contentDescription = "Управление Wi-Fi: включить или выключить"
            background = shape(0xAA315A50.toInt(), 26, gold)
            setPadding(dp(3), dp(3), dp(3), dp(3))
            setOnClickListener { toggleWifi() }
        }
        header.addView(wifiQuickButton, LinearLayout.LayoutParams(dp(54), dp(54)).apply { leftMargin = dp(7) })
        val settingsButton = label("⚙", 23f, gold, true).apply {
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            contentDescription = "Открыть системные настройки телевизора"
            background = shape(0xAA59351F.toInt(), 26, gold)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setOnClickListener { openTvSettings() }
        }
        header.addView(settingsButton, LinearLayout.LayoutParams(dp(54), dp(54)).apply { leftMargin = dp(7) })
        shell.addView(header)

        val garland = TextView(this).apply {
            text = "✦  ●  ✧  ●  ✦  ●  ✧  ●  ✦  ●  ✧  ●  ✦"
            textSize = 17f
            setTextColor(gold)
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(5))
        }
        shell.addView(garland)

        nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val navScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            addView(nav)
        }
        shell.addView(navScroll)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(24))
        }
        val scroller = ScrollView(this).apply { isFillViewport = true; clipToPadding = false; addView(content) }
        shell.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))

        // Foreground snowfall is transparent and non-clickable; remote controls pass through.
        snow = SnowView()
        frame.addView(snow, FrameLayout.LayoutParams(-1, -1))
        setContentView(frame)
        render()
    }

    private fun render() {
        nav.removeAllViews()
        sections.forEach { section ->
            val active = selected == section
            val chip = label("${symbols[section]}  $section", 13f, if (active) red else white, true)
            chip.background = shape(if (active) white else 0x44FFFFFF, 18, if (active) gold else 0x66FFFFFF)
            chip.setPadding(dp(13), dp(10), dp(13), dp(10))
            chip.isFocusable = true
            chip.isClickable = true
            chip.setOnClickListener { if (section == "Музыка") startActivity(Intent(this, ChristmasMusicActivity::class.java)) else { selected = section; render() } }
            chip.setOnFocusChangeListener { v, focused ->
                if (focused) v.background = shape(white, 18, gold)
                else v.background = shape(if (active) white else 0x44FFFFFF, 18, if (active) gold else 0x66FFFFFF)
                chip.setTextColor(if (active || focused) red else white)
            }
            nav.addView(chip, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(7) })
        }
        content.removeAllViews()
        when (selected) {
            "Главная" -> home()
            "Настройки" -> settings()
            "Карточки" -> cardsManager()
            "Галерея" -> gallerySection()
            else -> appSection(selected)
        }
    }

    private fun heading(value: String) {
        content.addView(label(value, 17f, gold, true).apply { setPadding(dp(2), dp(10), 0, dp(9)) })
    }

    private fun home() {
        val wide = resources.configuration.screenWidthDp >= 600
        val hero = FrameLayout(this).apply {
            background = shape(0xA51B1520.toInt(), 26, 0x99FFFFFF.toInt())
            clipToOutline = true
            elevation = dp(8).toFloat()
        }
        val tree = ImageView(this).apply {
            setImageResource(R.drawable.christmas_tree_feature)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = "Пышная рождественская ёлка с красными и золотыми украшениями"
        }
        hero.addView(tree, FrameLayout.LayoutParams(if (wide) dp(300) else dp(178), -1, Gravity.END or Gravity.CENTER_VERTICAL))
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(14), dp(8), dp(14))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0xF00B101B.toInt(), 0xB80B101B.toInt(), 0x000B101B)
            )
        }
        copy.addView(label("ЗИМА, КОТОРАЯ\nОЖИВАЕТ ДОМА", if (wide) 25f else 19f, white, true))
        copy.addView(label("Пусть каждый вечер будет\nнаполнен теплом и чудом.", 12f, Color.rgb(255, 224, 185), true).apply {
            setPadding(0, dp(7), 0, dp(10))
        })
        copy.addView(label("✦  ДОБРО ПОЖАЛОВАТЬ  ✦", 9f, deepRed, true).apply {
            background = shape(gold, 14)
            setPadding(dp(12), dp(7), dp(12), dp(7))
        })
        hero.addView(copy, FrameLayout.LayoutParams(if (wide) dp(310) else dp(220), -1, Gravity.START or Gravity.CENTER_VERTICAL))
        content.addView(hero, LinearLayout.LayoutParams(-1, if (wide) dp(270) else dp(236)).apply { bottomMargin = dp(8) })

        heading("ВАШЕ ПРАЗДНИЧНОЕ ПРОСТРАНСТВО")
        content.addView(label("Выберите, чем наполнить этот зимний вечер", 11f, ivory).apply {
            setPadding(dp(2), 0, 0, dp(9))
        })
        val items = listOf(
            Triple("Фильмы", "▣", "Большой экран и любимые истории"),
            Triple("ТВ", "▤", "Ваши приложения для просмотра"),
            Triple("Игры", "♟", "Время для весёлых побед"),
            Triple("Приложения", "▦", "Вся ваша коллекция"),
            Triple("Галерея", "▧", "Фотографии и изображения"),
            Triple("Карточки", "＋", "Настроить главный экран"),
            Triple("Музыка", "♫", "Ваш праздничный MP3-плеер"),
            Triple("Настройки", "⚙", "Оформление и управление")
        )
        val columns = if (wide) 4 else 2
        items.chunked(columns).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEachIndexed { index, item ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_VERTICAL
                    background = glass(20)
                    setPadding(dp(14), dp(12), dp(10), dp(12))
                    isFocusable = true
                    isClickable = true
                    elevation = dp(4).toFloat()
                }
                card.addView(label(item.second, 27f, gold, true))
                card.addView(label(item.first.uppercase(Locale.getDefault()), 10f, gold, true).apply { setPadding(0, dp(3), 0, dp(3)) })
                card.addView(label(item.third, 10f, Color.rgb(238, 218, 203)).apply { maxLines = 2 })
                card.setOnClickListener { if (item.first == "Музыка") startActivity(Intent(this, ChristmasMusicActivity::class.java)) else { selected = item.first; render() } }
                card.setOnFocusChangeListener { v, focused ->
                    v.background = glass(20, focused)
                    v.scaleX = if (focused) 1.035f else 1f
                    v.scaleY = if (focused) 1.035f else 1f
                }
                row.addView(card, LinearLayout.LayoutParams(0, dp(if (wide) 126 else 116), 1f).apply {
                    rightMargin = dp(6); leftMargin = dp(3); bottomMargin = dp(8)
                })
            }
            if (group.size < columns) row.addView(View(this), LinearLayout.LayoutParams(0, dp(116), (columns - group.size).toFloat()))
            content.addView(row)
        }
        heading("МОИ КАРТОЧКИ")
        content.addView(label("Добавляй приложения и разделы на главный экран или убирай карточки без удаления самих приложений.", 11f, ivory))
        actionCard("⚙  Управление всеми карточками") { selected = "Карточки"; render() }
        actionCard("＋  Добавить карточку приложения") { manageHomeCards(true) }
        actionCard("−  Удалить карточку приложения") { manageHomeCards(false) }
        actionCard("＋  Добавить карточку раздела") { manageHomeSections(true) }
        actionCard("−  Удалить карточку раздела") { manageHomeSections(false) }
        val pinned = pinnedApps()
        if (pinned.isNotEmpty()) appGrid(pinned)
        val pinnedSections = wallpaperPrefs.getStringSet("home_card_sections", emptySet()) ?: emptySet()
        pinnedSections.forEach { section -> actionCard("✦  $section") { if (section == "Музыка") startActivity(Intent(this, ChristmasMusicActivity::class.java)) else { selected = section; render() } } }
        heading("БЫСТРЫЙ ДОСТУП")
        appGrid(installedApps().take(if (wide) 12 else 8))
    }

    private fun cardsManager() {
        heading("УПРАВЛЕНИЕ КАРТОЧКАМИ")
        content.addView(label("Здесь можно добавлять и убирать карточки приложений и разделов. Удаление карточки не удаляет само приложение.", 12f, ivory))
        actionCard("＋  ДОБАВИТЬ карточку приложения") { manageHomeCards(true) }
        actionCard("−  УБРАТЬ карточку приложения") { manageHomeCards(false) }
        actionCard("＋  ДОБАВИТЬ карточку раздела") { manageHomeSections(true) }
        actionCard("−  УБРАТЬ карточку раздела") { manageHomeSections(false) }
        heading("УЖЕ ДОБАВЛЕНО")
        val pins = pinnedApps()
        if (pins.isEmpty()) content.addView(label("Карточки приложений пока не добавлены.", 12f, ivory))
        else appGrid(pins)
        val pinnedSections = wallpaperPrefs.getStringSet("home_card_sections", emptySet()) ?: emptySet()
        if (pinnedSections.isEmpty()) content.addView(label("Карточки разделов пока не добавлены.", 12f, ivory))
        else pinnedSections.sorted().forEach { section -> actionCard("✦  $section") { selected = section; render() } }
    }

    private fun gallerySection() {
        heading("ВАША ГАЛЕРЕЯ")
        content.addView(label("Откройте фотографии и изображения из памяти телевизора или с подключённого USB-накопителя.", 12f, ivory))
        val galleryApps = installedApps().filter { info ->
            val n = (info.loadLabel(packageManager).toString() + " " + info.activityInfo.packageName).lowercase(Locale.getDefault())
            listOf("gallery", "photos", "photo", "галере", "фото", "image").any { n.contains(it) }
        }
        if (galleryApps.isNotEmpty()) {
            heading("УСТАНОВЛЕННЫЕ ПРИЛОЖЕНИЯ")
            appGrid(galleryApps)
        }
        actionCard("▧  ОТКРЫТЬ ФОТОГАЛЕРЕЮ") { openGalleryPicker() }
        actionCard("▧  Выбрать изображение из памяти или USB") { openGalleryPicker() }
        content.addView(label("Если отдельное приложение «Галерея» не установлено или скрыто производителем телевизора, Android откроет системный выбор изображений. Это позволит просматривать доступные фотографии.", 11f, ivory))
    }

    private fun openGalleryPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        try {
            startActivity(Intent.createChooser(intent, "Открыть галерею"))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_PICK).apply { type = "image/*" })
            } catch (_: Exception) {
                android.widget.Toast.makeText(this, "На телевизоре нет доступного приложения для просмотра изображений", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun pinnedApps(): List<android.content.pm.ResolveInfo> {
        val pins = wallpaperPrefs.getStringSet("home_card_packages", emptySet()) ?: emptySet()
        return installedApps().filter { it.activityInfo.packageName in pins }
    }

    private fun manageHomeCards(add: Boolean) {
        val apps = installedApps()
        if (apps.isEmpty()) return
        val current = (wallpaperPrefs.getStringSet("home_card_packages", emptySet()) ?: emptySet()).toMutableSet()
        val candidates = if (add) apps.filter { it.activityInfo.packageName !in current } else apps.filter { it.activityInfo.packageName in current }
        if (candidates.isEmpty()) {
            android.widget.Toast.makeText(this, if (add) "Все доступные приложения уже добавлены" else "На главном экране пока нет добавленных карточек", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val labels = candidates.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        AlertDialog.Builder(this).setTitle(if (add) "Добавить карточку" else "Удалить карточку с главного экрана")
            .setItems(labels) { _, which ->
                val pkg = candidates[which].activityInfo.packageName
                if (add) current.add(pkg) else current.remove(pkg)
                wallpaperPrefs.edit().putStringSet("home_card_packages", current).apply()
                render()
                android.widget.Toast.makeText(this, if (add) "Карточка добавлена" else "Карточка убрана. Приложение осталось установленным.", android.widget.Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Отмена", null).show()
    }

    private fun manageHomeSections(add: Boolean) {
        val available = sections.filter { it != "Главная" }
        val current = (wallpaperPrefs.getStringSet("home_card_sections", emptySet()) ?: emptySet()).toMutableSet()
        val candidates = if (add) available.filter { it !in current } else available.filter { it in current }
        if (candidates.isEmpty()) {
            android.widget.Toast.makeText(this, if (add) "Все разделы уже добавлены" else "Добавленных разделов нет", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(if (add) "Добавить карточку раздела" else "Убрать карточку раздела")
            .setItems(candidates.toTypedArray()) { _, which ->
                val section = candidates[which]
                if (add) current.add(section) else current.remove(section)
                wallpaperPrefs.edit().putStringSet("home_card_sections", current).apply()
                render()
            }.setNegativeButton("Отмена", null).show()
    }

    private fun refreshWifiQuickButton() {
        val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val enabled = try { manager?.isWifiEnabled == true } catch (_: Exception) { false }
        wifiQuickButton?.text = if (enabled) "Wi-Fi\nВКЛ" else "Wi-Fi\nВЫКЛ"
        wifiQuickButton?.contentDescription = if (enabled) "Wi-Fi включён. Нажмите, чтобы выключить" else "Wi-Fi выключен. Нажмите, чтобы включить"
    }

    @Suppress("DEPRECATION")
    private fun toggleWifi() {
        val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && manager != null) {
                manager.isWifiEnabled = !manager.isWifiEnabled
                refreshWifiQuickButton()
            } else {
                val panel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS
                startActivity(Intent(panel))
            }
        } catch (_: Exception) {
            try { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) } catch (_: Exception) { }
        }
    }

    private fun openTvSettings() {
        try { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        catch (_: Exception) {
            try { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) } catch (_: Exception) { }
        }
    }

    private fun chooseClockSkin() {
        val names = arrayOf("Красный лёд — красный циферблат и голубые стрелки", "Снежный кристалл — белый лёд и серебро", "Ледяной механизм — красное стекло и синие шестерёнки", "Волшебные часы — морозное сияние")
        AlertDialog.Builder(this).setTitle("Дизайн часов").setSingleChoiceItems(names, wallpaperPrefs.getInt("ice_clock_skin", 0)) { dialog, which ->
            wallpaperPrefs.edit().putInt("ice_clock_skin", which).apply()
            iceClock?.skin = which
            iceClock?.invalidate()
            dialog.dismiss()
        }.setNegativeButton("Отмена", null).show()
    }

    private fun toggleMusic() {
        val enabled = !wallpaperPrefs.getBoolean("jingle_music_enabled", true)
        wallpaperPrefs.edit().putBoolean("jingle_music_enabled", enabled).apply()
        if (enabled && !isFinishing) musicPlayer.start(wallpaperPrefs.getString("custom_music_uri", null)) else musicPlayer.stop()
        render()
    }

    private fun toggleNewYear() {
        val enabled = !wallpaperPrefs.getBoolean("new_year_enabled", true)
        wallpaperPrefs.edit().putBoolean("new_year_enabled", enabled).apply()
        render()
    }

    private fun appSection(section: String) {
        val subtitle = when (section) {
            "Фильмы" -> "ОТКРОЙТЕ ПРИЛОЖЕНИЕ ДЛЯ ПРОСМОТРА ФИЛЬМОВ"
            "ТВ" -> "ПРИЛОЖЕНИЯ ДЛЯ ТЕЛЕВИДЕНИЯ"
            "Игры" -> "ВАШИ ИГРЫ"
            else -> "ВСЕ УСТАНОВЛЕННЫЕ ПРИЛОЖЕНИЯ"
        }
        heading(subtitle)
        if (section == "Фильмы") {
            actionCard("🎬   Открыть Christmas Cinema — выбрать фильм") {
                startActivity(Intent(this, ChristmasCinemaActivity::class.java))
            }
            content.addView(label("Поддерживаются видео с USB-флешки и из памяти устройства. Выбирай несколько файлов, чтобы создать очередь просмотра.", 12f, ivory).apply {
                setPadding(dp(4), dp(6), dp(4), dp(10))
            })
        }
        val apps = installedApps().filter { app ->
            val name = app.loadLabel(packageManager).toString().lowercase(Locale.getDefault())
            when (section) {
                "Фильмы" -> listOf("youtube", "netflix", "кино", "ivi", "окко", "кинопоиск", "премьер", "видео", "медиа").any { name.contains(it) }
                "ТВ" -> listOf("tv", "телев", "iptv", "эфир", "канал").any { name.contains(it) }
                "Игры" -> try { packageManager.getApplicationInfo(app.activityInfo.packageName, 0).category == android.content.pm.ApplicationInfo.CATEGORY_GAME } catch (_: Exception) { false }
                else -> true
            }
        }
        if (apps.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = shape(ivory, 22, 0xFFFFDCA7.toInt())
                setPadding(dp(18), dp(24), dp(18), dp(24))
            }
            empty.addView(label("🎄", 34f, red, true).apply { gravity = Gravity.CENTER })
            empty.addView(label("Пока здесь пусто", 19f, red, true).apply { gravity = Gravity.CENTER })
            empty.addView(label("Подходящие установленные приложения появятся в этом разделе автоматически.", 13f, wine).apply { gravity = Gravity.CENTER; textAlignment = View.TEXT_ALIGNMENT_CENTER })
            content.addView(empty)
            return
        }
        appGrid(apps.take(60))
    }

    private fun appGrid(apps: List<android.content.pm.ResolveInfo>) {
        if (apps.isEmpty()) {
            content.addView(label("Пока нет установленных приложений.", 13f, ivory))
            return
        }
        val columns = when {
            resources.configuration.screenWidthDp >= 900 -> 6
            resources.configuration.screenWidthDp >= 600 -> 5
            resources.configuration.screenWidthDp >= 400 -> 3
            else -> 2
        }
        apps.chunked(columns).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { info ->
                val appName = info.loadLabel(packageManager).toString()
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = glass(19)
                    setPadding(dp(7), dp(10), dp(7), dp(8))
                    isFocusable = true
                    isFocusableInTouchMode = false
                    isClickable = true
                    elevation = dp(3).toFloat()
                    contentDescription = "Открыть приложение $appName"
                }
                val iconFrame = FrameLayout(this).apply {
                    background = GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(0xB85B3442.toInt(), 0xA52B2634.toInt())
                    ).apply {
                        cornerRadius = dp(15).toFloat()
                        setStroke(dp(1), 0x77E7C88D)
                    }
                }
                val icon = try { info.loadIcon(packageManager) } catch (_: Exception) { null }
                iconFrame.addView(ImageView(this).apply {
                    if (icon != null) setImageDrawable(icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = appName
                }, FrameLayout.LayoutParams(dp(42), dp(42), Gravity.CENTER))
                card.addView(iconFrame, LinearLayout.LayoutParams(dp(54), dp(54)).apply {
                    bottomMargin = dp(7)
                })
                card.addView(label(appName, 11f, ivory, true).apply {
                    gravity = Gravity.CENTER
                    maxLines = 2
                    minLines = 2
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(-1, -2))
                card.setOnClickListener { launch(info.activityInfo.packageName) }
                card.setOnFocusChangeListener { v, focused ->
                    v.background = glass(19, focused)
                    v.animate().scaleX(if (focused) 1.045f else 1f)
                        .scaleY(if (focused) 1.045f else 1f)
                        .translationZ(if (focused) dp(5).toFloat() else 0f)
                        .setDuration(140L).start()
                }
                row.addView(card, LinearLayout.LayoutParams(0, dp(112), 1f).apply {
                    rightMargin = dp(5)
                    leftMargin = dp(2)
                    bottomMargin = dp(8)
                })
            }
            if (group.size < columns) row.addView(View(this), LinearLayout.LayoutParams(0, dp(112), (columns - group.size).toFloat()))
            content.addView(row)
        }
    }

    private fun installedApps(): List<android.content.pm.ResolveInfo> {
        val phone = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val tv = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return (packageManager.queryIntentActivities(phone, PackageManager.MATCH_ALL) +
            packageManager.queryIntentActivities(tv, PackageManager.MATCH_ALL))
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
    }

    private fun launch(pkg: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) startActivity(intent)
            else android.widget.Toast.makeText(this, "Не удалось открыть приложение", android.widget.Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            android.widget.Toast.makeText(this, "Не удалось открыть приложение", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun settings() {
        heading("НАСТРОЙКИ ПРАЗДНИЧНОГО ЭКРАНА")
        val info = label("❄  Рождественская красно-белая тема\n\n✦  Снежные эффекты на фоне\n\n🎮  Управление пультом: стрелки, OK и Back\n\n📱  Сенсорное управление поддерживается\n\n🛡  Системные приложения и прошивка не изменяются", 15f, wine)
        info.background = shape(0xDDF8F3EA.toInt(), 22, 0xAAFFFFFF.toInt())
        info.setPadding(dp(18), dp(18), dp(18), dp(18))
        content.addView(info)
        actionCard("🕰   Выбрать дизайн ледяных часов") { chooseClockSkin() }
        actionCard("♫   Фоновая музыка: " + if (wallpaperPrefs.getBoolean("jingle_music_enabled", true)) "ВКЛ" else "ВЫКЛ") { toggleMusic() }
        actionCard("♫   Открыть рождественский MP3-плеер") { startActivity(Intent(this, ChristmasMusicActivity::class.java)) }
        actionCard("♫   Выбрать свою фоновую музыку") { chooseMusic() }
        actionCard("↺   Вернуть встроенную музыку") { resetMusic() }
        val selectedTrack = wallpaperPrefs.getString("custom_music_uri", null)
        content.addView(label(if (selectedTrack == null) "Сейчас: встроенная праздничная мелодия" else "Сейчас: выбран ваш аудиофайл", 11f, ivory).apply { setPadding(dp(4), dp(3), 0, dp(5)) })
        actionCard("🎆   Новогодний сюрприз: " + if (wallpaperPrefs.getBoolean("new_year_enabled", true)) "ВКЛ" else "ВЫКЛ") { toggleNewYear() }
        actionCard("🖼   Выбрать свой фон") { chooseWallpaper() }
        actionCard("↺   Вернуть праздничный фон") { resetWallpaper() }
        actionCard("↻   Обновить список приложений") { render() }
        actionCard("⌂   Вернуться к штатному экрану TCL") { returnToStock() }
    }

    private fun setDefaultWallpaper() {
        wallpaperView.setImageResource(
            if (resources.configuration.screenWidthDp >= 600)
                R.drawable.christmas_background_tv else R.drawable.christmas_background_phone
        )
    }

    private fun chooseWallpaper() {
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try { startActivityForResult(picker, wallpaperRequestCode) }
        catch (_: Exception) {
            android.widget.Toast.makeText(this, "Не удалось открыть выбор изображения", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun chooseMusic() {
        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try { startActivityForResult(picker, musicRequestCode) }
        catch (_: Exception) {
            android.widget.Toast.makeText(this, "Не удалось открыть выбор аудиофайла", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun resetMusic() {
        wallpaperPrefs.edit().remove("custom_music_uri").apply()
        musicPlayer.stop()
        if (wallpaperPrefs.getBoolean("jingle_music_enabled", true)) musicPlayer.start()
        render()
        android.widget.Toast.makeText(this, "Встроенная праздничная музыка восстановлена", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun resetWallpaper() {
        wallpaperPrefs.edit().remove("custom_wallpaper_uri").apply()
        setDefaultWallpaper()
        android.widget.Toast.makeText(this, "Праздничный фон восстановлен", android.widget.Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Android API; kept for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        if (requestCode == musicRequestCode) {
            try {
                val permission = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, permission)
                wallpaperPrefs.edit().putString("custom_music_uri", uri.toString()).apply()
                musicPlayer.stop()
                if (wallpaperPrefs.getBoolean("jingle_music_enabled", true)) musicPlayer.start(uri.toString())
                render()
                android.widget.Toast.makeText(this, "Ваша музыка установлена", android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                android.widget.Toast.makeText(this, "Не удалось сохранить доступ к этому аудиофайлу. Выберите файл из памяти устройства.", android.widget.Toast.LENGTH_LONG).show()
            }
            return
        }
        if (requestCode != wallpaperRequestCode) return
        try {
            val permission = data.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, permission)
            wallpaperPrefs.edit().putString("custom_wallpaper_uri", uri.toString()).apply()
            wallpaperView.setImageURI(uri)
            android.widget.Toast.makeText(this, "Ваш фон установлен", android.widget.Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            try {
                wallpaperView.setImageURI(uri)
                wallpaperPrefs.edit().putString("custom_wallpaper_uri", uri.toString()).apply()
                android.widget.Toast.makeText(this, "Фон установлен", android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                android.widget.Toast.makeText(this, "Не удалось загрузить это изображение", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun actionCard(value: String, action: () -> Unit) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = premiumActionBackground(false)
            setPadding(dp(17), dp(14), dp(17), dp(14))
            isFocusable = true
            isClickable = true
            isFocusableInTouchMode = false
            elevation = dp(3).toFloat()
            contentDescription = value
        }
        val accent = View(this).apply {
            background = shape(0xFFE7BD72.toInt(), 3, 0)
        }
        card.addView(accent, LinearLayout.LayoutParams(dp(3), dp(34)).apply { rightMargin = dp(14) })
        card.addView(label(value, 14f, 0xFFFFEBD0.toInt(), true), LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(label("›", 27f, 0xFFE8C47F.toInt(), true))
        card.setOnClickListener { action() }
        card.setOnFocusChangeListener { view, focused ->
            view.background = premiumActionBackground(focused)
            view.animate().scaleX(if (focused) 1.015f else 1f).scaleY(if (focused) 1.015f else 1f)
                .setDuration(130L).start()
        }
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
            bottomMargin = dp(2)
        })
    }

    private fun premiumActionBackground(focused: Boolean): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            if (focused) intArrayOf(0xFFF4DFC0.toInt(), 0xFFE8C68D.toInt())
            else intArrayOf(0xE63B2635.toInt(), 0xE625202D.toInt())
        ).apply {
            cornerRadius = dp(17).toFloat()
            setStroke(dp(if (focused) 2 else 1), if (focused) 0xFFFFF0D4.toInt() else 0x77D9B77D)
        }

    private fun returnToStock() {
        AlertDialog.Builder(this)
            .setTitle("Штатный экран")
            .setMessage("Android откроет системный выбор домашнего экрана. Выберите штатный TCL Launcher. Само приложение TCL не отключается и не изменяется.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Продолжить") { _, _ ->
                try { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { }
            }.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && selected != "Главная") {
            selected = "Главная"
            render()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private data class SnowFlake(var x: Float, var y: Float, val radius: Float, val speed: Float, val drift: Float)

    private inner class SnowView : View(this@MainActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val flakes = List(115) {
            SnowFlake(Random.nextFloat(), Random.nextFloat(), 1.8f + Random.nextFloat() * 3.2f, 0.0022f + Random.nextFloat() * 0.0040f, (Random.nextFloat() - 0.5f) * 0.0008f)
        }
        private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                flakes.forEach { f ->
                    f.y += f.speed
                    f.x += f.drift
                    if (f.y > 1.02f) { f.y = -0.02f; f.x = Random.nextFloat() }
                    if (f.x < 0f) f.x = 1f
                    if (f.x > 1f) f.x = 0f
                }
                invalidate()
            }
        }

        init {
            isClickable = false
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            animator.start()
        }

        override fun onDetachedFromWindow() {
            animator.cancel()
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            paint.color = 0xDDF7FCFF.toInt()
            flakes.forEach { f -> canvas.drawCircle(f.x * width, f.y * height, dp(f.radius.toInt().coerceAtLeast(2)).toFloat(), paint) }
        }
    }
}
