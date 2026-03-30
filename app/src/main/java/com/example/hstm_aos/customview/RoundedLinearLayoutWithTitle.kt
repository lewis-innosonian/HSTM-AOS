package com.example.hstm_aos.customview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.res.ResourcesCompat
import com.example.hstm_aos.R

class RoundedLinearLayoutWithTitle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val activeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val rect = RectF()
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val titleRect = RectF()

    private var radius = 0f
    private var borderWidth = 0f

    private var titleText = ""
    private var titleBgColor = Color.WHITE
    private var titleVisible = false

    private var titlePaddingLR = 20f
    private var titlePaddingTB = 2f

    private var topExtra = 0f
    private var borderProgress = 0f

    init {
        setWillNotDraw(false)
        clipChildren = false
        clipToPadding = false

        val d = resources.displayMetrics.density
        titlePaddingLR *= d
        titlePaddingTB *= d

        context.obtainStyledAttributes(attrs, R.styleable.RoundedView).apply {
            radius = getDimension(R.styleable.RoundedView_rv_radius, 0f)
            borderWidth = getDimension(R.styleable.RoundedView_rv_borderWidth, 0f)

            bgPaint.color =
                getColor(R.styleable.RoundedView_rv_backgroundColor, Color.TRANSPARENT)

            borderPaint.apply {
                style = Paint.Style.STROKE
                strokeWidth = borderWidth
                color = Color.parseColor("#DDDDDD")
            }

            activeBorderPaint.apply {
                style = Paint.Style.STROKE
                strokeWidth = borderWidth
                color = Color.parseColor("#711BFF")
            }

            titlePaint.apply {
                textSize = 16 * resources.displayMetrics.scaledDensity
                textAlign = Paint.Align.CENTER
                color = Color.WHITE
                typeface = ResourcesCompat.getFont(context, R.font.inter_bold)
            }

            recycle()
        }
    }

    override fun onMeasure(w: Int, h: Int) {
        super.onMeasure(w, h)

        val fm = titlePaint.fontMetrics
        val height = fm.descent - fm.ascent + titlePaddingTB * 2
        topExtra = height / 2f
    }

    override fun onDraw(canvas: Canvas) {
        rect.set(0f, topExtra, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, radius, radius, bgPaint)

        val half = borderWidth / 2
        rect.inset(half, half)
        canvas.drawRoundRect(rect, radius, radius, borderPaint)

        if (borderProgress > 0f) {
            val path = Path().apply {
                addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            val pm = PathMeasure(path, false)
            val dst = Path()
            pm.getSegment(0f, pm.length * borderProgress, dst, true)
            canvas.drawPath(dst, activeBorderPaint)
        }
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
        if (!titleVisible) return

        val fm = titlePaint.fontMetrics
        val textH = fm.descent - fm.ascent + titlePaddingTB * 2
        val textW = titlePaint.measureText(titleText) + titlePaddingLR * 2

        val cx = width / 2f
        titleRect.set(cx - textW / 2, 0f, cx + textW / 2, textH)

        canvas.drawRoundRect(titleRect, textH / 2, textH / 2, Paint().apply {
            color = titleBgColor
            isAntiAlias = true
        })

        val y = titleRect.centerY() - (fm.ascent + fm.descent) / 2
        canvas.drawText(titleText, titleRect.centerX(), y, titlePaint)
    }

    /* =========================
     * PUBLIC API
     * ========================= */

    fun setMyTurn(title: String) {
        titleText = if (title.length > 21) {
            title.substring(0, 21) + "..."
        } else {
            title
        }

        titleBgColor = Color.parseColor("#711BFF")
        titleVisible = true

        animateBorder(1f)
    }

    fun setVirtualTurn() {
        titleVisible = false
        animateBorder(0f)
    }

    private fun animateBorder(target: Float) {
        ValueAnimator.ofFloat(borderProgress, target).apply {
            duration = 420
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                borderProgress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}
