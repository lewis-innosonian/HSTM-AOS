package com.example.hstm_aos.customview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.hstm_aos.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class GradientFanView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var trainingType: String = "CPR"

    fun setTrainingType(type: String) {
        trainingType = type
        invalidate()
    }

    private fun isVentMode(): Boolean {
        return trainingType.contains("vent", true)
    }

    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val fanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#D9D9D9")
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#999999")
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
    }

    private var targetBarValue: Int = 0
    private var radius = 0f
    private var centerRadius = 0f
    private var innerRadius = 0f

    var barValue: Int = 0
        private set

    private var barAnimator: ValueAnimator? = null
    var normalRange: IntRange = 100..200

    private var virtualData: Boolean = false
    fun setVirtualData(isVirtual: Boolean) {
        virtualData = isVirtual
        invalidate()
    }

    private fun loadVectorAsBitmap(
        resourceId: Int,
        sizeDp: Float,
        gray: Boolean
    ): Bitmap? {

        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()
        val drawable = ContextCompat.getDrawable(context, resourceId) ?: return null

        if (gray) {
            drawable.colorFilter =
                PorterDuffColorFilter(
                    Color.parseColor("#EEEEEE"),
                    PorterDuff.Mode.SRC_IN
                )
        } else {
            drawable.colorFilter = null
        }

        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bmp
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)

        radius = size / 2f - borderPaint.strokeWidth / 2f
        innerRadius = radius
        centerRadius = radius / 6.25f
    }

    override fun onDraw(canvas: Canvas) {

        val cx = width / 2f
        val cy = height / 2f

        val isVent = isVentMode()
        val hasValue = barValue > 0
        val isNormal = barValue in normalRange

        val mainColor = when {
            isVent -> Color.parseColor("#EEEEEE")
            !hasValue -> Color.parseColor("#D9D9D9")
            isNormal -> Color.parseColor("#56ED89")
            else -> Color.parseColor("#EE6B6B")
        }

        centerPaint.color = if (virtualData) {
            Color.parseColor("#D9D9D9")
        } else {
            mainColor
        }

        borderPaint.color =
            if (isVent) Color.parseColor("#EEEEEE")
            else Color.parseColor("#999999")

        barPaint.color = when {
            isVent || !hasValue -> Color.TRANSPARENT
            virtualData -> Color.parseColor("#D9D9D9")
            else -> mainColor
        }

        canvas.drawCircle(cx, cy, innerRadius, outerPaint)

        val borderStartAngle = 120f
        val borderSweepAngle = 300f

        val normalStartFraction = normalRange.first / 300f
        val normalEndFraction = normalRange.last / 300f

        val normalStartAngle =
            borderStartAngle + borderSweepAngle * normalStartFraction

        val normalSweepAngle =
            borderSweepAngle * (normalEndFraction - normalStartFraction)

        val path = Path()
        path.moveTo(cx, cy)

        val steps = 100
        for (i in 0..steps) {

            val angleDeg =
                normalStartAngle + normalSweepAngle * i / steps

            val angleRad = Math.toRadians(angleDeg.toDouble())

            val x = cx + innerRadius * cos(angleRad).toFloat()
            val y = cy + innerRadius * sin(angleRad).toFloat()

            path.lineTo(x, y)
        }
        path.close()

        val fanEndColor =
            if (isVent)
                Color.parseColor("#EEEEEE")
            else
                Color.parseColor("#D9D9D9")

        val shader = RadialGradient(
            cx, cy, innerRadius,
            intArrayOf(
                Color.parseColor("#00FFFFFF"),
                fanEndColor
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        fanPaint.shader = shader
        canvas.drawPath(path, fanPaint)

        canvas.drawCircle(cx, cy, centerRadius, centerPaint)

        val rect = RectF(
            cx - innerRadius,
            cy - innerRadius,
            cx + innerRadius,
            cy + innerRadius
        )

        canvas.drawArc(
            rect,
            borderStartAngle,
            borderSweepAngle,
            false,
            borderPaint
        )

        if (hasValue && !isVent) {

            val fraction = barValue / 300f
            val barDeg =
                borderStartAngle + borderSweepAngle * fraction

            val barRad = Math.toRadians(barDeg.toDouble())

            val maxBarLength =
                (innerRadius - centerRadius) * 2f / 3f

            val endX = cx
            val endY = cy

            val startRadius =
                centerRadius + maxBarLength

            val startX =
                cx + startRadius * cos(barRad).toFloat()

            val startY =
                cy + startRadius * sin(barRad).toFloat()

            canvas.drawLine(
                startX,
                startY,
                endX,
                endY,
                barPaint
            )
        }
        val leftIcon = loadVectorAsBitmap(
            R.drawable.inno_rabbits_icon,
            40f,
            isVent
        )

        val rightIcon = loadVectorAsBitmap(
            R.drawable.inno_turtle_icon,
            40f,
            isVent
        )

        val liftUpPx = 30f * resources.displayMetrics.density   // 바닥에서 더 멀어짐
        val verticalOffsetPx = -30f * resources.displayMetrics.density

        val iconData = listOf(
            Triple(leftIcon,  borderStartAngle + borderSweepAngle - 0f, 20f),
            Triple(rightIcon, borderStartAngle + 0f, 20f)
        )

        for ((bmp, angleDeg, extraOffsetDp) in iconData) {

            bmp?.let {

                val rad =
                    Math.toRadians(angleDeg.toDouble())

                val extraOffsetPx =
                    extraOffsetDp * resources.displayMetrics.density

                val distanceFromCenter =
                    innerRadius - liftUpPx + extraOffsetPx

                val iconX =
                    cx + distanceFromCenter * cos(rad).toFloat()

                val iconY =
                    cy + distanceFromCenter * sin(rad).toFloat() +
                            verticalOffsetPx

                canvas.drawBitmap(
                    it,
                    iconX - it.width / 2f,
                    iconY - it.height / 2f,
                    null
                )
            }
        }

    }

    fun moveBarTo(input: Int) {

        targetBarValue =
            chestCompressionSpeedAdjustValue(input)
                .coerceIn(0, 300)

        barAnimator?.cancel()

        barAnimator =
            ValueAnimator.ofFloat(
                barValue.toFloat(),
                targetBarValue.toFloat()
            ).apply {

                duration = 100L

                addUpdateListener { animation ->
                    val value =
                        animation.animatedValue as Float

                    barValue = value.toInt()
                    invalidate()
                }
                start()
            }
    }

    private fun chestCompressionSpeedAdjustValue(
        input: Int
    ): Int {

        return when (input) {

            in 0..99 ->
                (0 + (input / 99f * 99f)).toInt()

            in 100..120 ->
                (100 + ((input - 100) / 20f * 100f)).toInt()

            in 121..255 ->
                (201 + ((input - 121) / 134f * 99f)).toInt()

            else -> 0
        }
    }
}
