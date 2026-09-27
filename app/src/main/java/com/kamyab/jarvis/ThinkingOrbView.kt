package com.kamyab.jarvis

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class ThinkingOrbView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private var thinking = false
    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 2600
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { phase = it.animatedValue as Float; invalidate() }
    }

    init { setLayerType(View.LAYER_TYPE_SOFTWARE, null) }

    fun setThinking(value: Boolean) {
        thinking = value
        if (value && !animator.isStarted) animator.start()
        if (!value) invalidate()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = (minOf(width, height) * 0.36f)

        // Outer glow
        paint.shader = RadialGradient(cx, cy, r * 1.35f,
            intArrayOf(Color.argb(if (thinking) 105 else 55, 65, 230, 255), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r * 1.35f, paint)

        // 3D sphere lighting
        paint.shader = RadialGradient(cx - r * .32f, cy - r * .38f, r * 1.25f,
            intArrayOf(Color.WHITE, Color.rgb(85, 225, 255), Color.rgb(25, 75, 235), Color.rgb(3, 8, 35)),
            floatArrayOf(0f, .18f, .58f, 1f), Shader.TileMode.CLAMP)
        paint.setShadowLayer(if (thinking) 26f else 12f, 0f, 0f, Color.rgb(50, 210, 255))
        canvas.drawCircle(cx, cy, r, paint)
        paint.clearShadowLayer()

        // Rotating energy ring
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(5f)
        paint.shader = SweepGradient(cx, cy,
            intArrayOf(Color.TRANSPARENT, Color.rgb(100, 245, 255), Color.rgb(40, 90, 255), Color.TRANSPARENT))
        canvas.save()
        canvas.rotate(phase, cx, cy)
        canvas.drawArc(cx-r*1.08f, cy-r*1.08f, cx+r*1.08f, cy+r*1.08f, -35f, 115f, false, paint)
        canvas.rotate(120f, cx, cy)
        canvas.drawArc(cx-r*1.12f, cy-r*1.12f, cx+r*1.12f, cy+r*1.12f, -20f, 75f, false, paint)
        canvas.restore()

        // Floating highlight for depth
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(cx-r*.32f, cy-r*.38f, r*.32f,
            Color.argb(190,255,255,255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx-r*.32f, cy-r*.38f, r*.32f, paint)

        paint.shader = null
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = dp(12f)
        canvas.drawText(if (thinking) "THINKING" else "JARVIS", cx, cy + r + dp(30f), paint)
        paint.style = Paint.Style.FILL
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
}