package com.example.hstm_aos.customview

import android.content.Context
import android.graphics.*
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
    private var strokeColor = Color.TRANSPARENT
    private var strokeWidth = 0f

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)

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
                strokeColor = getColor(
                    R.styleable.RoundedButton_rb_strokeColor,
                    Color.TRANSPARENT
                )
                strokeWidth = getDimension(
                    R.styleable.RoundedButton_rb_strokeWidth,
                    0f
                )
            } finally {
                recycle()
            }
        }

        fillPaint.style = Paint.Style.FILL
        fillPaint.color = bgColor

        strokePaint.style = Paint.Style.STROKE
        strokePaint.color = strokeColor
        strokePaint.strokeWidth = strokeWidth
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val halfStroke = strokeWidth / 2f

        rect.set(
            halfStroke,
            halfStroke,
            w.toFloat() - halfStroke,
            h.toFloat() - halfStroke
        )

        path.reset()
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        // 배경
        canvas.drawPath(path, fillPaint)

        // 테두리
        if (strokeWidth > 0f) {
            canvas.drawPath(path, strokePaint)
        }

        canvas.clipPath(path)
        super.onDraw(canvas)
    }

    fun setRadius(radius: Float) {
        this.radius = radius
        requestLayout()
        invalidate()
    }

    fun setBgColor(color: Int) {
        bgColor = color
        fillPaint.color = color
        invalidate()
    }

    fun setStroke(color: Int, width: Float) {
        strokeColor = color
        strokeWidth = width
        strokePaint.color = color
        strokePaint.strokeWidth = width
        requestLayout()
        invalidate()
    }
}
