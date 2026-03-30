package com.example.hstm_aos

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.graphics.Matrix

class AnimatedGradientView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var linearGradient: LinearGradient
    private var dx = 0f

    init {
        // 애니메이션
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 2000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener {
            dx = it.animatedFraction
            invalidate()
        }
        animator.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        linearGradient = LinearGradient(
            -w.toFloat(), 0f, w.toFloat(), 0f,
            intArrayOf(Color.parseColor("#4A90E2"), Color.parseColor("#50A0F0"), Color.parseColor("#007AFF")),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.MIRROR
        )
        paint.shader = linearGradient
    }

    override fun onDraw(canvas: Canvas) {
        // 이동시키기
        val matrix = Matrix()
        matrix.setTranslate(width * dx, 0f)
        linearGradient.setLocalMatrix(matrix)
        val radius = 12f.dpToPx()
        canvas.drawRoundRect(
            0f, 0f,
            width.toFloat(), height.toFloat(),
            radius, radius,
            paint
        )
    }

    private fun Float.dpToPx() = this * resources.displayMetrics.density
}
