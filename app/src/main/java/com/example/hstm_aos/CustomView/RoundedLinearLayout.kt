package com.example.hstm_aos.CustomView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.LinearLayout
import com.example.hstm_aos.R

class RoundedLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var radius = 0f

    init {
        setWillNotDraw(false)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.RoundedView,
            0,
            0
        ).apply {
            try {
                radius = getDimension(
                    R.styleable.RoundedView_rv_radius,
                    0f
                )
                paint.color = getColor(
                    R.styleable.RoundedView_rv_backgroundColor,
                    0x00000000
                )
            } finally {
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, paint)
        super.onDraw(canvas)
    }

    fun setRadius(radius: Float) {
        this.radius = radius
        invalidate()
    }

    fun setBackgroundColorInt(color: Int) {
        paint.color = color
        invalidate()
    }
}
