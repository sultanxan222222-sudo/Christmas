package com.christmasdream.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.min
import kotlin.math.sin
import java.util.Random

/**
 * Animated Ramadan hero artwork.
 *
 * The mosque illumination is driven by the real prayer timetable supplied by
 * MainActivity. This view never invents or schedules prayer times itself.
 */
class RamadanHeroScene(
    context: Context,
    private val prayerLightOn: () -> Boolean,
    private val catStyle: () -> Int,
    private val onMosqueClick: () -> Unit
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private val stars = Array(34) {
        val random = Random(2401L + it * 37L)
        floatArrayOf(random.nextFloat(), random.nextFloat() * 0.34f, 0.7f + random.nextFloat() * 1.8f)
    }
    private val animator = object : Runnable {
        override fun run() {
            phase += 0.045f
            if (phase > 1000f) phase = 0f
            invalidate()
            postDelayed(this, 42L)
        }
    }

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        isFocusable = true
        isClickable = true
        contentDescription = "Интерактивная мечеть Рамадана. Нажмите, чтобы открыть расписание намазов."
        setOnClickListener { onMosqueClick() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(animator)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animator)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        drawStars(canvas, w, h)
        drawFlags(canvas, w, h)
        drawMoonAndStar(canvas, w, h)
        drawGround(canvas, w, h)
        drawMosque(canvas, w, h)
        drawTreeAndKitten(canvas, w, h)
        drawLantern(canvas, w * 0.405f, h * 0.855f, h * 0.105f, 0.2f)
        drawLantern(canvas, w * 0.925f, h * 0.855f, h * 0.105f, 1.1f)

        if (hasFocus()) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2.2f
            paint.color = 0x99FFD98B.toInt()
            canvas.drawRoundRect(RectF(2f, 2f, w - 2f, h - 2f), 18f, 18f, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawStars(c: Canvas, w: Float, h: Float) {
        stars.forEachIndexed { index, star ->
            val shimmer = (0.55f + 0.45f * sin(phase * 0.7f + index)).coerceIn(0.1f, 1f)
            paint.color = Color.argb((90f + 130f * shimmer).toInt(), 226, 238, 210)
            c.drawCircle(star[0] * w, star[1] * h, star[2], paint)
        }
    }

    private fun drawFlags(c: Canvas, w: Float, h: Float) {
        val count = if (w > 760f) 7 else 3
        val start = w * 0.105f
        val end = w * 0.765f
        val ropeY = h * 0.105f
        paint.color = 0xFFB88A49.toInt()
        paint.strokeWidth = maxOf(1.2f, h * 0.003f)
        paint.style = Paint.Style.STROKE
        val rope = Path().apply {
            moveTo(start, ropeY)
            quadTo(w * 0.43f, ropeY + h * 0.065f, end, ropeY)
        }
        c.drawPath(rope, paint)
        paint.style = Paint.Style.FILL

        val spacing = if (count > 1) (end - start) / (count - 1) else 0f
        val flagW = if (count == 7) w * 0.075f else w * 0.165f
        val flagH = h * 0.145f
        repeat(count) { i ->
            val x = start + spacing * i
            val y = ropeY + h * 0.025f + sin(phase * 1.2f + i * 0.75f) * h * 0.006f
            val sway = sin(phase * 1.15f + i * 0.8f) * 5.2f
            c.save()
            c.rotate(sway, x, y)
            val flag = Path().apply {
                moveTo(x - flagW * 0.5f, y)
                lineTo(x + flagW * 0.5f, y)
                lineTo(x + flagW * 0.42f, y + flagH * 0.86f)
                lineTo(x, y + flagH)
                lineTo(x - flagW * 0.42f, y + flagH * 0.86f)
                close()
            }
            paint.color = 0xFF0B5A40.toInt()
            paint.setShadowLayer(5f, 0f, 2f, 0x55000000)
            c.drawPath(flag, paint)
            paint.clearShadowLayer()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxOf(1f, h * 0.003f)
            paint.color = 0xFFD8B36B.toInt()
            c.drawPath(flag, paint)
            paint.style = Paint.Style.FILL
            paint.color = 0xFFFFD98A.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            paint.textSize = if (count == 7) h * 0.031f else h * 0.043f
            c.drawText("РАМАДАН", x, y + flagH * 0.56f + paint.textSize * 0.34f, paint)
            c.restore()
        }
    }

    private fun drawMoonAndStar(c: Canvas, w: Float, h: Float) {
        val x = w * 0.86f
        val y = h * 0.205f
        val r = h * 0.075f
        paint.color = 0xFFFFD77E.toInt()
        paint.setShadowLayer(h * 0.045f, 0f, 0f, 0xFFFFC45B.toInt())
        c.drawCircle(x, y, r, paint)
        paint.clearShadowLayer()
        paint.color = 0xFF08241F.toInt()
        c.drawCircle(x + r * 0.43f, y - r * 0.23f, r * 0.82f, paint)
        drawFivePointStar(c, x + r * 1.17f, y - r * 0.78f, r * 0.33f, 0xFFFFD98A.toInt())
    }

    private fun drawFivePointStar(c: Canvas, x: Float, y: Float, radius: Float, color: Int) {
        val path = Path()
        for (i in 0 until 10) {
            val a = Math.toRadians(-90.0 + i * 36.0).toFloat()
            val r = if (i % 2 == 0) radius else radius * 0.43f
            val px = x + kotlin.math.cos(a) * r
            val py = y + kotlin.math.sin(a) * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        paint.color = color
        paint.setShadowLayer(radius * 0.7f, 0f, 0f, color)
        c.drawPath(path, paint)
        paint.clearShadowLayer()
    }

    private fun drawGround(c: Canvas, w: Float, h: Float) {
        val ground = Path().apply {
            moveTo(0f, h * 0.875f)
            quadTo(w * 0.22f, h * 0.82f, w * 0.43f, h * 0.875f)
            quadTo(w * 0.72f, h * 0.91f, w, h * 0.84f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        paint.color = 0xFF071B16.toInt()
        c.drawPath(ground, paint)
        paint.color = 0xFF173B2D.toInt()
        paint.strokeWidth = maxOf(1f, h * 0.003f)
        paint.style = Paint.Style.STROKE
        c.drawLine(0f, h * 0.89f, w, h * 0.89f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawMosque(c: Canvas, w: Float, h: Float) {
        val glowOn = prayerLightOn()
        val pulse = if (glowOn) 0.82f + 0.18f * sin(phase * 2.1f) else 0.10f
        val goldAlpha = if (glowOn) (185f + 70f * pulse).toInt() else 92
        val center = w * 0.555f
        val left = w * 0.315f
        val right = w * 0.795f
        val base = h * 0.835f
        val bodyTop = h * 0.49f

        // Soft pool of light appears only during a scheduled prayer window.
        if (glowOn) {
            paint.color = Color.argb((30f + 24f * pulse).toInt(), 255, 190, 88)
            paint.setShadowLayer(h * 0.11f, 0f, 0f, 0xFFFFB64E.toInt())
            c.drawOval(left - h * 0.08f, h * 0.42f, right + h * 0.08f, base + h * 0.02f, paint)
            paint.clearShadowLayer()
        }

        // Main prayer hall and its gold cornice.
        paint.color = 0xFF12392F.toInt()
        c.drawRoundRect(left, bodyTop, right, base, h * 0.035f, h * 0.035f, paint)
        paint.color = 0xFF0A241F.toInt()
        c.drawRect(left - h * 0.025f, bodyTop - h * 0.018f, right + h * 0.025f, bodyTop + h * 0.025f, paint)
        paint.color = 0xFFC19A59.toInt()
        c.drawRect(left - h * 0.018f, bodyTop, right + h * 0.018f, bodyTop + h * 0.009f, paint)

        // Large central dome with a distinct outline.
        val dome = Path().apply {
            moveTo(center - w * 0.145f, bodyTop + h * 0.005f)
            cubicTo(center - w * 0.142f, h * 0.305f, center - w * 0.095f, h * 0.245f, center, h * 0.235f)
            cubicTo(center + w * 0.095f, h * 0.245f, center + w * 0.142f, h * 0.305f, center + w * 0.145f, bodyTop + h * 0.005f)
            close()
        }
        paint.color = 0xFF1D5542.toInt()
        c.drawPath(dome, paint)
        paint.color = 0xFFD1AB69.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1.5f, h * 0.006f)
        c.drawPath(dome, paint)
        paint.style = Paint.Style.FILL
        paint.color = 0xFFB68D50.toInt()
        c.drawRect(center - w * 0.006f, h * 0.224f, center + w * 0.006f, h * 0.196f, paint)
        paint.color = 0xFFFFD98A.toInt()
        paint.setShadowLayer(h * 0.025f, 0f, 0f, 0xFFFFC45B.toInt())
        c.drawCircle(center, h * 0.19f, h * 0.012f, paint)
        paint.clearShadowLayer()
        drawSmallCrescent(c, center, h * 0.16f, h * 0.025f)

        // Side domes.
        drawSideDome(c, w * 0.405f, bodyTop + h * 0.01f, w * 0.074f, h * 0.13f)
        drawSideDome(c, w * 0.705f, bodyTop + h * 0.01f, w * 0.074f, h * 0.13f)

        // Two tall minarets framing the mosque.
        drawMinaret(c, w * 0.292f, h, pulse, glowOn)
        drawMinaret(c, w * 0.815f, h, pulse, glowOn)

        // Tall central entrance.
        drawArchWindow(c, center, h * 0.605f, w * 0.072f, h * 0.225f, glowOn, pulse, true)
        // Rows of arched windows on both wings.
        listOf(w * 0.365f, w * 0.445f, w * 0.665f, w * 0.745f).forEachIndexed { index, x ->
            drawArchWindow(c, x, h * 0.59f, w * 0.034f, h * 0.115f, glowOn, pulse, false)
            drawArchWindow(c, x, h * 0.725f, w * 0.034f, h * 0.075f, glowOn, pulse, false)
        }

        // Decorative facade bands and small columns.
        paint.color = 0xFFD0A96A.toInt()
        paint.strokeWidth = maxOf(1f, h * 0.003f)
        paint.style = Paint.Style.STROKE
        c.drawLine(left + h * 0.03f, h * 0.565f, right - h * 0.03f, h * 0.565f, paint)
        c.drawLine(left + h * 0.03f, h * 0.815f, right - h * 0.03f, h * 0.815f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawSmallCrescent(c: Canvas, x: Float, y: Float, r: Float) {
        paint.color = 0xFFFFD98A.toInt()
        c.drawCircle(x, y, r, paint)
        paint.color = 0xFF1D5542.toInt()
        c.drawCircle(x + r * 0.42f, y - r * 0.24f, r * 0.82f, paint)
    }

    private fun drawSideDome(c: Canvas, center: Float, base: Float, width: Float, height: Float) {
        val path = Path().apply {
            moveTo(center - width, base)
            cubicTo(center - width * 0.94f, base - height * 0.7f, center - width * 0.42f, base - height, center, base - height)
            cubicTo(center + width * 0.42f, base - height, center + width * 0.94f, base - height * 0.7f, center + width, base)
            close()
        }
        paint.color = 0xFF174A39.toInt()
        c.drawPath(path, paint)
        paint.color = 0xFFC6A166.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.4f
        c.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawMinaret(c: Canvas, x: Float, h: Float, pulse: Float, glowOn: Boolean) {
        val width = h * 0.052f
        val top = h * 0.265f
        val bottom = h * 0.84f
        paint.color = 0xFF174A3B.toInt()
        c.drawRoundRect(x - width * 0.5f, top + h * 0.08f, x + width * 0.5f, bottom, width * 0.12f, width * 0.12f, paint)
        paint.color = 0xFF0B2D25.toInt()
        c.drawRect(x - width * 0.85f, h * 0.54f, x + width * 0.85f, h * 0.565f, paint)
        c.drawRect(x - width * 0.78f, h * 0.38f, x + width * 0.78f, h * 0.40f, paint)
        // Lantern room.
        paint.color = 0xFF164A3A.toInt()
        c.drawRoundRect(x - width * 0.95f, top + h * 0.03f, x + width * 0.95f, top + h * 0.16f, width * 0.18f, width * 0.18f, paint)
        paint.color = 0xFFC9A263.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, h * 0.004f)
        c.drawRect(x - width * 1.05f, top + h * 0.025f, x + width * 1.05f, top + h * 0.165f, paint)
        paint.style = Paint.Style.FILL
        // Tower windows glow at prayer time.
        paint.color = if (glowOn) Color.argb((170f + 85f * pulse).toInt().coerceIn(0, 255), 255, 211, 133)
            else 0x554E805F
        if (glowOn) paint.setShadowLayer(h * 0.055f, 0f, 0f, 0xFFFFB94D.toInt())
        c.drawRoundRect(x - width * 0.20f, top + h * 0.065f, x + width * 0.20f, top + h * 0.125f, 3f, 3f, paint)
        c.drawRoundRect(x - width * 0.14f, h * 0.60f, x + width * 0.14f, h * 0.65f, 3f, 3f, paint)
        paint.clearShadowLayer()
        // Pointed roof and finial.
        val roof = Path().apply {
            moveTo(x - width * 0.78f, top + h * 0.035f)
            lineTo(x, top - h * 0.055f)
            lineTo(x + width * 0.78f, top + h * 0.035f)
            close()
        }
        paint.color = 0xFFB99054.toInt()
        c.drawPath(roof, paint)
        paint.color = 0xFFFFD98A.toInt()
        c.drawLine(x, top - h * 0.055f, x, top - h * 0.085f, paint)
        c.drawCircle(x, top - h * 0.09f, h * 0.008f, paint)
    }

    private fun drawArchWindow(c: Canvas, x: Float, y: Float, width: Float, height: Float,
                               glowOn: Boolean, pulse: Float, door: Boolean) {
        val rect = RectF(x - width * 0.5f, y, x + width * 0.5f, y + height)
        paint.color = 0xFF071C18.toInt()
        c.drawRoundRect(rect, width * 0.5f, width * 0.5f, paint)
        val light = if (glowOn) Color.argb((185f + 65f * pulse).toInt().coerceIn(0, 255), 255, 208, 126)
            else 0x554C6648
        paint.color = light
        if (glowOn) paint.setShadowLayer(height * 0.28f, 0f, 0f, 0xFFFFB84F.toInt())
        val inner = RectF(rect.left + width * 0.15f, rect.top + height * 0.10f,
            rect.right - width * 0.15f, rect.bottom - height * 0.08f)
        c.drawRoundRect(inner, width * 0.40f, width * 0.40f, paint)
        paint.clearShadowLayer()
        paint.color = 0xFFC79E5D.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, width * 0.055f)
        c.drawRoundRect(rect, width * 0.5f, width * 0.5f, paint)
        paint.style = Paint.Style.FILL
        if (door) {
            paint.color = 0xFFD0A15D.toInt()
            paint.strokeWidth = maxOf(1f, width * 0.035f)
            paint.style = Paint.Style.STROKE
            c.drawLine(x, y + height * 0.15f, x, y + height * 0.9f, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawTreeAndKitten(c: Canvas, w: Float, h: Float) {
        val trunkX = w * 0.178f
        val rootY = h * 0.875f
        val sway = sin(phase * 0.32f) * 1.8f
        // Ground shadow beneath the tree.
        paint.color = 0x99030E0A.toInt()
        c.drawOval(w * 0.025f, h * 0.79f, w * 0.35f, h * 0.905f, paint)

        // Old, low, spreading trunk and roots, inspired by the broad Sahabi tree silhouette.
        paint.color = 0xFF493322.toInt()
        paint.strokeWidth = w * 0.025f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        val trunk = Path().apply {
            moveTo(trunkX, rootY)
            cubicTo(trunkX - w * 0.035f, h * 0.79f, trunkX + w * 0.008f, h * 0.69f, trunkX + sway, h * 0.605f)
            cubicTo(trunkX - w * 0.035f, h * 0.57f, w * 0.105f, h * 0.545f, w * 0.075f, h * 0.535f)
            moveTo(trunkX + sway, h * 0.66f)
            cubicTo(w * 0.235f, h * 0.60f, w * 0.265f, h * 0.565f, w * 0.30f, h * 0.56f)
            moveTo(trunkX, h * 0.70f)
            cubicTo(w * 0.115f, h * 0.65f, w * 0.085f, h * 0.62f, w * 0.055f, h * 0.61f)
            moveTo(trunkX, rootY)
            cubicTo(w * 0.12f, h * 0.875f, w * 0.09f, h * 0.895f, w * 0.055f, h * 0.89f)
            moveTo(trunkX, rootY)
            cubicTo(w * 0.22f, h * 0.875f, w * 0.25f, h * 0.895f, w * 0.29f, h * 0.89f)
        }
        c.drawPath(trunk, paint)
        paint.strokeWidth = w * 0.008f
        paint.color = 0xFF86603A.toInt()
        c.drawPath(trunk, paint)
        paint.style = Paint.Style.FILL

        // Wide crown, with a gentle wind sway and overlapping leaf masses.
        c.save()
        c.rotate(sway, trunkX, h * 0.68f)
        paint.color = 0xFF0B3526.toInt()
        c.drawOval(w * 0.018f, h * 0.48f, w * 0.345f, h * 0.77f, paint)
        val leaves = listOf(
            floatArrayOf(0.075f,0.61f,0.075f), floatArrayOf(0.115f,0.56f,0.083f),
            floatArrayOf(0.17f,0.535f,0.092f), floatArrayOf(0.235f,0.55f,0.09f),
            floatArrayOf(0.29f,0.61f,0.075f), floatArrayOf(0.10f,0.68f,0.075f),
            floatArrayOf(0.18f,0.66f,0.10f), floatArrayOf(0.265f,0.68f,0.082f)
        )
        leaves.forEachIndexed { i, leaf ->
            paint.color = when (i % 4) {
                0 -> 0xFF174B31.toInt()
                1 -> 0xFF1D5A39.toInt()
                2 -> 0xFF245F3B.toInt()
                else -> 0xFF103D2A.toInt()
            }
            c.drawOval(w * (leaf[0] - leaf[2]), h * (leaf[1] - leaf[2] * 1.28f),
                w * (leaf[0] + leaf[2]), h * (leaf[1] + leaf[2] * 1.18f), paint)
        }
        paint.color = 0xFF34764A.toInt()
        repeat(18) { i ->
            val x = w * (0.045f + (i % 6) * 0.049f)
            val y = h * (0.55f + (i / 6) * 0.055f + (i % 2) * 0.012f)
            c.drawCircle(x, y, h * 0.008f, paint)
        }
        c.restore()

        // Small cat rests quietly in the shade below the trunk.
        drawRestingKitten(c, w * 0.205f, h * 0.824f, min(h * 0.118f, w * 0.052f))
    }

    private fun drawRestingKitten(c: Canvas, x: Float, ground: Float, s: Float) {
        val fur = when (catStyle()) {
            1 -> 0xFF9BA5AA.toInt()
            2 -> 0xFFE7A05A.toInt()
            else -> 0xFFF1EEE8.toInt()
        }
        val breathing = 1f + 0.025f * sin(phase * 0.9f)
        val tailSway = sin(phase * 0.72f) * s * 0.20f
        val blink = sin(phase * 0.19f) > 0.965f

        paint.color = 0xFF050F0B.toInt()
        c.drawOval(x - s * 1.0f, ground - s * 0.14f, x + s * 1.1f, ground + s * 0.08f, paint)

        // Curled tail rests alongside the paws but moves slightly in the breeze.
        paint.color = fur
        paint.strokeWidth = s * 0.19f
        paint.strokeCap = Paint.Cap.ROUND
        paint.style = Paint.Style.STROKE
        val tail = Path().apply {
            moveTo(x + s * 0.22f, ground - s * 0.30f)
            cubicTo(x + s * 1.00f, ground - s * 0.58f + tailSway,
                x + s * 1.05f, ground + s * 0.12f + tailSway,
                x + s * 0.42f, ground + s * 0.02f)
        }
        c.drawPath(tail, paint)
        paint.style = Paint.Style.FILL

        // Curled body and a gently breathing shoulder.
        c.save()
        c.scale(1f, breathing, x, ground - s * 0.35f)
        paint.color = fur
        paint.setShadowLayer(s * 0.20f, 0f, s * 0.05f, 0x663B2418)
        c.drawOval(x - s * 0.75f, ground - s * 0.72f, x + s * 0.52f, ground - s * 0.05f, paint)
        c.drawOval(x - s * 0.76f, ground - s * 0.20f, x - s * 0.20f, ground + s * 0.03f, paint)
        c.drawOval(x + s * 0.05f, ground - s * 0.19f, x + s * 0.54f, ground + s * 0.03f, paint)
        c.drawCircle(x - s * 0.12f, ground - s * 0.72f, s * 0.39f, paint)
        paint.clearShadowLayer()
        // Ears.
        val ears = Path().apply {
            moveTo(x - s * 0.43f, ground - s * 0.91f)
            lineTo(x - s * 0.42f, ground - s * 1.38f)
            lineTo(x - s * 0.05f, ground - s * 1.02f)
            close()
            moveTo(x + s * 0.02f, ground - s * 1.02f)
            lineTo(x + s * 0.31f, ground - s * 1.36f)
            lineTo(x + s * 0.39f, ground - s * 0.81f)
            close()
        }
        c.drawPath(ears, paint)
        paint.color = 0xFFE9A7A5.toInt()
        val inner = Path().apply {
            moveTo(x - s * 0.36f, ground - s * 1.02f)
            lineTo(x - s * 0.38f, ground - s * 1.27f)
            lineTo(x - s * 0.16f, ground - s * 1.04f)
            close()
            moveTo(x + s * 0.10f, ground - s * 1.04f)
            lineTo(x + s * 0.27f, ground - s * 1.25f)
            lineTo(x + s * 0.31f, ground - s * 0.94f)
            close()
        }
        c.drawPath(inner, paint)
        // Face and sleepy eyes.
        paint.color = 0xFF24332A.toInt()
        if (blink) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = s * 0.055f
            c.drawLine(x - s * 0.28f, ground - s * 0.77f, x - s * 0.12f, ground - s * 0.77f, paint)
            c.drawLine(x + s * 0.09f, ground - s * 0.77f, x + s * 0.25f, ground - s * 0.77f, paint)
            paint.style = Paint.Style.FILL
        } else {
            c.drawCircle(x - s * 0.20f, ground - s * 0.78f, s * 0.035f, paint)
            c.drawCircle(x + s * 0.17f, ground - s * 0.78f, s * 0.035f, paint)
        }
        paint.color = 0xFFFFB5A5.toInt()
        c.drawCircle(x - s * 0.02f, ground - s * 0.66f, s * 0.045f, paint)
        c.restore()
    }

    private fun drawLantern(c: Canvas, x: Float, bottom: Float, size: Float, offset: Float) {
        val sway = sin(phase * 0.65f + offset) * size * 0.035f
        val y = bottom - size
        c.save()
        c.rotate(sin(phase * 0.55f + offset) * 2.0f, x, bottom)
        // Handle.
        paint.color = 0xFFC79C5D.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, size * 0.045f)
        c.drawArc(RectF(x - size * 0.18f, y - size * 0.17f, x + size * 0.18f, y + size * 0.18f),
            180f, 180f, false, paint)
        paint.style = Paint.Style.FILL
        // Brass body.
        val body = RectF(x - size * 0.31f + sway, y + size * 0.08f, x + size * 0.31f + sway, bottom)
        paint.color = 0xFF9B713F.toInt()
        c.drawRoundRect(body, size * 0.08f, size * 0.08f, paint)
        paint.color = 0xFF193A2D.toInt()
        c.drawRoundRect(body.left + size * 0.055f, body.top + size * 0.075f,
            body.right - size * 0.055f, body.bottom - size * 0.075f, size * 0.05f, size * 0.05f, paint)
        // Moving flame and warm glow.
        val flamePulse = 0.88f + 0.12f * sin(phase * 3.3f + offset)
        val flame = Path().apply {
            moveTo(x + sway, y + size * 0.77f)
            cubicTo(x - size * 0.25f, y + size * 0.58f,
                x + size * (0.10f + 0.04f * sin(phase * 2f + offset)), y + size * 0.42f,
                x + sway, y + size * 0.22f)
            cubicTo(x + size * 0.28f, y + size * 0.50f,
                x + size * 0.22f, y + size * 0.65f,
                x + sway, y + size * 0.77f)
            close()
        }
        paint.color = 0xFFFF8B32.toInt()
        paint.setShadowLayer(size * 0.26f * flamePulse, 0f, 0f, 0xFFFF9B3D.toInt())
        c.drawPath(flame, paint)
        paint.clearShadowLayer()
        paint.color = 0xFFFFE6A0.toInt()
        c.drawOval(x - size * 0.045f + sway, y + size * 0.42f,
            x + size * 0.055f + sway, y + size * 0.72f, paint)
        // Brass lattice highlights.
        paint.color = 0xFFD5B16E.toInt()
        paint.strokeWidth = maxOf(1f, size * 0.025f)
        paint.style = Paint.Style.STROKE
        c.drawLine(body.left + size * 0.12f, body.top, body.left + size * 0.12f, body.bottom, paint)
        c.drawLine(body.right - size * 0.12f, body.top, body.right - size * 0.12f, body.bottom, paint)
        c.drawLine(body.left, body.centerY(), body.right, body.centerY(), paint)
        paint.style = Paint.Style.FILL
        c.restore()
    }
}
