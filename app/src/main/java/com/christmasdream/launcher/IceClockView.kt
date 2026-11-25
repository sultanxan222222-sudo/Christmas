package com.christmasdream.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/** Live analog ice clock. The four skins share the same real device-time mechanism. */
class IceClockView(context: Context, var skin: Int = 0) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tick = Runnable { invalidate(); postDelayed(tick, 1000L) }
    init { contentDescription = "Живые аналоговые часы"; isFocusable = false }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); post(tick) }
    override fun onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow() }
    private fun color(v: Int) = v
    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val cx=width/2f; val cy=height/2f; val r=minOf(cx,cy)*.91f
        if(r<=0) return
        val (case,dial,hand,accent)=when(skin){
            1-> intArrayOf(0xDDEFFFFF.toInt(),0xCCF7FDFF.toInt(),0xFF287BB8.toInt(),0xFF9BCDEB.toInt())
            2-> intArrayOf(0xDDE8F8FF.toInt(),0xCC7E182C.toInt(),0xFF8EDBFF.toInt(),0xFF54D7F2.toInt())
            3-> intArrayOf(0xE6F8FFFF.toInt(),0xCCF4F7FA.toInt(),0xFF2475B8.toInt(),0xFFFF4963.toInt())
            else-> intArrayOf(0xDDF7FFFF.toInt(),0xCC9E1C36.toInt(),0xFF67CFFF.toInt(),0xFF42DDF2.toInt())
        }
        p.style=Paint.Style.FILL; p.color=case; c.drawCircle(cx,cy,r,p)
        p.color=dial; c.drawCircle(cx,cy,r*.87f,p)
        p.style=Paint.Style.STROKE; p.strokeWidth=r*.055f; p.color=0xAAFFFFFF.toInt(); c.drawCircle(cx,cy,r*.94f,p)
        p.strokeWidth=r*.018f; p.color=accent
        for(i in 0 until 60){
            val a=Math.PI*i/30.0; val major=i%5==0
            val inner=r*(if(major).72f else .80f); val outer=r*.84f
            c.drawLine(cx+cos(a).toFloat()*inner,cy+sin(a).toFloat()*inner,cx+cos(a).toFloat()*outer,cy+sin(a).toFloat()*outer,p)
        }
        p.style=Paint.Style.FILL; p.textAlign=Paint.Align.CENTER; p.typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD)
        p.textSize=r*.19f; p.color=if(skin==2) Color.WHITE else hand
        for(n in 1..12){ val a=Math.PI*n/6.0; c.drawText(n.toString(),cx+sin(a).toFloat()*r*.61f,cy-cos(a).toFloat()*r*.61f+r*.065f,p) }
        // Small visible gear wheels rotate continuously, while hands follow wall-clock time.
        val now=java.util.Calendar.getInstance()
        p.color=accent; p.style=Paint.Style.STROKE; p.strokeWidth=r*.025f
        val gearAngle=now.get(java.util.Calendar.SECOND)*Math.PI/30.0
        for((gx,gy,gr) in listOf(Triple(cx-r*.48f,cy+r*.38f,r*.12f),Triple(cx+r*.48f,cy+r*.38f,r*.09f))){
            c.drawCircle(gx,gy,gr,p)
            for(k in 0 until 8){val a=gearAngle+k*Math.PI/4; c.drawLine(gx+cos(a).toFloat()*gr*.72f,gy+sin(a).toFloat()*gr*.72f,gx+cos(a).toFloat()*gr*1.25f,gy+sin(a).toFloat()*gr*1.25f,p)}
        }
        val sec=now.get(java.util.Calendar.SECOND); val min=now.get(java.util.Calendar.MINUTE)+sec/60f; val hour=now.get(java.util.Calendar.HOUR)*1f+min/60f
        fun hand(angle:Double,length:Float,stroke:Float,col:Int){
            p.color=col;p.strokeWidth=stroke;p.strokeCap=Paint.Cap.ROUND
            c.drawLine(cx,cy,cx+sin(angle).toFloat()*length,cy-cos(angle).toFloat()*length,p)
        }
        hand(hour*Math.PI/6,r*.43f,r*.075f,hand)
        hand(min*Math.PI/30,r*.65f,r*.045f,hand)
        hand(sec*Math.PI/30,r*.73f,r*.014f,accent)
        p.style=Paint.Style.FILL;p.color=0xFFFFF8E8.toInt();c.drawCircle(cx,cy,r*.055f,p)
    }
}
