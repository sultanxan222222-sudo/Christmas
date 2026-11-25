package com.christmasdream.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : Activity() {
    private val gold = Color.rgb(255, 205, 92)
    private val cream = Color.rgb(255, 246, 224)
    private val sections = listOf("Главная", "Фильмы", "ТВ", "Игры", "Приложения", "Настройки")
    private var selected = "Главная"
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 35, 31)
        window.navigationBarColor = Color.rgb(12, 35, 31)
        buildUi()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun panel(color: Int, radius: Float = 22f): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius.toInt()).toFloat(); setStroke(dp(1), 0x55FFFFFF) }

    private fun text(value: String, size: Float, color: Int = cream, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun buildUi() {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(12)); background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.rgb(7,31,30), Color.rgb(18,48,43), Color.rgb(48,20,29))) }
        setContentView(SnowView())
        val shell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(12), dp(18), dp(12)) }
        shell.background = root.background
        setContentView(shell)
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        brand.addView(text("✦ CHRISTMAS DREAM", 21f, gold, true))
        brand.addView(text("ТВОЙ ПРАЗДНИЧНЫЙ ЭКРАН", 10f, 0xFFBFD5C8.toInt(), true))
        header.addView(brand)
        val now = SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(Date())
        header.addView(text("❄  $now   ✨", 14f, cream, true))
        shell.addView(header)
        val divider = View(this).apply { setBackgroundColor(0x66D7B66A) }
        shell.addView(divider, LinearLayout.LayoutParams(-1, dp(1)).apply { topMargin = dp(10); bottomMargin = dp(10) })
        nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val navScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(nav) }
        shell.addView(navScroll)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, 0) }
        val scroller = ScrollView(this).apply { isFillViewport = true; addView(content) }
        shell.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        render()
    }

    private fun render() {
        nav.removeAllViews()
        sections.forEach { section ->
            val b = text((if (selected == section) "❄  " else "") + section, 14f, if (selected == section) gold else cream, selected == section)
            b.background = panel((if (selected == section) 0x553D8B69L else 0x332FFFFFFL).toInt(), 18f)
            b.setPadding(dp(14), dp(10), dp(14), dp(10))
            b.isFocusable = true
            b.setOnClickListener { selected = section; render() }
            nav.addView(b, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(7) })
        }
        content.removeAllViews()
        when (selected) {
            "Главная" -> home()
            "Настройки" -> settings()
            else -> appSection(selected)
        }
    }

    private fun home() {
        val hero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; background = panel(0xAA173F35.toInt(), 28f); setPadding(dp(18), dp(18), dp(18), dp(18)) }
        hero.addView(text("🎄", 48f, gold, true).apply { gravity = Gravity.CENTER })
        hero.addView(text("С РОЖДЕСТВОМ И НОВЫМ ГОДОМ!", 22f, gold, true).apply { gravity = Gravity.CENTER })
        hero.addView(text("Тепло • уют • любимые приложения", 14f).apply { gravity = Gravity.CENTER })
        content.addView(hero, LinearLayout.LayoutParams(-1, dp(180)).apply { bottomMargin = dp(12) })
        content.addView(text("БЫСТРЫЙ ДОСТУП", 13f, gold, true))
        appSection("Приложения", true)
    }

    private fun appSection(section: String, compact: Boolean = false) {
        val title = if (compact) "" else section.uppercase(Locale.getDefault())
        if (title.isNotEmpty()) content.addView(text(title, 18f, gold, true).apply { setPadding(0, dp(6), 0, dp(10)) })
        val apps = installedApps().filter { app ->
            val name = app.loadLabel(packageManager).toString().lowercase(Locale.getDefault())
            when (section) {
                "Фильмы" -> listOf("youtube","netflix","кино","ivi","окко","кинопоиск","премьер","видео","медиа").any { name.contains(it) }
                "ТВ" -> listOf("tv","телев","iptv","эфир","канал").any { name.contains(it) }
                "Игры" -> try { packageManager.getApplicationInfo(app.activityInfo.packageName, 0).category == android.content.pm.ApplicationInfo.CATEGORY_GAME } catch (_: Exception) { false }
                else -> true
            }
        }
        if (apps.isEmpty()) {
            val empty = text("Пока здесь нет подходящих установленных приложений.\nОткрой раздел «Приложения» или установи нужное приложение обычным способом.", 15f)
            empty.background = panel(0x55333333, 18f); empty.setPadding(dp(16), dp(18), dp(16), dp(18))
            content.addView(empty)
            return
        }
        apps.take(40).chunked(5).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { info ->
                val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; background = panel(0x883B3028.toInt(), 20f); setPadding(dp(8), dp(12), dp(8), dp(12)); isFocusable = true; isClickable = true }
                val label = info.loadLabel(packageManager).toString()
                val icon = try { info.loadIcon(packageManager) } catch (_: Exception) { null }
                val image = android.widget.ImageView(this).apply { if (icon != null) setImageDrawable(icon); scaleType = android.widget.ImageView.ScaleType.FIT_CENTER }
                card.addView(image, LinearLayout.LayoutParams(dp(48), dp(48)))
                card.addView(text(label, 12f, cream, true).apply { gravity = Gravity.CENTER; maxLines = 2; textAlignment = View.TEXT_ALIGNMENT_CENTER })
                card.setOnClickListener { launch(info.activityInfo.packageName) }
                card.setOnFocusChangeListener { v, focus -> v.background = panel(if (focus) 0xFF8B6335.toInt() else 0x883B3028.toInt(), 20f); v.scaleX = if (focus) 1.05f else 1f; v.scaleY = if (focus) 1.05f else 1f }
                row.addView(card, LinearLayout.LayoutParams(0, dp(112), 1f).apply { rightMargin = dp(7); bottomMargin = dp(9) })
            }
            content.addView(row)
        }
    }

    private fun installedApps(): List<android.content.pm.ResolveInfo> {
        // Android TV apps often expose LEANBACK_LAUNCHER instead of LAUNCHER.
        val phoneIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val tvIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val combined = packageManager.queryIntentActivities(phoneIntent, PackageManager.MATCH_ALL) +
            packageManager.queryIntentActivities(tvIntent, PackageManager.MATCH_ALL)
        return combined
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
    }

    private fun launch(pkg: String) {
        try { packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) } }
        catch (_: Exception) { }
    }

    private fun settings() {
        val note = text("Настройки лаунчера", 22f, gold, true)
        content.addView(note)
        val info = text("• Снежная анимация включена\n• Управление пультом: стрелки, OK и Back\n• Сенсорное управление поддерживается\n• Системные приложения и прошивка не изменяются", 16f)
        info.background = panel(0x66333333, 20f); info.setPadding(dp(16), dp(18), dp(16), dp(18))
        content.addView(info)
        addAction("Вернуться к штатному экрану TCL") { returnToStock() }
        addAction("Обновить список приложений") { render() }
    }

    private fun addAction(label: String, action: () -> Unit) {
        val v = text(label, 16f, gold, true); v.background = panel(0x553D8B69, 18f); v.setPadding(dp(16), dp(14), dp(16), dp(14)); v.isFocusable = true; v.setOnClickListener { action() }
        content.addView(v, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
    }

    private fun returnToStock() {
        AlertDialog.Builder(this).setTitle("Штатный экран").setMessage("Android откроет системный выбор домашнего экрана. Выберите штатный TCL Launcher. Само приложение TCL не отключается и не изменяется.")
            .setNegativeButton("Отмена", null).setPositiveButton("Продолжить") { _, _ ->
                try { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                catch (_: Exception) { }
            }.show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && selected != "Главная") { selected = "Главная"; render(); return true }
        return super.onKeyDown(keyCode, event)
    }

    private inner class SnowView : View(this@MainActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val flakes = List(55) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), 1f + Random.nextFloat() * 3f) }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.rgb(8, 34, 32))
            paint.color = 0x99FFFFFF.toInt()
            flakes.forEach { f -> canvas.drawCircle(f[0] * width, f[1] * height, f[2], paint) }
        }
    }
}
