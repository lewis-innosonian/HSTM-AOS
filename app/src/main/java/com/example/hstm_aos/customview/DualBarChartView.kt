package com.example.hstm_aos.customview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.HorizontalScrollView

class DualBarChartView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    private var trainingType: String = "CPR"
    private var isVirtualTime = false
    private val strokeWidth = dpToPx(1f)
    private val barWidth = dpToPx(20f)
    private val barSpacing = dpToPx(14f)
    private val cornerRadius = dpToPx(20f)
    private var virtualStartTime: Long = 0L
    private val barColors = mutableListOf<Int>()
    private val topChartPaint = Paint().apply {
        color = Color.parseColor("#999999")
        style = Paint.Style.STROKE
        strokeWidth = this@DualBarChartView.strokeWidth
        isAntiAlias = true
    }

    private val greenPaint = Paint().apply {
        color = Color.parseColor("#56ED89")
        style = Paint.Style.FILL
    }

    private val yellowPaint = Paint().apply {
        color = Color.parseColor("#FFD600")
        style = Paint.Style.FILL
    }

    private val grayPaint = Paint().apply {
        color = Color.parseColor("#CCCCCC")
        style = Paint.Style.FILL
    }

    private val redPaint = Paint().apply {
        color = Color.parseColor("#FF3B30")
        style = Paint.Style.FILL
    }

    private val topDataGreen = mutableListOf<Int>()
    private val topDataYellow = mutableListOf<Int>()
    private val timeStamps = mutableListOf<Long>()
    private val topBarProgress = mutableListOf<Float>()

    private var topMinValue = 50f
    private var topMaxValue = 60f

    private var minValid = 50f
    private var maxValid = 60f



    fun handleIncomingTopValue(green: Int, yellow: Int) {
        if (green <= 20) return

        topDataGreen.add(green)
        topDataYellow.add(yellow)
        timeStamps.add(System.currentTimeMillis())

        val color = if (isVirtualTime) Color.parseColor("#BAB5FF") else {
            when {
                green <= 49f -> Color.parseColor("#CCCCCC")
                green <= 60f -> Color.parseColor("#56ED89")
                else -> Color.parseColor("#FF3B30")
            }
        }

        barColors.add(color)

        topBarProgress.add(0f)
        animateBarProgress(topBarProgress.lastIndex)

        requestLayout()
        invalidate()
    }

    private fun animateBarProgress(index: Int) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 150
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            if (index < topBarProgress.size) {
                topBarProgress[index] = value
            }
            invalidate()
        }
        animator.start()
    }

    private fun calculateXOffsets(): List<Float> {
        val offsets = mutableListOf<Float>()
        val startMargin = dpToPx(10f)
        var x = startMargin

        for (i in timeStamps.indices) {
            if (i == 0) {
                offsets.add(x)
                continue
            }
            val diff = timeStamps[i] - timeStamps[i - 1]
            val extra = if (diff > 600) ((diff - 600).toFloat() / 500f) * barSpacing else 0f
            x += barWidth + barSpacing + extra
            offsets.add(x)
        }
        return offsets
    }

    init {
        val padding = dpToPx(10f).toInt()
        setPadding(padding, 0, padding, 0)
    }



    private fun calculateTotalWidth(): Float {
        val xOffsets = calculateXOffsets()
        val baseWidth =
            if (xOffsets.isNotEmpty()) xOffsets.last() + barWidth + barSpacing else suggestedMinimumWidth.toFloat()
        val extraPadding = dpToPx(100f)
        return baseWidth + extraPadding
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val desiredWidth = calculateTotalWidth().toInt()
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)

        val width = maxOf(desiredWidth, parentWidth)
        val height = resolveSize(400, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {

        val save = canvas.save()

        val scrollX = (parent as? HorizontalScrollView)?.scrollX ?: 0
        val parentWidth = (parent as View).width.toFloat()

        val chartRect = RectF(
            scrollX.toFloat(),
            0f,
            scrollX + parentWidth,
            height.toFloat()
        )

        val clip = Path().apply {
            addRoundRect(
                chartRect,
                cornerRadius,
                cornerRadius,
                Path.Direction.CW
            )
        }

        canvas.clipPath(clip)

        val xOffsets = calculateXOffsets()

        for (i in topDataGreen.indices) {

            val x = xOffsets[i]

            val green = topDataGreen[i].toFloat()
            val yellow = topDataYellow[i].toFloat()
            val progress = topBarProgress.getOrNull(i) ?: 1f

            val totalHeight = mapValueToHeight(green, height.toFloat())

            val yellowRatio = if (green > 0f) (yellow / green).coerceIn(0f, 1f) else 0f
            val yellowHeight = totalHeight * yellowRatio

            val animatedTotal = totalHeight * progress
            val animatedYellow = yellowHeight * progress

            val barColor = barColors.getOrNull(i) ?: Color.GRAY

            val paint = Paint().apply {
                color = barColor
                style = Paint.Style.FILL
            }

            // main bar
            canvas.drawRect(
                x,
                0f,
                x + barWidth,
                animatedTotal,
                paint
            )

            // yellow overlay (그대로 유지)
            if (yellow > 0) {
                canvas.drawRect(
                    x,
                    0f,
                    x + barWidth,
                    animatedYellow,
                    yellowPaint
                )
            }
        }

        canvas.restoreToCount(save)
    }

    private fun mapValueToHeight(value: Float, chartHeight: Float): Float {
        val sectionHeight = chartHeight / 3f

        return when {
            value < minValid -> {
                val ratio = value / minValid
                sectionHeight * ratio
            }
            value <= maxValid -> {
                val ratio = (value - minValid) / (maxValid - minValid)
                sectionHeight + (sectionHeight * ratio)
            }
            else -> {
                val ratio = ((value - maxValid) / (maxValid)).coerceIn(0f, 1f)
                sectionHeight * 2f + (sectionHeight * ratio)
            }
        }
    }


    fun setManikinType(type: String) {
        if (type.equals("infant",true)) {
            minValid = 30f
            maxValid = 40f
        } else {
            minValid = 50f
            maxValid = 60f
        }
        invalidate()
    }


    fun setTrainingType(type: String) {
        trainingType = type
        invalidate()
    }

    fun setVirtualTimeMode(enabled: Boolean) {
        isVirtualTime = enabled
        virtualStartTime = if (enabled) System.currentTimeMillis() else 0L
        invalidate()
    }

}