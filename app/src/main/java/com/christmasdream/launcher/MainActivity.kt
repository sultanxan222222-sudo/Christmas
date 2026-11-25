package com.christmasdream.launcher

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = deepRed
        window.navigationBarColor = deepRed
        buildUi()
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
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(10), dp(16), dp(12))
            background = gradient(intArrayOf(Color.rgb(113, 8, 31), Color.rgb(177, 24, 47), Color.rgb(91, 7, 27)), 0)
        }
        frame.addView(shell, FrameLayout.LayoutParams(-1, -1))

        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        brand.addView(label("✦ CHRISTMAS DREAM ✦", 20f, gold, true))
        brand.addView(label("РОЖДЕСТВО НАЧИНАЕТСЯ ДОМА", 9f, ivory, true))
        header.addView(brand)
        val date = SimpleDateFormat("d MMMM", Locale("ru")).format(Date())
        header.addView(label("❄  $date  ❄", 13f, white, true))
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
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = gradient(intArrayOf(Color.rgb(255, 250, 241), Color.rgb(255, 231, 213)), 28)
            setPadding(dp(18), dp(14), dp(18), dp(16))
        }
        hero.addView(label("🎄  ✨  🎄", 33f, red, true).apply { gravity = Gravity.CENTER })
        hero.addView(label("С РОЖДЕСТВОМ!", 25f, red, true).apply { gravity = Gravity.CENTER })
        hero.addView(label("Пусть дома будет тепло, свет и волшебство", 12f, wine, true).apply { gravity = Gravity.CENTER })
        hero.addView(label("❄     ✦     ❄     ✦     ❄", 15f, red, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(5), 0, 0) })
        content.addView(hero, LinearLayout.LayoutParams(-1, dp(170)).apply { bottomMargin = dp(12) })

        heading("ПРАЗДНИЧНОЕ МЕНЮ")
        val menu = listOf(
            Triple("Фильмы", "🎬", "Вечер любимого кино"),
            Triple("ТВ", "📺", "Телевидение и каналы"),
            Triple("Игры", "🎮", "Игры для всей семьи"),
            Triple("Приложения", "📱", "Все установленные приложения"),
            Triple("Настройки", "⚙️", "Оформление и возврат")
        )
        val columns = if (resources.configuration.screenWidthDp >= 600) 3 else 2
        menu.chunked(columns).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { item ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = shape(white, 20, 0xFFFFDCA7.toInt())
                    setPadding(dp(8), dp(12), dp(8), dp(12))
                    isFocusable = true
                    isClickable = true
                }
                card.addView(label(item.second, 29f, red, true).apply { gravity = Gravity.CENTER })
                card.addView(label(item.first, 15f, wine, true).apply { gravity = Gravity.CENTER })
                card.addView(label(item.third, 10f, Color.rgb(117, 71, 67)).apply { gravity = Gravity.CENTER; maxLines = 2; textAlignment = View.TEXT_ALIGNMENT_CENTER })
                card.setOnClickListener { selected = item.first; render() }
                card.setOnFocusChangeListener { v, focused ->
                    v.background = shape(if (focused) Color.rgb(255, 224, 190) else white, 20, if (focused) red else 0xFFFFDCA7.toInt())
                    v.scaleX = if (focused) 1.035f else 1f
                    v.scaleY = if (focused) 1.035f else 1f
                }
                row.addView(card, LinearLayout.LayoutParams(0, dp(112), 1f).apply {
                    rightMargin = dp(6); leftMargin = dp(3); bottomMargin = dp(8)
                })
            }
            if (group.size < columns) {
                row.addView(View(this), LinearLayout.LayoutParams(0, dp(112), (columns - group.size).toFloat()))
            }
            content.addView(row)
        }

        heading("ВАШИ ПРИЛОЖЕНИЯ")
        appGrid(installedApps().take(8))
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
                    background = shape(ivory, 18, 0x55FFFFFF)
                    setPadding(dp(5), dp(9), dp(5), dp(9))
                    isFocusable = true
                    isClickable = true
                }
                val icon = try { info.loadIcon(packageManager) } catch (_: Exception) { null }
                card.addView(ImageView(this).apply {
                    if (icon != null) setImageDrawable(icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }, LinearLayout.LayoutParams(dp(42), dp(42)))
                card.addView(label(info.loadLabel(packageManager).toString(), 11f, wine, true).apply {
                    gravity = Gravity.CENTER
                    maxLines = 2
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                })
                card.setOnClickListener { launch(info.activityInfo.packageName) }
                card.setOnFocusChangeListener { v, focused ->
                    v.background = shape(if (focused) Color.rgb(255, 225, 193) else ivory, 18, if (focused) red else 0x55FFFFFF)
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
        info.background = shape(ivory, 22, 0xFFFFDCA7.toInt())
        info.setPadding(dp(18), dp(18), dp(18), dp(18))
        content.addView(info)
        actionCard("↻   Обновить список приложений") { render() }
        actionCard("⌂   Вернуться к штатному экрану TCL") { returnToStock() }
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
