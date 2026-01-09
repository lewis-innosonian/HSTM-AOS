package com.example.hstm_aos.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import com.example.hstm_aos.R

class RoundedButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr) {

    private var radius = 0f
    private var bgColor = Color.TRANSPARENT

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()

    init {
        background = null
        isAllCaps = false

        context.obtainStyledAttributes(attrs, R.styleable.RoundedButton).apply {
            try {
                radius = getDimension(R.styleable.RoundedButton_rb_radius, 0f)
                bgColor = getColor(
                    R.styleable.RoundedButton_rb_bgColor,
                    Color.TRANSPARENT
                )
            } finally {
                recycle()
            }
        }

        paint.color = bgColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        path.reset()
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)
        canvas.clipPath(path)
        super.onDraw(canvas)
    }

    fun setRadius(radius: Float) {
        this.radius = radius
        invalidate()
    }

    fun setBgColor(color: Int) {
        bgColor = color
        paint.color = color
        invalidate()
    }
}
