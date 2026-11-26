package com.christmasdream.launcher

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
    private val sections = listOf("Главная", "Фильмы", "ТВ", "Игры", "Приложения", "Настройки")
    private val symbols = mapOf("Главная" to "⌂", "Фильмы" to "🎬", "ТВ" to "📺", "Игры" to "🎮", "Приложения" to "▦", "Настройки" to "⚙")
    private var selected = "Главная"
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private lateinit var snow: SnowView
    private lateinit var wallpaperView: ImageView
    private val wallpaperPrefs by lazy { getSharedPreferences("christmas_dream_settings", MODE_PRIVATE) }
    private val wallpaperRequestCode = 4107
    private var wifiQuickButton: TextView? = null
    private var wifiReceiverRegistered = false
    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshWifiQuickButton()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = deepRed
        window.navigationBarColor = deepRed
        buildUi()
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
        refreshWifiQuickButton()
    }

    override fun onStop() {
        if (wifiReceiverRegistered) {
            try { unregisterReceiver(wifiStateReceiver) } catch (_: Exception) { }
            wifiReceiverRegistered = false
        }
        super.onStop()
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
            intArrayOf(0x663C4658, 0x55303A4B, 0x66302B3A)
        ).apply {
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(if (focused) 2 else 1), if (focused) gold else 0x88FFFFFF.toInt())
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
        header.addView(label("❄  $date  ❄", 12f, white, true))
        wifiQuickButton = label("Wi-Fi\n…", 9f, white, true).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            isFocusable = true
            isClickable = true
            contentDescription = "Управление Wi-Fi: включить или выключить"
            background = shape(0xAA315A50.toInt(), 26, gold)
            setPadding(dp(3), dp(3), dp(3), dp(3))
            setOnClickListener { toggleWifi() }
            setOnFocusChangeListener { view, focused ->
                view.background = shape(
                    if (focused) 0xFF39775F.toInt() else 0xAA315A50.toInt(),
                    26,
                    if (focused) white else gold
                )
            }
        }
        header.addView(wifiQuickButton, LinearLayout.LayoutParams(dp(54), dp(54)).apply {
            leftMargin = dp(6)
        })
        val settingsButton = label("⚙", 23f, gold, true).apply {
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            contentDescription = "Открыть системные настройки телевизора"
            background = shape(0xAA59351F.toInt(), 26, gold)
            setOnClickListener { openTvSettings() }
            setOnFocusChangeListener { view, focused ->
                view.background = shape(
                    if (focused) 0xFF825126.toInt() else 0xAA59351F.toInt(),
                    26,
                    if (focused) white else gold
                )
            }
        }
        header.addView(settingsButton, LinearLayout.LayoutParams(dp(54), dp(54)).apply {
            leftMargin = dp(7)
        })
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
            chip.setOnClickListener { selected = section; render() }
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
        copy.addView(label("ЗИМА, КОТОРАЯ\\nОЖИВАЕТ ДОМА", if (wide) 25f else 19f, white, true))
        copy.addView(label("Пусть каждый вечер будет\\nнаполнен теплом и чудом.", 12f, Color.rgb(255, 224, 185), true).apply {
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
                card.setOnClickListener { selected = item.first; render() }
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
        heading("БЫСТРЫЙ ДОСТУП")
        appGrid(installedApps().take(if (wide) 12 else 8))
    }

    private fun appSection(section: String) {
        val subtitle = when (section) {
            "Фильмы" -> "ОТКРОЙТЕ ПРИЛОЖЕНИЕ ДЛЯ ПРОСМОТРА ФИЛЬМОВ"
            "ТВ" -> "ПРИЛОЖЕНИЯ ДЛЯ ТЕЛЕВИДЕНИЯ"
            "Игры" -> "ВАШИ ИГРЫ"
            else -> "ВСЕ УСТАНОВЛЕННЫЕ ПРИЛОЖЕНИЯ"
        }
        heading(subtitle)
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
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = glass(18)
                    setPadding(dp(5), dp(9), dp(5), dp(9))
                    isFocusable = true
                    isClickable = true
                }
                val icon = try { info.loadIcon(packageManager) } catch (_: Exception) { null }
                card.addView(ImageView(this).apply {
                    if (icon != null) setImageDrawable(icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }, LinearLayout.LayoutParams(dp(42), dp(42)))
                card.addView(label(info.loadLabel(packageManager).toString(), 11f, white, true).apply {
                    gravity = Gravity.CENTER
                    maxLines = 2
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                })
                card.setOnClickListener { launch(info.activityInfo.packageName) }
                card.setOnFocusChangeListener { v, focused ->
                    v.background = glass(18, focused)
                    v.scaleX = if (focused) 1.04f else 1f
                    v.scaleY = if (focused) 1.04f else 1f
                }
                row.addView(card, LinearLayout.LayoutParams(0, dp(94), 1f).apply { rightMargin = dp(5); bottomMargin = dp(7) })
            }
            if (group.size < columns) row.addView(View(this), LinearLayout.LayoutParams(0, dp(94), (columns - group.size).toFloat()))
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
        actionCard("⚙   Открыть системные настройки телевизора") { openTvSettings() }
        actionCard("⌁   Настройки Wi-Fi") { openWifiSettings() }
        actionCard("🔊   Настройки звука") { openSettingsAction(Settings.ACTION_SOUND_SETTINGS) }
        actionCard("🖥   Настройки изображения") { openSettingsAction(Settings.ACTION_DISPLAY_SETTINGS) }
        actionCard("▦   Управление приложениями") { openSettingsAction(Settings.ACTION_APPLICATION_SETTINGS) }
        actionCard("🖼   Выбрать свой фон") { chooseWallpaper() }
        actionCard("↺   Вернуть праздничный фон") { resetWallpaper() }
        actionCard("↻   Обновить список приложений") { render() }
        actionCard("⌂   Вернуться к штатному экрану TCL") { returnToStock() }
    }

    private fun refreshWifiQuickButton() {
        val button = wifiQuickButton ?: return
        val enabled = try {
            (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
        } catch (_: Exception) {
            false
        }
        button.text = if (enabled) "Wi-Fi\nВКЛ" else "Wi-Fi\nВЫКЛ"
        button.contentDescription = if (enabled) {
            "Wi-Fi включён. Нажмите, чтобы выключить"
        } else {
            "Wi-Fi выключен. Нажмите, чтобы включить"
        }
        button.background = shape(
            if (enabled) 0xAA315A50.toInt() else 0xAA51313B.toInt(),
            26,
            gold
        )
    }

    @Suppress("DEPRECATION")
    private fun toggleWifi() {
        try {
            val manager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val targetEnabled = !manager.isWifiEnabled
            val accepted = manager.setWifiEnabled(targetEnabled)
            if (accepted) {
                android.widget.Toast.makeText(
                    this,
                    if (targetEnabled) "Запрос на включение Wi-Fi отправлен" else "Запрос на выключение Wi-Fi отправлен",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                openWifiSettings()
            }
        } catch (_: SecurityException) {
            openWifiSettings()
        } catch (_: Exception) {
            openWifiSettings()
        }
    }

    private fun openWifiSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(Settings.Panel.ACTION_WIFI)
            } else {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            } catch (_: Exception) {
                android.widget.Toast.makeText(this, "Не удалось открыть настройки Wi-Fi", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openTvSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) {
            android.widget.Toast.makeText(this, "Не удалось открыть системные настройки телевизора", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun openSettingsAction(action: String) {
        try {
            startActivity(Intent(action))
        } catch (_: Exception) {
            openTvSettings()
        }
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

    private fun resetWallpaper() {
        wallpaperPrefs.edit().remove("custom_wallpaper_uri").apply()
        setDefaultWallpaper()
        android.widget.Toast.makeText(this, "Праздничный фон восстановлен", android.widget.Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Android API; kept for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != wallpaperRequestCode || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
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
        val v = label(value, 15f, red, true)
        v.background = shape(white, 18, 0xFFFFDCA7.toInt())
        v.setPadding(dp(16), dp(15), dp(16), dp(15))
        v.isFocusable = true
        v.setOnClickListener { action() }
        v.setOnFocusChangeListener { view, focused -> view.background = shape(if (focused) Color.rgb(255, 224, 190) else white, 18, red) }
        content.addView(v, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
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
        private val flakes = List(62) {
            SnowFlake(Random.nextFloat(), Random.nextFloat(), 1.2f + Random.nextFloat() * 2.6f, 0.0015f + Random.nextFloat() * 0.0035f, (Random.nextFloat() - 0.5f) * 0.0008f)
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
            paint.color = 0x99FFFFFF.toInt()
            flakes.forEach { f -> canvas.drawCircle(f.x * width, f.y * height, dp(f.radius.toInt().coerceAtLeast(1)).toFloat(), paint) }
        }
    }
}
