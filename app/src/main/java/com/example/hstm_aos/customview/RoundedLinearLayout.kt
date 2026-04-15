package com.example.hstm_aos.customview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.LinearLayout
import com.example.hstm_aos.R

class RoundedLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val clipPath = Path()

    private var radius = 0f
    private var borderWidth = 0f

    init {
        setWillNotDraw(false)
        clipChildren = true
        clipToPadding = true
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.RoundedView,
            0,
            0
        ).apply {
            try {
                radius = getDimension(R.styleable.RoundedView_rv_radius, 0f)
                bgPaint.color = getColor(
                    R.styleable.RoundedView_rv_backgroundColor,
                    Color.TRANSPARENT
                )

                borderWidth =
                    getDimension(R.styleable.RoundedView_rv_borderWidth, 0f)

                borderPaint.color = getColor(
                    R.styleable.RoundedView_rv_borderColor,
                    Color.BLACK
                )

                borderPaint.style = Paint.Style.STROKE
                borderPaint.strokeWidth = borderWidth

            } finally {
                recycle()
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        val save = canvas.save()

        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        clipPath.reset()
        clipPath.addRoundRect(
            rect,
            radius,
            radius,
            Path.Direction.CW
        )
        canvas.clipPath(clipPath)

        super.dispatchDraw(canvas)
        canvas.restoreToCount(save)
    }

    override fun onDraw(canvas: Canvas) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, bgPaint)
        if (borderWidth > 0f) {
            val half = borderWidth / 2
            rect.set(half, half, width - half, height - half)
            canvas.drawRoundRect(rect, radius, radius, borderPaint)
        }
    }

    fun setRadius(radius: Float) {
        this.radius = radius
        invalidate()
    }

    fun setBackgroundColorInt(color: Int) {
        bgPaint.color = color
        invalidate()
    }

    fun setBorderColor(color: Int) {
        borderPaint.color = color
        invalidate()
    }

    fun setBorderWidth(width: Float) {
        borderWidth = width
        borderPaint.strokeWidth = width
        invalidate()
    }
}
