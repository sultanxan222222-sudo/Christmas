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
        val wide = resources.configuration.screenWidthDp >= 600
        val hero = FrameLayout(this).apply {
            background = shape(Color.rgb(45, 12, 24), 26, 0x88E4BD79.toInt())
            clipToOutline = true
            elevation = dp(8).toFloat()
        }
        hero.addView(ChristmasSceneView(), FrameLayout.LayoutParams(-1, -1))
        val copy = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20), dp(14), dp(8), dp(14))
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
                    background = gradient(
                        if (index % 2 == 0) intArrayOf(Color.rgb(116, 20, 39), Color.rgb(66, 9, 27))
                        else intArrayOf(Color.rgb(88, 19, 37), Color.rgb(48, 8, 25)), 20
                    )
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
                    v.background = if (focused) shape(Color.rgb(126, 33, 48), 20, gold)
                    else gradient(intArrayOf(Color.rgb(102, 18, 38), Color.rgb(48, 8, 25)), 20)
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

    private inner class ChristmasSceneView : View(this@MainActivity) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private fun fill(c: Int) { p.color = c; p.style = Paint.Style.FILL; p.shader = null }
        private fun box(c: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) {
            fill(color); c.drawRoundRect(l, t, r, b, radius, radius, p)
        }
        private fun dot(c: Canvas, x: Float, y: Float, radius: Float, color: Int) {
            fill(color); c.drawCircle(x, y, radius, p)
        }
        private fun branch(c: Canvas, cx: Float, top: Float, width: Float, height: Float, tier: Int, color: Int) {
            val y0 = top + height * tier / 3f
            val y1 = top + height * (tier + 1) / 3f
            val half = width * (tier + 1) / 6f
            val path = Path().apply {
                moveTo(cx, y0 - height * 0.07f)
                cubicTo(cx - half * .5f, y0 + height * .02f, cx - half * .85f, y1 - height * .05f, cx - half, y1)
                quadTo(cx, y1 + height * .08f, cx + half, y1)
                cubicTo(cx + half * .85f, y1 - height * .05f, cx + half * .5f, y0 + height * .02f, cx, y0 - height * .07f)
                close()
            }
            fill(color); c.drawPath(path, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f; p.color = 0x5577D69A
            c.drawPath(path, p); p.style = Paint.Style.FILL
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat(); val h = height.toFloat()
            if (w < 1f || h < 1f) return
            p.shader = android.graphics.LinearGradient(0f, 0f, w, h,
                intArrayOf(Color.rgb(25, 10, 22), Color.rgb(88, 22, 39), Color.rgb(30, 8, 19)),
                null, android.graphics.Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, p); p.shader = null

            // Snowy window behind the tree
            val left = w * .55f
            box(canvas, left, h * .09f, w * .96f, h * .76f, 8f, 0xFF33202A.toInt())
            box(canvas, left + 5f, h * .09f + 5f, w * .96f - 5f, h * .76f - 5f, 5f, 0xFF263747.toInt())
            fill(0xFF78838D.toInt())
            canvas.drawRect(left + (w * .96f - left) * .49f, h * .11f, left + (w * .96f - left) * .51f, h * .74f, p)
            canvas.drawRect(left + 5f, h * .42f, w * .96f - 5f, h * .44f, p)
            for (i in 0..22) {
                val x = left + Random(i * 17 + 5).nextFloat() * (w * .38f)
                val y = h * .13f + Random(i * 29 + 2).nextFloat() * (h * .58f)
                dot(canvas, x, y, 1f + (i % 3), 0xCCFFFFFF.toInt())
            }

            // Fireplace and amber light
            box(canvas, w * .79f, h * .65f, w * .98f, h * .93f, 7f, 0xFF421C27.toInt())
            box(canvas, w * .81f, h * .69f, w * .96f, h * .90f, 4f, 0xFF160D14.toInt())
            dot(canvas, w * .885f, h * .83f, h * .075f, 0x88F07836.toInt())
            dot(canvas, w * .885f, h * .83f, h * .045f, 0xFFFFC35C.toInt())

            // A full layered evergreen tree, not a glyph
            val cx = w * .77f
            val top = h * .075f
            val tw = minOf(w * .50f, h * .90f)
            val th = h * .80f
            box(canvas, cx - tw * .055f, top + th * .76f, cx + tw * .055f, top + th * .99f, 4f, 0xFF70402B.toInt())
            branch(canvas, cx, top, tw, th, 0, 0xFF0B4934.toInt())
            branch(canvas, cx, top, tw, th, 1, 0xFF0D5A3D.toInt())
            branch(canvas, cx, top, tw, th, 2, 0xFF0A4933.toInt())

            // Gold star
            dot(canvas, cx, top, 14f, 0x55FFD77A)
            val star = Path()
            for (i in 0 until 10) {
                val a = Math.PI * i / 5.0 - Math.PI / 2.0
                val r = if (i % 2 == 0) 12f else 5f
                val x = cx + kotlin.math.cos(a).toFloat() * r
                val y = top + kotlin.math.sin(a).toFloat() * r
                if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
            }
            star.close(); fill(0xFFFFD778.toInt()); canvas.drawPath(star, p)

            // Draped garlands and warm bulbs
            for (row in 0..3) {
                val yy = top + th * (.22f + row * .17f)
                val span = tw * (.13f + row * .105f)
                val curve = Path().apply { moveTo(cx - span, yy); quadTo(cx, yy + th * .12f, cx + span, yy) }
                p.color = 0xFFD8B16C.toInt(); p.style = Paint.Style.STROKE; p.strokeWidth = 2.2f
                canvas.drawPath(curve, p); p.style = Paint.Style.FILL
                for (j in 0..7) {
                    val t = j / 7f
                    val x = cx - span + 2f * span * t
                    val y = yy + th * .12f * (1f - kotlin.math.abs(2f * t - 1f))
                    dot(canvas, x, y, 5.5f, 0x33FFD477)
                    dot(canvas, x, y, 2.8f, if ((j + row) % 2 == 0) 0xFFFFD477.toInt() else 0xFFFFF0C4.toInt())
                }
            }

            // Glass baubles with tiny highlights
            val ornaments = listOf(
                Pair(-.20f, .32f), Pair(.16f, .38f), Pair(-.29f, .55f), Pair(.28f, .58f),
                Pair(-.12f, .72f), Pair(.13f, .80f), Pair(0f, .48f), Pair(.35f, .73f)
            )
            ornaments.forEachIndexed { i, o ->
                val x = cx + tw * o.first; val y = top + th * o.second
                dot(canvas, x + 1.5f, y + 2f, 7f, 0x66000000)
                dot(canvas, x, y, 6.2f, when (i % 3) {
                    0 -> 0xFFC63B43.toInt()
                    1 -> 0xFFFFD27B.toInt()
                    else -> 0xFFECE2CF.toInt()
                })
                dot(canvas, x - 2f, y - 2f, 1.8f, 0xCCFFFFFF.toInt())
            }

            // Wrapped presents at the foot of the tree
            box(canvas, cx - tw * .46f, h * .82f, cx - tw * .13f, h * .96f, 4f, 0xFF9E243A.toInt())
            box(canvas, cx - tw * .31f, h * .82f, cx - tw * .28f, h * .96f, 2f, 0xFFFFD47E.toInt())
            box(canvas, cx + tw * .13f, h * .85f, cx + tw * .45f, h * .97f, 4f, 0xFFE7C18A.toInt())
            box(canvas, cx + tw * .27f, h * .85f, cx + tw * .30f, h * .97f, 2f, 0xFF9E243A.toInt())
            fill(0x44F0B46D); canvas.drawOval(w * .38f, h * .91f, w * .99f, h * 1.04f, p)

            p.style = Paint.Style.STROKE; p.strokeWidth = 1.2f; p.color = 0x99E7C27E.toInt()
            canvas.drawRoundRect(1f, 1f, w - 1f, h - 1f, 25f, 25f, p)
            p.style = Paint.Style.FILL
        }
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
