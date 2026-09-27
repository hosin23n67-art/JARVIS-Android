package com.kamyab.jarvis

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View

class ThinkingOrbView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private var thinking = false
    private var startedAt = 0L
    private var elapsedSeconds = 0L
    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 2600
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            phase = it.animatedValue as Float
            if (thinking && startedAt > 0L) elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000L
            invalidate()
        }
    }
    init { setLayerType(View.LAYER_TYPE_SOFTWARE, null) }
    fun setThinking(value: Boolean) {
        thinking = value
        if (value) {
            startedAt = System.currentTimeMillis()
            elapsedSeconds = 0L
            if (!animator.isStarted) animator.start()
        } else {
            startedAt = 0L
            elapsedSeconds = 0L
            invalidate()
        }
    }
    override fun onDetachedFromWindow() { animator.cancel(); super.onDetachedFromWindow() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) * 0.30f
        paint.shader = RadialGradient(cx, cy, r * 1.45f,
            intArrayOf(if (thinking) Color.argb(105, 65, 230, 255) else Color.argb(55, 65, 230, 255), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r * 1.45f, paint)
        paint.shader = RadialGradient(cx - r*.32f, cy-r*.38f, r*1.25f,
            intArrayOf(Color.WHITE, Color.rgb(85,225,255), Color.rgb(25,75,235), Color.rgb(3,8,35)),
            floatArrayOf(0f,.18f,.58f,1f), Shader.TileMode.CLAMP)
        paint.setShadowLayer(if (thinking) 26f else 12f,0f,0f,Color.rgb(50,210,255))
        canvas.drawCircle(cx,cy,r,paint)
        paint.clearShadowLayer()
        paint.style=Paint.Style.STROKE
        paint.strokeWidth=dp(4f)
        paint.shader=SweepGradient(cx,cy,intArrayOf(Color.TRANSPARENT,Color.rgb(100,245,255),Color.rgb(40,90,255),Color.TRANSPARENT))
        canvas.save()
        canvas.rotate(phase,cx,cy)
        canvas.drawArc(cx-r*1.25f,cy-r*1.25f,cx+r*1.25f,cy+r*1.25f,-35f,115f,false,paint)
        canvas.restore()
        paint.style=Paint.Style.FILL
        paint.shader=null
        paint.color=Color.WHITE
        paint.textAlign=Paint.Align.CENTER
        paint.typeface=Typeface.DEFAULT_BOLD
        paint.textSize=dp(8f)
        val label=if(thinking) "THINKING • "+elapsedSeconds+"s" else "JARVIS"
        canvas.drawText(label,cx,cy+r+dp(18f),paint)
    }
    private fun dp(v:Float)=v*resources.displayMetrics.density
}