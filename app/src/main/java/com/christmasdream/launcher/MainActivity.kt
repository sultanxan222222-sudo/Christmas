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
    private val prayerNames = linkedMapOf("fajr" to "Фаджр", "dhuhr" to "Зухр", "asr" to "Аср", "maghrib" to "Магриб", "isha" to "Иша")
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("ru"))
    private val updateClock = object : Runnable {
        override fun run() {
            if (::clockText.isInitialized) clockText.text = when (prefs.getInt("clock", 2)) {
                0 -> SimpleDateFormat("HH:mm:ss", Locale("ar")).format(Date())
                1 -> "✧  " + SimpleDateFormat("HH:mm", Locale("ru")).format(Date()) + "  ✧"
                2 -> SimpleDateFormat("HH:mm:ss", Locale("ru")).format(Date())
                else -> "۞  " + SimpleDateFormat("HH:mm", Locale("ru")).format(Date()) + "  ۞"
            }
            if (::prayerText.isInitialized) updateNextPrayer()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(3, 24, 19)
        window.navigationBarColor = Color.rgb(3, 24, 19)
        buildScreen()
        handler.post(updateClock)
        loadPrayerTimes()
        PrayerAlarm.scheduleNext(this)
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateClock)
        super.onDestroy()
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
        title.addView(text("РАМАДАН • ИЗУМРУДНАЯ НОЧЬ", 11f, 0xFFD8F5E5.toInt(), true))
        header.addView(title)
        val date = SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date())
        header.addView(text("Грозный  •  $date", 12f, 0xFFE6F7ED.toInt(), true))
        page.addView(header)
        page.addView(text("الله", 68f, 0xFF72FFAA.toInt(), true).apply {
            gravity = Gravity.CENTER
            setShadowLayer(dp(20).toFloat(), 0f, 0f, 0xFF00FF78.toInt())
            setPadding(0, dp(4), 0, dp(2))
        })
        page.addView(text("МИР ВАШЕМУ ДОМУ", 10f, 0xFFE3D5A9.toInt(), true).apply {
            gravity = Gravity.CENTER
            letterSpacing = 0.18f
            setPadding(0, 0, 0, dp(12))
        })
        val timePanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = cardBg(); setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        clockText = text("00:00:00", 34f, 0xFFFFE6B1.toInt(), true).apply { gravity = Gravity.CENTER }
        timePanel.addView(clockText)
        timePanel.addView(text("ЧЕТЫРЕ СТИЛЯ ЧАСОВ — ВЫБЕРИТЕ СВОИ", 9f, 0xFFBDEDD1.toInt(), true).apply { gravity = Gravity.CENTER })
        page.addView(timePanel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        val clockRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val clockStyles = listOf("☾  Классика", "✧  Золото", "◷  Цифровые", "۞  Орнамент")
        clockStyles.forEachIndexed { index, name ->
            val selected = prefs.getInt("clock", 2) == index
            val v = text(name, 10f, if (selected) 0xFFFFD98D.toInt() else Color.WHITE, true)
            v.gravity = Gravity.CENTER; v.background = cardBg(selected); v.setPadding(dp(6), dp(12), dp(6), dp(12))
            v.isFocusable = true; v.setOnClickListener {
                prefs.edit().putInt("clock", index).apply()
                Toast.makeText(this, "Стиль часов выбран", Toast.LENGTH_SHORT).show()
                buildScreen()
            }
            v.setOnFocusChangeListener { view, focus -> view.background = cardBg(focus || selected) }
            clockRow.addView(v, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(4) })
        }
        page.addView(clockRow)
        page.addView(text("ВРЕМЕНА НАМАЗОВ • ГРОЗНЫЙ", 14f, 0xFFFFD88A.toInt(), true).apply { setPadding(0, dp(16), 0, dp(6)) })
        prayerText = text("Загружаем расписание молитв…", 13f, 0xFFE5F5EA.toInt())
        prayerText.background = cardBg(); prayerText.setPadding(dp(14), dp(12), dp(14), dp(12))
        page.addView(prayerText)
        statusText = text("Источник расписания: ДУМ ЧР • UTC+3", 10f, 0xFFB8D7C6.toInt())
        page.addView(statusText)
        page.addView(text("ВАШЕ МЕНЮ", 14f, 0xFFFFD88A.toInt(), true).apply { setPadding(0, dp(16), 0, dp(6)) })
        val menu = listOf("🐈  Выбрать котёнка" to { chooseCat() }, "🕌  Азан и звуки" to { soundSettings() },
            "🌙  Календарь хиджры" to { Toast.makeText(this, "Календарь добавим в следующую сборку", Toast.LENGTH_SHORT).show() },
            "⚙  Настройки" to { soundSettings() })
        menu.forEach { (name, action) ->
            val item = text(name, 15f, Color.WHITE, true)
            item.background = cardBg(); item.setPadding(dp(16), dp(13), dp(16), dp(13))
            item.isFocusable = true; item.setOnClickListener { action() }
            item.setOnFocusChangeListener { view, focus -> view.background = cardBg(focus) }
            page.addView(item, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        }

        page.addView(text("МОИ ПРИЛОЖЕНИЯ", 14f, 0xFFFFD88A.toInt(), true).apply {
            setPadding(0, dp(18), 0, dp(6))
        })
        page.addView(text("Все приложения телефона или телевизора — нажмите на значок, чтобы открыть.", 10f, 0xFFBDEDD1.toInt()))
        val refreshApps = text("↻  Обновить список приложений", 12f, 0xFFFFD98D.toInt(), true).apply {
            background = cardBg(); setPadding(dp(12), dp(10), dp(12), dp(10))
            isFocusable = true
            setOnClickListener { buildScreen() }
            setOnFocusChangeListener { v, focused -> v.background = cardBg(focused) }
        }
        page.addView(refreshApps, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
        page.addView(buildInstalledAppsGrid(), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        page.addView(text("Нашид Рамадана • мягкая фоновая музыка", 11f, 0xFFE6D9B3.toInt()).apply { setPadding(dp(4), dp(12), 0, 0) })
        setContentView(root)
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
        val apps = getInstalledLaunchableApps()
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (apps.isEmpty()) {
            grid.addView(text("Не удалось получить список приложений. Нажмите «Обновить список приложений».", 12f, 0xFFE5F5EA.toInt()))
            return grid
        }
        val wide = resources.configuration.screenWidthDp >= 600
        val columns = when {
            resources.configuration.screenWidthDp >= 1000 -> 6
            resources.configuration.screenWidthDp >= 700 -> 5
            resources.configuration.screenWidthDp >= 500 -> 4
            resources.configuration.screenWidthDp >= 380 -> 3
            else -> 2
        }
        apps.chunked(columns).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { info ->
                val appPackage = info.activityInfo.packageName
                val tile = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = cardBg()
                    setPadding(dp(5), dp(8), dp(5), dp(8))
                    isFocusable = true
                    isClickable = true
                    contentDescription = "Открыть " + info.loadLabel(packageManager)
                }
                val icon = try { info.loadIcon(packageManager) } catch (_: Exception) { null }
                tile.addView(ImageView(this).apply {
                    if (icon != null) setImageDrawable(icon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = null
                }, LinearLayout.LayoutParams(dp(if (wide) 54 else 42), dp(if (wide) 54 else 42)))
                tile.addView(text(info.loadLabel(packageManager).toString(), if (wide) 11f else 10f, Color.WHITE, true).apply {
                    gravity = Gravity.CENTER
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    maxLines = 2
                    setPadding(0, dp(5), 0, 0)
                }, LinearLayout.LayoutParams(-1, -2))
                tile.setOnClickListener {
                    try {
                        val launchIntent = packageManager.getLaunchIntentForPackage(appPackage)
                            ?: Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(appPackage)
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                    } catch (_: Exception) {
                        Toast.makeText(this, "Не удалось открыть приложение", Toast.LENGTH_SHORT).show()
                    }
                }
                tile.setOnFocusChangeListener { v, focused ->
                    v.background = cardBg(focused)
                    v.scaleX = if (focused) 1.05f else 1f
                    v.scaleY = if (focused) 1.05f else 1f
                }
                row.addView(tile, LinearLayout.LayoutParams(0, dp(if (wide) 112 else 94), 1f).apply {
                    rightMargin = dp(5)
                    bottomMargin = dp(7)
                })
            }
            if (group.size < columns) {
                row.addView(View(this), LinearLayout.LayoutParams(0, dp(if (wide) 112 else 94), (columns - group.size).toFloat()))
            }
            grid.addView(row)
        }
        return grid
    }

    private fun chooseCat() {
        val cats = arrayOf("Белый пушистый", "Серый пушистый", "Рыжий пушистый")
        AlertDialog.Builder(this).setTitle("Котёнок на полумесяце")
            .setSingleChoiceItems(cats, prefs.getInt("cat", 0)) { dialog, which ->
                prefs.edit().putInt("cat", which).apply(); dialog.dismiss(); buildScreen()
            }.setNegativeButton("Отмена", null).show()
    }

    private fun soundSettings() {
        val options = arrayOf("Включить азан по времени молитв", "Выключить автоматический азан", "Проверить азан сейчас")
        AlertDialog.Builder(this).setTitle("Звуки и азан")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { prefs.edit().putBoolean("adhan_enabled", true).apply(); PrayerAlarm.scheduleNext(this); Toast.makeText(this, "Автоматический азан включён", Toast.LENGTH_SHORT).show() }
                    1 -> { prefs.edit().putBoolean("adhan_enabled", false).apply(); PrayerAlarm.cancel(this); Toast.makeText(this, "Автоматический азан выключен", Toast.LENGTH_SHORT).show() }
                    2 -> startService(Intent(this, AdhanPlaybackService::class.java))
                }
            }.show()
    }

    private fun loadPrayerTimes() {
        statusText.text = "Получаем официальное расписание ДУМ ЧР…"
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
                        statusText.text = "Расписание ДУМ ЧР • Грозный • UTC+3"
                        updateNextPrayer()
                        PrayerAlarm.scheduleNext(this)
                    } else statusText.text = "Не удалось прочитать расписание. Проверьте подключение."
                }
            } catch (_: Exception) {
                runOnUiThread { statusText.text = "Нет связи с расписанием. Повторите позже." }
            }
        }
    }

    private fun updateNextPrayer() {
        if (prayerTimes.isEmpty()) return
        val now = timeFormat.format(Date())
        val next = prayerNames.keys.firstOrNull { (prayerTimes[it] ?: "99:99") > now }
        val line = if (next != null) "Следующий намаз: ${prayerNames[next]} • ${prayerTimes[next]}"
            else "На сегодня молитвы завершены"
        prayerText.text = prayerNames.keys.joinToString("     ") { "${prayerNames[it]}  ${prayerTimes[it] ?: "--:--"}" } + "\n\n" + line
    }

    private inner class RamadanSky(context: Context) : View(context) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private val snow = List(70) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), 1f + Random.nextFloat() * 3f, 0.001f + Random.nextFloat() * 0.003f) }
        private var animationPhase = 0f
        private val anim = object : Runnable {
            override fun run() {
                animationPhase += 0.055f
                snow.forEach { it[1] += it[3]; it[0] += 0.00015f; if (it[1] > 1f) { it[1] = 0f; it[0] = Random.nextFloat() } }
                invalidate(); postDelayed(this, 40)
            }
        }
        init { setLayerType(View.LAYER_TYPE_SOFTWARE, null) }
        override fun onAttachedToWindow() { super.onAttachedToWindow(); post(anim) }
        override fun onDetachedFromWindow() { removeCallbacks(anim); super.onDetachedFromWindow() }
        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            p.shader = LinearGradient(0f, 0f, w, h, intArrayOf(0xFF061C19.toInt(), 0xFF092C24.toInt(), 0xFF020D12.toInt()), null, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w, h, p); p.shader = null
            // Distant hills and mosque silhouette
            p.color = 0xFF061B18.toInt()
            c.drawOval(-w*.2f, h*.58f, w*.55f, h*.95f, p); c.drawOval(w*.42f, h*.55f, w*1.2f, h*.98f, p)
            p.color = 0xFF04110F.toInt()
            c.drawRect(w*.35f, h*.58f, w*.68f, h*.82f, p)
            c.drawRect(w*.47f, h*.48f, w*.53f, h*.58f, p)
            c.drawCircle(w*.5f, h*.48f, w*.035f, p)
            // Minaret with warm lantern glow
            p.color = 0xFF0A211C.toInt(); c.drawRect(w*.72f, h*.38f, w*.755f, h*.79f, p)
            c.drawCircle(w*.737f, h*.36f, w*.026f, p)
            p.color = 0xFFFFD887.toInt(); p.setShadowLayer(22f, 0f, 0f, 0xFFFFC45B.toInt())
            c.drawCircle(w*.737f, h*.49f, w*.012f, p); p.clearShadowLayer()
            // Crescent and stylized kitten
            val cx = w*.87f; val cy = h*.20f; val r = w*.085f
            c.save()
            c.rotate(2.2f * kotlin.math.sin(animationPhase * 0.72f), cx, cy)
            p.color = 0xFFFFD77E.toInt(); p.setShadowLayer(24f, 0f, 0f, 0xFFFFC55A.toInt())
            c.drawCircle(cx, cy, r, p); p.clearShadowLayer()
            p.color = 0xFF071C19.toInt(); c.drawCircle(cx+r*.38f, cy-r*.22f, r*.82f, p)
            drawKitten(c, cx-r*.25f, cy-r*.45f, r*.48f)
            c.restore()
            // Snow
            p.color = 0xCCFFFFFF.toInt()
            snow.forEach { c.drawCircle(it[0]*w, it[1]*h, it[2], p) }
        }
        private fun drawKitten(c: Canvas, x: Float, y: Float, s: Float) {
            val cat = prefs.getInt("cat", 0)
            val fur = when(cat) { 1 -> 0xFF9BA5AA.toInt(); 2 -> 0xFFE7A05A.toInt(); else -> 0xFFF1EEE8.toInt() }
            val sway = kotlin.math.sin(animationPhase * 1.35f)
            val blink = kotlin.math.sin(animationPhase * 0.43f) > 0.985f
            p.color = fur
            p.strokeWidth = s * 0.16f
            p.strokeCap = Paint.Cap.ROUND
            p.style = Paint.Style.STROKE
            val tail = Path().apply {
                moveTo(x+s*.28f, y+s*.91f)
                cubicTo(x+s*(.95f+sway*.12f), y+s*1.05f, x+s*(.92f+sway*.18f), y+s*.22f, x+s*(1.12f+sway*.2f), y+s*.43f)
            }
            c.drawPath(tail,p)
            p.style = Paint.Style.FILL
            p.setShadowLayer(8f, 0f, 0f, 0x8899FFCC.toInt())
            c.drawOval(x-s*.43f, y+s*.28f, x+s*.48f, y+s*1.32f, p)
            c.drawOval(x-s*.39f, y+s*1.05f, x-s*.08f, y+s*1.42f, p)
            c.drawOval(x-s*.08f, y+s*1.07f, x+s*.19f, y+s*1.43f, p)
            c.drawOval(x+s*.12f, y+s*1.02f, x+s*.43f, y+s*1.38f, p)
            c.drawOval(x-s*.28f, y+s*.88f, x-s*.02f, y+s*1.28f, p)
            c.drawCircle(x, y, s*.55f, p)
            val ear = Path().apply { moveTo(x-s*.42f,y-s*.22f); lineTo(x-s*.50f,y-s*.8f); lineTo(x-s*.04f,y-s*.47f); close() }
            c.drawPath(ear,p)
            val ear2 = Path().apply { moveTo(x+s*.18f,y-s*.44f); lineTo(x+s*.53f,y-s*.77f); lineTo(x+s*.49f,y-s*.12f); close() }
            c.drawPath(ear2,p)
            p.clearShadowLayer()
            p.color = 0xFFE9A7A5.toInt()
            val inner1 = Path().apply { moveTo(x-s*.39f,y-s*.31f); lineTo(x-s*.44f,y-s*.68f); lineTo(x-s*.13f,y-s*.46f); close() }
            val inner2 = Path().apply { moveTo(x+s*.26f,y-s*.42f); lineTo(x+s*.47f,y-s*.66f); lineTo(x+s*.45f,y-s*.2f); close() }
            c.drawPath(inner1,p); c.drawPath(inner2,p)
            p.color = 0xFF14251F.toInt()
            if (blink) {
                p.strokeWidth = s*.045f; p.style = Paint.Style.STROKE
                c.drawLine(x-s*.25f,y-s*.02f,x-s*.12f,y-s*.02f,p)
                c.drawLine(x+s*.12f,y-s*.02f,x+s*.25f,y-s*.02f,p)
                p.style = Paint.Style.FILL
            } else {
                c.drawCircle(x-s*.19f,y-s*.02f,s*.055f,p)
                c.drawCircle(x+s*.19f,y-s*.02f,s*.055f,p)
            }
            p.color = 0xFFFFB7A7.toInt()
            c.drawCircle(x,y+s*.12f,s*.05f,p)
            p.color = 0xFF14251F.toInt()
            p.strokeWidth = s*.025f; p.style = Paint.Style.STROKE
            c.drawLine(x,y+s*.16f,x-s*.08f,y+s*.22f,p)
            c.drawLine(x,y+s*.16f,x+s*.08f,y+s*.22f,p)
            p.style = Paint.Style.FILL
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
                player = MediaPlayer.create(this, id)?.apply {
                    setOnCompletionListener { it.release(); player = null; stopSelf() }
                    start()
                }
            } else stopSelf()
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }
    override fun onDestroy() { player?.release(); player = null; if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else stopForeground(true); super.onDestroy() }
}
