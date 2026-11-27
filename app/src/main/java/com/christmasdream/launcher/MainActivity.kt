package com.christmasdream.launcher

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.*
import android.view.*
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.random.Random

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("ramadan", MODE_PRIVATE) }
    private lateinit var root: FrameLayout
    private lateinit var clockText: TextView
    private lateinit var prayerText: TextView
    private lateinit var statusText: TextView
    private var prayerTimes = linkedMapOf<String, String>()
    private var nasheedPlayer: MediaPlayer? = null
    private val currentSection get() = when (prefs.getString("launcher_section", "Главная")) { "Фильмы" -> "Кино"; else -> prefs.getString("launcher_section", "Главная") ?: "Главная" }
    private val prayerNames = linkedMapOf("fajr" to "Фаджр", "dhuhr" to "Зухр", "asr" to "Аср", "maghrib" to "Магриб", "isha" to "Иша")
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("ru")).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }
    private var heroScene: RamadanHeroScene? = null
    private val updateClock = object : Runnable {
        override fun run() {
            if (::clockText.isInitialized) clockText.text = when (prefs.getInt("clock", 2)) {
                0 -> SimpleDateFormat("HH:mm:ss", Locale("ar")).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }.format(Date())
                1 -> "✧  " + SimpleDateFormat("HH:mm", Locale("ru")).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }.format(Date()) + "  ✧"
                2 -> SimpleDateFormat("HH:mm:ss", Locale("ru")).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }.format(Date())
                else -> "۞  " + SimpleDateFormat("HH:mm", Locale("ru")).apply { timeZone = TimeZone.getTimeZone("Europe/Moscow") }.format(Date()) + "  ۞"
            }
            if (::prayerText.isInitialized) updateNextPrayer()
            heroScene?.invalidate()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(3, 24, 19)
        window.navigationBarColor = Color.rgb(3, 24, 19)
        prayerNames.keys.forEach { key ->
            prefs.getString("prayer_$key", null)?.let { prayerTimes[key] = it }
        }
        buildScreen()
        handler.post(updateClock)
        if (prefs.getBoolean("nasheed_enabled", true)) startNasheed()
        loadPrayerTimes()
        PrayerAlarm.scheduleNext(this)
    }

    override fun onResume() {
        super.onResume()
        if (prefs.getBoolean("nasheed_enabled", true)) startNasheed()
        heroScene?.invalidate()
    }

    override fun onPause() {
        nasheedPlayer?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateClock)
        nasheedPlayer?.release()
        nasheedPlayer = null
        super.onDestroy()
    }

    private fun startNasheed() {
        if (nasheedPlayer?.isPlaying == true) return
        try {
            if (nasheedPlayer == null) {
                val id = resources.getIdentifier("nasheed", "raw", packageName)
                if (id == 0) return
                nasheedPlayer = MediaPlayer.create(this, id)?.apply {
                    setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
                    isLooping = true
                    setVolume(0.18f, 0.18f)
                    setOnErrorListener { mp, _, _ -> mp.reset(); true }
                }
            }
            nasheedPlayer?.start()
        } catch (_: Exception) { }
    }

    private fun toggleNasheed() {
        val enabled = !prefs.getBoolean("nasheed_enabled", true)
        prefs.edit().putBoolean("nasheed_enabled", enabled).apply()
        if (enabled) startNasheed() else nasheedPlayer?.pause()
        Toast.makeText(this, if (enabled) "Нашид включён" else "Нашид выключен", Toast.LENGTH_SHORT).show()
        buildScreen()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun cardBg(focused: Boolean = false) = GradientDrawable(
        GradientDrawable.Orientation.TL_BR,
        intArrayOf(0xB91C493B.toInt(), 0x9B0B2B25.toInt(), 0xA60B1D1A.toInt())
    ).apply {
        cornerRadius = dp(18).toFloat()
        setStroke(dp(if (focused) 2 else 1), if (focused) 0xFFFFD78B.toInt() else 0x776FD9A6)
    }
    private fun text(s: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun buildScreen() {
        root = FrameLayout(this)
        heroScene = null
        root.addView(RamadanSky(this), FrameLayout.LayoutParams(-1, -1))
        val shade = View(this).apply { setBackgroundColor(0x3300120D) }
        root.addView(shade, FrameLayout.LayoutParams(-1, -1))
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(24))
        }
        scroll.addView(page)
        root.addView(scroll)
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val title = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        title.addView(text("ليلة مباركة", 15f, 0xFFFFD88A.toInt(), true))
        title.addView(text(if (currentSection == "Главная") "РАМАДАН • ИЗУМРУДНАЯ НОЧЬ" else "‹  ВЕРНУТЬСЯ НА ГЛАВНУЮ", 11f, 0xFFD8F5E5.toInt(), true))
        title.isClickable = true
        title.isFocusable = currentSection != "Главная"
        title.setOnClickListener {
            if (currentSection != "Главная") {
                prefs.edit().putString("launcher_section", "Главная").apply()
                buildScreen()
            }
        }
        header.addView(title)
        val date = SimpleDateFormat("d MMMM yyyy", Locale("ru")).apply {
            timeZone = TimeZone.getTimeZone("Europe/Moscow")
        }.format(Date())
        header.addView(text("Грозный  •  $date", 12f, 0xFFE6F7ED.toInt(), true))
        val settingsButton = text("⚙", 23f, 0xFFFFD98A.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xD9143B30.toInt())
                setStroke(dp(2), 0xFFFFD98A.toInt())
            }
            setPadding(dp(12), dp(12), dp(12), dp(12))
            isFocusable = true
            contentDescription = "Открыть системные настройки телевизора"
            setOnClickListener { openSystemSettings() }
            setOnFocusChangeListener { v, focused ->
                v.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (focused) 0xFF245541.toInt() else 0xD9143B30.toInt())
                    setStroke(dp(if (focused) 3 else 2), 0xFFFFD98A.toInt())
                }
            }
        }
        header.addView(settingsButton, LinearLayout.LayoutParams(dp(50), dp(50)).apply { leftMargin = dp(9) })
        page.addView(header)
        if (currentSection == "Главная") {
        page.addView(createHeroScene(), LinearLayout.LayoutParams(-1, dp(360)).apply {
            bottomMargin = dp(7)
        })
        val timePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = cardBg(); setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        clockText = text("00:00:00", 34f, 0xFFFFE6B1.toInt(), true).apply { gravity = Gravity.CENTER }
        timePanel.addView(clockText)
        timePanel.addView(text("ВРЕМЯ ГРОЗНОГО", 9f, 0xFFBDEDD1.toInt(), true).apply { gravity = Gravity.CENTER })
        page.addView(timePanel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        page.addView(text("ВРЕМЕНА НАМАЗОВ • ГРОЗНЫЙ", 14f, 0xFFFFD88A.toInt(), true).apply { setPadding(0, dp(16), 0, dp(6)) })
        prayerText = text("Загружаем расписание молитв…", 13f, 0xFFE5F5EA.toInt())
        prayerText.background = cardBg(); prayerText.setPadding(dp(14), dp(12), dp(14), dp(12))
        page.addView(prayerText)
        statusText = text("Источник расписания: ДУМ ЧР • UTC+3", 10f, 0xFFB8D7C6.toInt())
        page.addView(statusText)
        page.addView(text("МОИ ПРИЛОЖЕНИЯ", 14f, 0xFFFFD88A.toInt(), true).apply {
            setPadding(0, dp(14), 0, dp(5))
        })
        page.addView(text("На экране только выбранные вами плитки. Установленные приложения не удаляются.", 10f, 0xFFBDEDD1.toInt()))
        page.addView(buildInstalledAppsGrid(), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        } else if (currentSection == "Календарь") {
            buildHijriCalendar(page)
        } else if (currentSection == "Настройки") {
            page.addView(text("НАСТРОЙКИ ЛАУНЧЕРА", 17f, 0xFFFFD88A.toInt(), true).apply { setPadding(0, dp(18), 0, dp(8)) })
            val musicSetting = text("♫  Фоновый нашид: " + if (prefs.getBoolean("nasheed_enabled", true)) "ВКЛ" else "ВЫКЛ", 15f, Color.WHITE, true).apply {
                background = cardBg(); setPadding(dp(16), dp(14), dp(16), dp(14)); isFocusable = true; setOnClickListener { toggleNasheed() }
                setOnFocusChangeListener { v, focused -> v.background = cardBg(focused) }
            }
            page.addView(musicSetting)
            val adhanSetting = text("🕌  Настройки азана", 15f, Color.WHITE, true).apply {
                background = cardBg(); setPadding(dp(16), dp(14), dp(16), dp(14)); isFocusable = true; setOnClickListener { soundSettings() }
                setOnFocusChangeListener { v, focused -> v.background = cardBg(focused) }
            }
            page.addView(adhanSetting, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            val playAdhan = text("▶  ПРОСЛУШАТЬ ЗАПИСЬ АЗАНА", 14f, 0xFFFFD88A.toInt(), true).apply {
                background = cardBg(true); setPadding(dp(16), dp(14), dp(16), dp(14)); isFocusable = true
                setOnClickListener { playAdhanNow() }
                setOnFocusChangeListener { v, focused -> v.background = cardBg(focused) }
            }
            page.addView(playAdhan, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            val catSetting = text("🐈  Выбрать котёнка", 15f, Color.WHITE, true).apply {
                background = cardBg(); setPadding(dp(16), dp(14), dp(16), dp(14)); isFocusable = true; setOnClickListener { chooseCat() }
                setOnFocusChangeListener { v, focused -> v.background = cardBg(focused) }
            }
            page.addView(catSetting, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            val clockSetting = text("◷  Выбрать стиль часов", 15f, Color.WHITE, true).apply {
                background = cardBg(); setPadding(dp(16), dp(14), dp(16), dp(14)); isFocusable = true
                setOnClickListener { prefs.edit().putInt("clock", (prefs.getInt("clock", 2) + 1) % 4).apply(); buildScreen() }
                setOnFocusChangeListener { v, focused -> v.background = cardBg(focused) }
            }
            page.addView(clockSetting, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        } else if (currentSection == "Музыка") {
            page.addView(text("МУЗЫКА И НАШИД", 17f, 0xFFFFD88A.toInt(), true).apply { setPadding(0, dp(18), 0, dp(8)) })
            page.addView(text("Тихий нашид играет на главном экране. Здесь его можно включить или выключить.", 12f, 0xFFE5F5EA.toInt()))
            val musicToggle = text("♫  ФОНОВЫЙ НАШИД: " + if (prefs.getBoolean("nasheed_enabled", true)) "ВКЛЮЧЁН" else "ВЫКЛЮЧЕН", 15f, 0xFFFFD88A.toInt(), true).apply {
                background = cardBg(true); setPadding(dp(16), dp(15), dp(16), dp(15)); isFocusable = true
                setOnClickListener { toggleNasheed() }
                setOnFocusChangeListener { v, focused -> v.background = cardBg(focused) }
            }
            page.addView(musicToggle, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            val testNasheed = text("▶  ПРОВЕРИТЬ ЗВУК НАШИДА", 13f, Color.WHITE, true).apply {
                background = cardBg(); setPadding(dp(16), dp(13), dp(16), dp(13)); isFocusable = true
                setOnClickListener { prefs.edit().putBoolean("nasheed_enabled", true).apply(); startNasheed(); Toast.makeText(this@MainActivity, "Запущен фоновый нашид", Toast.LENGTH_SHORT).show(); buildScreen() }
                setOnFocusChangeListener { v, focused -> v.background = cardBg(focused) }
            }
            page.addView(testNasheed, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
            page.addView(text("УСТАНОВЛЕННЫЕ МУЗЫКАЛЬНЫЕ ПРИЛОЖЕНИЯ", 13f, 0xFFFFD88A.toInt(), true))
            page.addView(buildAppsGrid(getAppsForSection("Музыка")), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        } else {
            val apps = getAppsForSection(currentSection)
            page.addView(text(when(currentSection) { "Кино" -> "КИНО И ВИДЕО"; "Игры" -> "ВАШИ ИГРЫ"; else -> "УСТАНОВЛЕННЫЕ ПРИЛОЖЕНИЯ" }, 17f, 0xFFFFD88A.toInt(), true).apply { setPadding(0, dp(18), 0, dp(8)) })
            page.addView(buildAppsGrid(apps), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        }
        setContentView(root)
    }



    override fun onBackPressed() {
        if (currentSection != "Главная") {
            prefs.edit().putString("launcher_section", "Главная").apply()
            buildScreen()
        } else {
            super.onBackPressed()
        }
    }

    private fun openSystemSettings() {
        try {
            startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        } catch (_: Exception) {
            try {
                startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, "Настройки телевизора недоступны", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createHeroScene(): FrameLayout {
        val frame = FrameLayout(this)
        val scene = RamadanHeroScene(
            this,
            { isMinaretLit() },
            { prefs.getInt("cat", 0) }
        ) { showPrayerInfo() }
        heroScene = scene
        frame.addView(scene, FrameLayout.LayoutParams(-1, -1))

        val dots = text("⋯", 27f, 0xFFFFD98A.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xE817382D.toInt())
                setStroke(dp(2), 0xFFFFD98A.toInt())
            }
            isFocusable = true
            contentDescription = "Дополнительное меню рядом с котёнком"
            setOnClickListener { showOverflowMenu(this) }
            setOnFocusChangeListener { v, focused ->
                v.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (focused) 0xFF285A43.toInt() else 0xE817382D.toInt())
                    setStroke(dp(if (focused) 3 else 2), 0xFFFFD98A.toInt())
                }
            }
        }
        frame.addView(dots, FrameLayout.LayoutParams(dp(48), dp(48)))
        frame.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            dots.x = view.width * 0.265f
            dots.y = view.height * 0.735f
        }
        return frame
    }

    private fun showPrayerInfo() {
        val times = prayerNames.entries.joinToString("\n") { (key, name) ->
            "$name  •  ${prayerTimes[key] ?: "--:--"}"
        }
        val state = if (isMinaretLit()) "Сейчас включена подсветка мечети." else "Подсветка включается к началу времени каждого намаза."
        AlertDialog.Builder(this)
            .setTitle("Мечеть Рамадана • Грозный")
            .setMessage("$times\n\n$state")
            .setPositiveButton("Понятно", null)
            .show()
    }

    private fun showOverflowMenu(anchor: View) {
        val items = listOf(
            "▦   МОИ ПЛИТКИ НА ГЛАВНОЙ" to { showHomeTilesManager() },
            "☾   КАЛЕНДАРЬ ХИДЖРЫ" to { openSection("Календарь") },
            "◖   АЗАН И ЗВУКИ" to { soundSettings() },
            "♫   ФОНОВЫЙ НАШИД: " + if (prefs.getBoolean("nasheed_enabled", true)) "ВКЛ" else "ВЫКЛ" to { toggleNasheed() },
            "♧   ВЫБРАТЬ КОТЁНКА" to { chooseCat() },
            "◷   СТИЛЬ ЧАСОВ" to {
                prefs.edit().putInt("clock", (prefs.getInt("clock", 2) + 1) % 4).apply()
                buildScreen()
            },
            "⚙   НАСТРОЙКИ ЛАУНЧЕРА" to { openSection("Настройки") },
            "♫   МУЗЫКАЛЬНЫЕ ПРИЛОЖЕНИЯ" to { openSection("Музыка") },
            "▣   КИНО И ВИДЕО" to { openSection("Кино") },
            "✧   ИГРЫ" to { openSection("Игры") },
            "▤   ВСЕ ПРИЛОЖЕНИЯ" to { openSection("Приложения") },
            "⌂   НА ГЛАВНУЮ" to { openSection("Главная") }
        )
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF174A39.toInt(), 0xFF08251F.toInt(), 0xFF041712.toInt())).apply {
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), 0xFFD8B36B.toInt())
            }
            setPadding(dp(9), dp(10), dp(9), dp(10))
        }
        panel.addView(text("РАМАДАН  •  МЕНЮ", 12f, 0xFFFFD98A.toInt(), true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(12))
        })
        items.forEachIndexed { index, item ->
            val row = text(item.first, 11f, 0xFFEAF7EF.toInt(), true).apply {
                setPadding(dp(13), dp(11), dp(10), dp(11))
                background = GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(0x442C7658)
                    setStroke(dp(1), 0x5577B994)
                }
                isFocusable = true
                isClickable = true
                setOnClickListener {
                    menu.dismiss()
                    item.second.invoke()
                }
                setOnFocusChangeListener { v, focused ->
                    v.background = GradientDrawable().apply {
                        cornerRadius = dp(12).toFloat()
                        setColor(if (focused) 0xFF285F47.toInt() else 0x442C7658)
                        setStroke(dp(if (focused) 2 else 1), if (focused) 0xFFFFD98A.toInt() else 0x5577B994)
                    }
                }
            }
            panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) })
        }
        val scroller = ScrollView(this).apply { isFillViewport = false; addView(panel) }
        val menu = PopupWindow(scroller, dp(286), dp(440), true).apply {
            setBackgroundDrawable(GradientDrawable().apply { setColor(0xFF08251F.toInt()); cornerRadius = dp(20).toFloat() })
            elevation = dp(18).toFloat()
            isOutsideTouchable = true
            setOnDismissListener { anchor.isSelected = false }
        }
        menu.showAsDropDown(anchor, dp(4), -dp(430), Gravity.START)
    }

    private fun openSection(section: String) {
        prefs.edit().putString("launcher_section", section).apply()
        buildScreen()
    }

    private fun showHomeTilesManager() {
        val apps = getInstalledLaunchableApps()
        if (apps.isEmpty()) {
            Toast.makeText(this, "Не найдены приложения для главного экрана", Toast.LENGTH_SHORT).show()
            return
        }
        val selected = prefs.getStringSet("home_apps", emptySet())?.toSet() ?: emptySet()
        val checked = BooleanArray(apps.size) { index ->
            selected.contains(apps[index].activityInfo.packageName)
        }
        val labels = apps.map { info -> info.loadLabel(packageManager).toString() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Плитки главного экрана")
            .setMessage("Отметьте приложения, которые хотите видеть на главном экране. Снятие отметки убирает только плитку — само приложение остаётся установленным.")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton("Сохранить") { _, _ ->
                val packages = apps.indices.filter { checked[it] }
                    .map { apps[it].activityInfo.packageName }.toSet()
                prefs.edit().putStringSet("home_apps", packages).apply()
                buildScreen()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun buildHijriCalendar(page: LinearLayout) {
        val todayCalendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"), Locale("ru"))
        val dayAdjustment = prefs.getInt("hijri_adjustment_days", 1).coerceIn(-2, 2)
        val today = HijriCalendarMath.fromGregorian(
            todayCalendar.get(Calendar.YEAR),
            todayCalendar.get(Calendar.MONTH) + 1,
            todayCalendar.get(Calendar.DAY_OF_MONTH) + dayAdjustment
        )
        val monthOffset = prefs.getInt("hijri_month_offset", 0)
        val absoluteMonth = today.year * 12 + today.month - 1 + monthOffset
        val shownYear = Math.floorDiv(absoluteMonth, 12)
        val shownMonth = Math.floorMod(absoluteMonth, 12) + 1
        val monthName = HijriCalendarMath.monthNames[shownMonth - 1]
        val monthDays = HijriCalendarMath.monthLength(shownYear, shownMonth)
        val firstGregorian = HijriCalendarMath.toGregorian(shownYear, shownMonth, 1, dayAdjustment)
        val firstCalendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"), Locale("ru")).apply {
            clear()
            set(firstGregorian.year, firstGregorian.month - 1, firstGregorian.day)
        }
        val firstWeekday = (firstCalendar.get(Calendar.DAY_OF_WEEK) + 5) % 7

        page.addView(text("КАЛЕНДАРЬ ХИДЖРЫ", 18f, 0xFFFFD88A.toInt(), true).apply {
            setPadding(0, dp(18), 0, dp(5))
        })
        page.addView(text(
            "Сегодня: ${today.day} ${HijriCalendarMath.monthNames[today.month - 1]} ${today.year} г.х.",
            13f, 0xFFE5F5EA.toInt(), true
        ).apply {
            background = cardBg(true)
            setPadding(dp(13), dp(12), dp(13), dp(12))
        })
        val monthBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(8))
        }
        fun monthButton(label: String, delta: Int): TextView = text(label, 23f, 0xFFFFD88A.toInt(), true).apply {
            gravity = Gravity.CENTER
            background = cardBg()
            isFocusable = true
            setPadding(dp(15), dp(5), dp(15), dp(5))
            setOnClickListener {
                prefs.edit().putInt("hijri_month_offset", monthOffset + delta).apply()
                buildScreen()
            }
            setOnFocusChangeListener { v, focused -> v.background = cardBg(focused) }
        }
        monthBar.addView(monthButton("‹", -1))
        monthBar.addView(text("$monthName  $shownYear г.х.", 16f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(0, -2, 1f))
        monthBar.addView(monthButton("›", 1))
        page.addView(monthBar)
        val adjustmentRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        adjustmentRow.addView(text("Поправка даты: ${if (dayAdjustment > 0) "+" else ""}$dayAdjustment дн.", 11f, 0xFFBDEDD1.toInt(), true),
            LinearLayout.LayoutParams(0, -2, 1f))
        listOf("−" to -1, "+" to 1).forEach { (label, delta) ->
            val adjustButton = text(label, 18f, 0xFFFFD88A.toInt(), true).apply {
                gravity = Gravity.CENTER
                background = cardBg()
                setPadding(dp(13), dp(4), dp(13), dp(4))
                isFocusable = true
                setOnClickListener {
                    prefs.edit().putInt("hijri_adjustment_days", (dayAdjustment + delta).coerceIn(-2, 2)).apply()
                    buildScreen()
                }
                setOnFocusChangeListener { v, focused -> v.background = cardBg(focused) }
            }
            adjustmentRow.addView(adjustButton, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(5) })
        }
        page.addView(adjustmentRow)

        val weekdays = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
        val weekdayRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        weekdays.forEach { name ->
            weekdayRow.addView(text(name, 10f, 0xFFBDEDD1.toInt(), true).apply {
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(0, dp(30), 1f))
        }
        page.addView(weekdayRow)

        val rows = (firstWeekday + monthDays + 6) / 7
        repeat(rows) { rowIndex ->
            val week = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            repeat(7) { column ->
                val cellIndex = rowIndex * 7 + column
                val hijriDay = cellIndex - firstWeekday + 1
                if (hijriDay in 1..monthDays) {
                    val isToday = monthOffset == 0 && today.day == hijriDay
                    val gregorian = HijriCalendarMath.toGregorian(shownYear, shownMonth, hijriDay, dayAdjustment)
                    val cell = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        background = cardBg(isToday)
                        setPadding(dp(2), dp(5), dp(2), dp(4))
                        isFocusable = true
                        contentDescription = "$hijriDay $monthName, ${gregorian.day}.${gregorian.month}.${gregorian.year}"
                        addView(text(hijriDay.toString(), 16f,
                            if (isToday) 0xFFFFD88A.toInt() else Color.WHITE, true).apply {
                            gravity = Gravity.CENTER
                        })
                        addView(text(gregorian.day.toString(), 9f, 0xFFBDEDD1.toInt()).apply {
                            gravity = Gravity.CENTER
                        })
                        setOnFocusChangeListener { v, focused -> v.background = cardBg(focused || isToday) }
                    }
                    week.addView(cell, LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                        rightMargin = dp(3)
                        bottomMargin = dp(4)
                    })
                } else {
                    week.addView(View(this), LinearLayout.LayoutParams(0, dp(58), 1f).apply {
                        rightMargin = dp(3)
                        bottomMargin = dp(4)
                    })
                }
            }
            page.addView(week)
        }

        val observance = when (shownMonth) {
            9 -> "Рамадан — месяц поста, Корана и милости"
            10 -> "Шавваль: первый день месяца — Ид аль-Фитр"
            12 -> "Зуль-хиджа — месяц хаджа; 10-й день — Ид аль-Адха"
            else -> "Хиджра — лунный календарь из 12 месяцев"
        }
        page.addView(text(observance, 12f, 0xFFFFD88A.toInt(), true).apply {
            background = cardBg()
            setPadding(dp(12), dp(11), dp(12), dp(11))
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        page.addView(text(
            "Календарь работает без интернета. Это расчётный исламский календарь; из-за местного наблюдения луны дата может отличаться на один день.",
            10f, 0xFFBDEDD1.toInt()
        ).apply { setPadding(dp(3), dp(9), dp(3), dp(12)) })
        val todayButton = text("◎  Вернуться к текущему месяцу", 13f, 0xFFFFD88A.toInt(), true).apply {
            background = cardBg()
            setPadding(dp(13), dp(12), dp(13), dp(12))
            isFocusable = true
            setOnClickListener {
                prefs.edit().putInt("hijri_month_offset", 0).apply()
                buildScreen()
            }
            setOnFocusChangeListener { v, focused -> v.background = cardBg(focused) }
        }
        page.addView(todayButton)
    }

    private fun isMinaretLit(): Boolean {
        if (prayerTimes.isEmpty()) return false
        val now = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"))
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return prayerTimes.values.any { value ->
            try {
                val parts = value.take(5).split(":")
                val prayerMinute = parts[0].toInt() * 60 + parts[1].toInt()
                nowMinutes >= prayerMinute && nowMinutes < prayerMinute + 20
            } catch (_: Exception) { false }
        }
    }

    private fun getAppsForSection(section: String): List<android.content.pm.ResolveInfo> {
        val apps = getInstalledLaunchableApps()
        return when (section) {
            "Музыка" -> apps.filter { app ->
                val label = app.loadLabel(packageManager).toString().lowercase(Locale.getDefault())
                listOf("музык", "music", "spotify", "soundcloud", "deezer", "радио", "radio", "youtube music", "яндекс музыка", "amazon music", "tunein").any { label.contains(it) }
            }
            "Игры" -> apps.filter { app ->
                try { packageManager.getApplicationInfo(app.activityInfo.packageName, 0).category == android.content.pm.ApplicationInfo.CATEGORY_GAME }
                catch (_: Exception) { false }
            }
            "Кино" -> apps.filter { app ->
                val label = app.loadLabel(packageManager).toString().lowercase(Locale.getDefault())
                listOf("youtube", "netflix", "кино", "ivi", "окко", "кинопоиск", "видео", "медиа", "prime video", "plex").any { label.contains(it) }
            }
            else -> apps
        }
    }

    private fun buildAppsGrid(apps: List<android.content.pm.ResolveInfo>): LinearLayout {
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (apps.isEmpty()) {
            grid.addView(text("В этом разделе пока нет найденных приложений.", 13f, 0xFFE5F5EA.toInt()))
            return grid
        }
        val columns = if (resources.configuration.screenWidthDp >= 700) 5 else if (resources.configuration.screenWidthDp >= 400) 3 else 2
        apps.chunked(columns).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { info ->
                val tile = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; background = cardBg(); setPadding(dp(5), dp(8), dp(5), dp(8)); isFocusable = true }
                val icon = try { info.loadIcon(packageManager) } catch (_: Exception) { null }
                tile.addView(ImageView(this).apply { if (icon != null) setImageDrawable(icon); scaleType = ImageView.ScaleType.FIT_CENTER }, LinearLayout.LayoutParams(dp(46), dp(46)))
                tile.addView(text(info.loadLabel(packageManager).toString(), 10f, Color.WHITE, true).apply { gravity = Gravity.CENTER; textAlignment = View.TEXT_ALIGNMENT_CENTER; maxLines = 2 })
                tile.setOnClickListener {
                    try { startActivity(packageManager.getLaunchIntentForPackage(info.activityInfo.packageName) ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(info.activityInfo.packageName)) }
                    catch (_: Exception) { Toast.makeText(this, "Не удалось открыть приложение", Toast.LENGTH_SHORT).show() }
                }
                tile.setOnFocusChangeListener { v, focused -> v.background = cardBg(focused); v.scaleX = if (focused) 1.05f else 1f; v.scaleY = if (focused) 1.05f else 1f }
                row.addView(tile, LinearLayout.LayoutParams(0, dp(98), 1f).apply { rightMargin = dp(5); bottomMargin = dp(7) })
            }
            if (group.size < columns) row.addView(View(this), LinearLayout.LayoutParams(0, dp(98), (columns-group.size).toFloat()))
            grid.addView(row)
        }
        return grid
    }

    private fun getInstalledLaunchableApps(): List<android.content.pm.ResolveInfo> {
        val phoneIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val tvIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return (packageManager.queryIntentActivities(phoneIntent, PackageManager.MATCH_ALL) +
            packageManager.queryIntentActivities(tvIntent, PackageManager.MATCH_ALL))
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
    }

    private fun buildInstalledAppsGrid(): LinearLayout {
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val selectedPackages = prefs.getStringSet("home_apps", emptySet())?.toSet() ?: emptySet()
        val apps = getInstalledLaunchableApps().filter { selectedPackages.contains(it.activityInfo.packageName) }
        if (apps.isEmpty()) {
            grid.addView(text("Здесь пока пусто. Через меню ⋯ выберите приложения, которые хотите видеть на главном экране.", 12f, 0xFFE5F5EA.toInt()))
        } else {
            grid.addView(buildAppsGrid(apps), LinearLayout.LayoutParams(-1, -2))
        }
        val manage = text("＋  ДОБАВИТЬ / УБРАТЬ ПЛИТКИ", 12f, 0xFFFFD98D.toInt(), true).apply {
            background = cardBg()
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            isFocusable = true
            setOnClickListener { showHomeTilesManager() }
            setOnFocusChangeListener { v, focused -> v.background = cardBg(focused) }
        }
        grid.addView(manage, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        return grid
    }

    private fun chooseCat() {
        val cats = arrayOf("Белый пушистый", "Серый пушистый", "Рыжий пушистый")
        AlertDialog.Builder(this).setTitle("Котёнок под деревом")
            .setSingleChoiceItems(cats, prefs.getInt("cat", 0)) { dialog, which ->
                prefs.edit().putInt("cat", which).apply(); dialog.dismiss(); buildScreen()
            }.setNegativeButton("Отмена", null).show()
    }

    private fun playAdhanNow() {
        try {
            val intent = Intent(this, AdhanPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
            Toast.makeText(this, "Запись азана запускается", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Не удалось запустить запись азана", Toast.LENGTH_LONG).show()
        }
    }

    private fun soundSettings() {
        val options = arrayOf("Включить азан по времени молитв", "Выключить автоматический азан", "Проверить азан сейчас")
        AlertDialog.Builder(this).setTitle("Звуки и азан")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { prefs.edit().putBoolean("adhan_enabled", true).apply(); PrayerAlarm.scheduleNext(this); Toast.makeText(this, "Автоматический азан включён", Toast.LENGTH_SHORT).show() }
                    1 -> { prefs.edit().putBoolean("adhan_enabled", false).apply(); PrayerAlarm.cancel(this); Toast.makeText(this, "Автоматический азан выключен", Toast.LENGTH_SHORT).show() }
                    2 -> playAdhanNow()
                }
            }.show()
    }

    private fun loadPrayerTimes() {
        if (::statusText.isInitialized) statusText.text = "Получаем расписание намазов для Грозного…"
        thread {
            try {
                val conn = URL("https://assolat.ru/api/namaz/today?city=grozny").openConnection() as HttpURLConnection
                conn.connectTimeout = 8000; conn.readTimeout = 8000; conn.requestMethod = "GET"
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val result = linkedMapOf<String, String>()
                prayerNames.keys.forEach { key -> if (json.has(key)) result[key] = json.getString(key).take(5) }
                runOnUiThread {
                    if (result.size == 5) {
                        prayerTimes = result
                        result.forEach { (key, value) -> prefs.edit().putString("prayer_$key", value).apply() }
                        if (::statusText.isInitialized) statusText.text = "Расписание • Грозный • UTC+3"
                        updateNextPrayer()
                        heroScene?.invalidate()
                        PrayerAlarm.scheduleNext(this)
                    } else if (::statusText.isInitialized) statusText.text = "Не удалось прочитать расписание. Проверьте подключение."
                }
            } catch (_: Exception) {
                runOnUiThread { if (::statusText.isInitialized) statusText.text = "Нет связи с расписанием. Повторите позже." }
            }
        }
    }

    private fun updateNextPrayer() {
        if (!::prayerText.isInitialized || prayerTimes.isEmpty()) return
        val now = timeFormat.format(Date())
        val next = prayerNames.keys.firstOrNull { (prayerTimes[it] ?: "99:99") > now }
        val line = if (next != null) "Следующий намаз: ${prayerNames[next]} • ${prayerTimes[next]}"
            else "На сегодня молитвы завершены"
        val maghrib = prayerTimes["maghrib"]
        val iftarLine = if (maghrib != null && now < maghrib) {
            try {
                val parts = maghrib.split(":")
                val target = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")).apply {
                    set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    set(Calendar.MINUTE, parts[1].take(2).toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val seconds = ((target.timeInMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                "До ифтара: %02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
            } catch (_: Exception) { "Время ифтара: Магриб • $maghrib" }
        } else if (maghrib != null) {
            "Ифтар • время Магриба наступило"
        } else ""
        val rows = prayerNames.keys.joinToString("     ") { "${prayerNames[it]}  ${prayerTimes[it] ?: "--:--"}" }
        prayerText.text = rows + "\n\n" + line + if (iftarLine.isNotEmpty()) "\n" + iftarLine else ""
    }

    private inner class RamadanSky(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stars = Array(42) { i ->
            val random = Random(9800L + i * 41L)
            floatArrayOf(random.nextFloat(), random.nextFloat() * 0.72f, 0.6f + random.nextFloat() * 1.4f)
        }
        private var phase = 0f
        private val animator = object : Runnable {
            override fun run() {
                phase += 0.025f
                invalidate()
                postDelayed(this, 90L)
            }
        }
        init { setLayerType(View.LAYER_TYPE_SOFTWARE, null) }
        override fun onAttachedToWindow() { super.onAttachedToWindow(); post(animator) }
        override fun onDetachedFromWindow() { removeCallbacks(animator); super.onDetachedFromWindow() }
        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            paint.shader = LinearGradient(
                0f, 0f, w, h,
                intArrayOf(0xFF041A17.toInt(), 0xFF0A3026.toInt(), 0xFF020D11.toInt()),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = null
            stars.forEachIndexed { index, star ->
                val shimmer = (0.50f + 0.40f * kotlin.math.sin(phase + index * 0.7f)).coerceIn(0.1f, 1f)
                paint.color = Color.argb((65f + shimmer * 95f).toInt(), 206, 230, 211)
                canvas.drawCircle(star[0] * w, star[1] * h, star[2], paint)
            }
            paint.color = 0x44020C0A
            canvas.drawOval(-w * 0.2f, h * 0.72f, w * 0.6f, h * 1.08f, paint)
            canvas.drawOval(w * 0.5f, h * 0.68f, w * 1.2f, h * 1.08f, paint)
        }
    }
}

object PrayerAlarm {
    private const val ID = 7511
    fun scheduleNext(context: Context) {
        if (!context.getSharedPreferences("ramadan", Context.MODE_PRIVATE).getBoolean("adhan_enabled", true)) return
        val prefs = context.getSharedPreferences("ramadan", Context.MODE_PRIVATE)
        val times = listOf("fajr","dhuhr","asr","maghrib","isha").mapNotNull { prefs.getString("prayer_$it", null) }
        val now = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"))
        val next = times.mapNotNull { value ->
            try {
                val parts = value.split(":")
                Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")).apply {
                    set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    set(Calendar.MINUTE, parts[1].take(2).toInt())
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
                }
            } catch (_: Exception) { null }
        }.minByOrNull { it.timeInMillis } ?: return
        val intent = PendingIntent.getBroadcast(context, ID, Intent(context, AdhanAlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 23) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, intent)
        else alarm.set(AlarmManager.RTC_WAKEUP, next.timeInMillis, intent)
    }
    fun cancel(context: Context) {
        val pi = PendingIntent.getBroadcast(context, ID, Intent(context, AdhanAlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
    }
}

class AdhanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (context.getSharedPreferences("ramadan", Context.MODE_PRIVATE).getBoolean("adhan_enabled", true))
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(Intent(context, AdhanPlaybackService::class.java))
            else context.startService(Intent(context, AdhanPlaybackService::class.java))
        PrayerAlarm.scheduleNext(context)
    }
}

class RamadanBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) PrayerAlarm.scheduleNext(context)
    }
}

class AdhanPlaybackService : Service() {
    private var player: MediaPlayer? = null
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val channelId = "ramadan_adhan"
            if (Build.VERSION.SDK_INT >= 26) {
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(NotificationChannel(channelId, "Азан", NotificationManager.IMPORTANCE_LOW))
            }
            val notification = if (Build.VERSION.SDK_INT >= 26)
                Notification.Builder(this, channelId).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Время молитвы").setContentText("Звучит азан").build()
            else Notification.Builder(this).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Время молитвы").setContentText("Звучит азан").build()
            startForeground(7512, notification)
            player?.release()
            val id = resources.getIdentifier("adhan", "raw", packageName)
            if (id != 0) {
                val created = MediaPlayer.create(this, id)
                if (created != null) {
                    player = created
                    created.setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    created.setOnCompletionListener { it.release(); player = null; stopSelf() }
                    created.start()
                } else stopSelf()
            } else stopSelf()
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() { player?.release(); player = null; if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else stopForeground(true); super.onDestroy() }
}
