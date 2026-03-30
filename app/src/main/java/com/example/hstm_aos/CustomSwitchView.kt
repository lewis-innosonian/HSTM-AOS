package com.example.hstm_aos

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class CustomSwitchView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var isCheckedInternal = false
    var isChecked: Boolean
        get() = isCheckedInternal
        set(value) {
            if (isCheckedInternal != value) {
                isCheckedInternal = value
                animateThumb(value)
                onCheckedChangeListener?.invoke(value)
            }
        }

    private var thumbPosition = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val switchRect = RectF()
    private val thumbRadius get() = height / 2.5f

    var onCheckedChangeListener: ((Boolean) -> Unit)? = null

    init {
        setOnClickListener {
            isChecked = !isChecked
        }
    }

    private fun animateThumb(checked: Boolean) {
        val start = thumbPosition
        val end = if (checked) 1f else 0f

        ValueAnimator.ofFloat(start, end).apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                thumbPosition = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val switchWidth = width.toFloat()
        val switchHeight = height.toFloat()
        val radius = switchHeight / 2

        switchRect.set(0f, 0f, switchWidth, switchHeight)

        paint.color = if (thumbPosition > 0.5f) Color.parseColor("#56ED89") else Color.parseColor("#CCCCCC")
        canvas.drawRoundRect(switchRect, radius, radius, paint)

        paint.color = Color.WHITE
        val thumbX = 5 + (switchWidth - 2 * thumbRadius - 10) * thumbPosition + thumbRadius
        val thumbY = switchHeight / 2
        canvas.drawCircle(thumbX, thumbY, thumbRadius, paint)
    }
}
