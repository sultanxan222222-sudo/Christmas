package com.christmasdream.launcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/** A lightweight, self-contained New Year overlay. No network assets or video decoder required. */
class NewYearCelebrationView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private val flakes = List(90) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), 1.2f + Random.nextFloat() * 3f) }
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 20_000L
        interpolator = LinearInterpolator()
        addUpdateListener { progress = it.animatedValue as Float; invalidate() }
    }

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        isClickable = false
        isFocusable = false
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        c.drawColor(0xB8000715.toInt())

        // Snowfall and warm festive glints.
        flakes.forEachIndexed { i, f ->
            val y = ((f[1] + progress * (0.18f + f[2] * 0.025f)) % 1.08f) * h
            val x = (f[0] + kotlin.math.sin(progress * 12f + i) * 0.012f) * w
            p.color = if (i % 7 == 0) 0xFFFFD98A.toInt() else 0xDDF4FBFF.toInt()
            p.alpha = 130 + (i % 4) * 30
            c.drawCircle(x, y, f[2], p)
        }

        // Firework bursts in three timed waves.
        val burstAlpha = when {
            progress < .24f -> (progress / .24f)
            progress < .82f -> 1f
            else -> (1f - progress) / .18f
        }.coerceIn(0f, 1f)
        if (burstAlpha > 0f) {
            val centers = listOf(.22f to .28f, .76f to .24f, .52f to .38f)
            centers.forEachIndexed { index, center ->
                val local = ((progress * 1.65f + index * .29f) % 1f)
                val radius = 12f + local * (w.coerceAtMost(h) * .16f)
                p.strokeWidth = dp(1).toFloat()
                p.style = Paint.Style.STROKE
                p.color = if (index % 2 == 0) 0xFFFFD27A.toInt() else 0xFFBDEBFF.toInt()
                p.alpha = (burstAlpha * (1f - local) * 230).toInt().coerceIn(0, 255)
                for (ray in 0 until 12) {
                    val a = Math.PI * 2 * ray / 12
                    val x = center.first * w
                    val y = center.second * h
                    val r1 = radius * .55f
                    val r2 = radius * (1f + (ray % 3) * .13f)
                    c.drawLine(x + kotlin.math.cos(a).toFloat() * r1, y + kotlin.math.sin(a).toFloat() * r1,
                        x + kotlin.math.cos(a).toFloat() * r2, y + kotlin.math.sin(a).toFloat() * r2, p)
                }
                p.style = Paint.Style.FILL
            }
        }

        // Glowing Christmas tree with animated coloured lights on the lower-left edge.
        val treeX = w * .13f
        val treeBase = h * .88f
        p.style = Paint.Style.FILL
        p.color = 0xFF164C3A.toInt()
        val tree = android.graphics.Path().apply {
            moveTo(treeX, h*.48f); lineTo(treeX-w*.085f,h*.67f); lineTo(treeX-w*.045f,h*.67f)
            lineTo(treeX-w*.12f,h*.80f); lineTo(treeX+w*.12f,h*.80f); lineTo(treeX+w*.045f,h*.67f)
            lineTo(treeX+w*.085f,h*.67f); close()
        }
        c.drawPath(tree,p)
        p.color = 0xFF9A6235.toInt(); c.drawRect(treeX-w*.018f,h*.80f,treeX+w*.018f,treeBase,p)
        for(i in 0 until 11) {
            val yy=h*(.55f+i*.025f); val spread=w*(.018f+i*.006f)
            p.color = when((i+(progress*18).toInt())%3){0->0xFFFFD65C.toInt();1->0xFFFF526B.toInt();else->0xFF79DFFF.toInt()}
            p.alpha = (150+105*kotlin.math.abs(kotlin.math.sin(progress*18+i))).toInt().coerceIn(0,255)
            c.drawCircle(treeX+(if(i%2==0) -spread else spread),yy,dp(3).toFloat(),p)
        }
        p.alpha=255
        // Draw a small sleigh and two reindeer as vector shapes, moving across the sky.
        if (progress in .30f.. .76f) {
            val t = (progress - .30f) / .46f
            val x = (-.22f + t * 1.44f) * w
            val y = h * (.20f + kotlin.math.sin(t * Math.PI).toFloat() * .035f)
            p.color = 0xFFFFDFA3.toInt(); p.strokeWidth=dp(2).toFloat(); p.style=Paint.Style.STROKE
            c.drawLine(x,y,x+dp(38),y-dp(5),p)
            c.drawLine(x+dp(38),y-dp(5),x+dp(60),y,p)
            c.drawLine(x+dp(5),y+dp(13),x+dp(55),y+dp(13),p)
            c.drawArc(x+dp(4),y+dp(4),x+dp(57),y+dp(23),0f,180f,false,p)
            p.style=Paint.Style.FILL
            c.drawCircle(x-dp(8),y-dp(3),dp(5).toFloat(),p)
            c.drawCircle(x-dp(21),y-dp(3),dp(5).toFloat(),p)
            c.drawLine(x-dp(8),y-dp(3),x-dp(13),y-dp(14),p)
            c.drawLine(x-dp(21),y-dp(3),x-dp(26),y-dp(14),p)
        }

        // Main greeting fades in after the Santa greeting starts.
        if (progress > .43f) {
            val alpha = (((progress - .43f) / .16f).coerceIn(0f, 1f) * 255).toInt()
            p.textAlign = Paint.Align.CENTER
            p.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            p.color = 0xFFFFD98A.toInt()
            p.alpha = alpha
            p.textSize = dp(29).toFloat()
            c.drawText("С НОВЫМ ГОДОМ!", w / 2f, h * .58f, p)
            p.color = Color.WHITE
            p.textSize = dp(15).toFloat()
            c.drawText("Пусть в доме всегда живут тепло и чудо", w / 2f, h * .58f + dp(30), p)
            p.textAlign = Paint.Align.LEFT
            p.alpha = 255
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
