package com.example.hstm_aos.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.hstm_aos.R

class CustomBarChartView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34C85A")
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val score80LinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333") // 원하는 색상
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val score80TextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#000000") // 점선 색이랑 맞춰도 좋음
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            20f,
            resources.displayMetrics
        )
        textAlign = Paint.Align.LEFT
        typeface = ResourcesCompat.getFont(context, R.font.montserrat_bold)
    }

    val margin10dp = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        10f,
        resources.displayMetrics
    )


    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        textSize =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, resources.displayMetrics)
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.font_600)
    }

    enum class MetricType {
        RESCUE_VENT_VOL,
        RESCUE_VENT_COUNT,
        RESCUE_VENT_SPEED,
        COMP_DEPTH,
        COMP_RELEASE,
        COMP_RATE,
        HAND_POS,
        COMP_FRACTION,
        COMP_COUNT,
        VENT_VOL,
        VENT_COUNT,
        VENT_RATE,
        VENT_SPEED,
        AED_SCORE
    }

    private var labelKeys = listOf<MetricType>()
    private var labels = arrayOf<String>()
    private var values = listOf<Float?>()
    private var viewWidth = 0
    private var viewHeight = 0
    private var barWidth = 0f
    private var type: String = "default"
    private var trainingType: String = "cpr"
    private var guideLine: String = "cpr"


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        barWidth =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40f, resources.displayMetrics)
    }

    fun setType(type: String, trainingType: String, guideLine: String, includeAed: Boolean = false) {
        this.type = type
        this.trainingType = trainingType.lowercase()
        this.guideLine = guideLine.lowercase()
        val keys = mutableListOf<MetricType>()

        // 기존 로직 그대로
        if (guideLine.contains("ERC", true) && !type.equals("Adult", true) && trainingType.equals("CPR Training", true)) {
            keys.add(MetricType.RESCUE_VENT_VOL)
            keys.add(MetricType.RESCUE_VENT_COUNT)
            if (type.equals("baby", true) || type.equals("infant", true)) {
                keys.add(MetricType.RESCUE_VENT_SPEED)
            }
        }

        if (!trainingType.equals("ventilation only", true)) {
            keys.add(MetricType.COMP_DEPTH)
            keys.add(MetricType.COMP_RELEASE)
            keys.add(MetricType.COMP_RATE)
            keys.add(MetricType.HAND_POS)
            keys.add(MetricType.COMP_FRACTION)
            if (trainingType.equals("cpr training", true)) {
                keys.add(MetricType.COMP_COUNT)
            }
        }

        if (!trainingType.equals("chest compression only", true)) {
            keys.add(MetricType.VENT_VOL)
            keys.add(if (trainingType.equals("ventilation only", true)) MetricType.VENT_RATE else MetricType.VENT_COUNT)
            if (type.equals("baby", true) || type.equals("infant", true)) {
                keys.add(MetricType.VENT_SPEED)
            }
        }

        // AED 점수 포함 옵션
        if (includeAed) {
            keys.add(MetricType.AED_SCORE)
        }

        labelKeys = keys
        labels = keys.map { getLabelTextForKey(it) }.toTypedArray()

        // values 조정
        values = values.toMutableList().apply {
            while (size < labels.size) add(null)
            if (size > labels.size) subList(labels.size, size).clear()
        }

        invalidate()
    }


    private fun getLabelTextForKey(key: MetricType): String {
        return when (key) {
            MetricType.AED_SCORE -> "AED\nTrainer"
            // 기존 MetricType 처리 그대로...
            MetricType.RESCUE_VENT_VOL -> context.getString(R.string.rescue_vent_volume)
            MetricType.RESCUE_VENT_COUNT -> context.getString(R.string.rescue_vent_count)
            MetricType.RESCUE_VENT_SPEED -> context.getString(R.string.rescue_vent_speed)
            MetricType.COMP_DEPTH -> context.getString(R.string.result_compression_depth)
            MetricType.COMP_RELEASE -> context.getString(R.string.result_compression_release)
            MetricType.COMP_RATE -> context.getString(R.string.result_compression_rate)
            MetricType.HAND_POS -> if (type == "baby" || type == "infant") context.getString(R.string.result_finger_position)
            else context.getString(R.string.result_hand_position)
            MetricType.COMP_FRACTION -> context.getString(R.string.result_compression_fraction)
            MetricType.COMP_COUNT -> context.getString(R.string.result_compression_count)
            MetricType.VENT_VOL -> context.getString(R.string.result_ventilation_volume)
            MetricType.VENT_COUNT -> context.getString(R.string.result_ventilation_count)
            MetricType.VENT_RATE -> context.getString(R.string.result_ventilation_rate)
            MetricType.VENT_SPEED -> context.getString(R.string.result_ventilation_speed)
            MetricType.RESCUE_VENT_VOL -> context.getString(R.string.rescue_vent_volume)
            MetricType.RESCUE_VENT_COUNT -> context.getString(R.string.rescue_vent_count)
            MetricType.RESCUE_VENT_SPEED -> context.getString(R.string.rescue_vent_speed)
        }
    }


    fun getLabelKeys(): List<MetricType> = labelKeys

    fun setChartData(data: List<Float?>) {
        values = data.toMutableList().apply {
            while (size < labelKeys.size) add(null)
            if (size > labelKeys.size) subList(labelKeys.size, size).clear()
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (viewWidth == 0 || viewHeight == 0 || values.isEmpty()) return

        val paddingTop = 0
        val paddingBottom = 74
        val baseY = viewHeight - paddingBottom

        val maxValue = 100f
        val unitHeight = (viewHeight - (paddingTop + paddingBottom)) / maxValue

        val score80 = 84f
        val score80Y = baseY - (score80 * unitHeight)



        val margin10dp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            10f,
            resources.displayMetrics
        )

        val textX = margin10dp
        val textY = score80Y - (score80TextPaint.ascent() + score80TextPaint.descent()) / 2

        canvas.drawText("84", textX, textY, score80TextPaint)

        val thresholdY = baseY - (maxValue * unitHeight)

        val sidePadding = barWidth / 2f
        val availableWidth = viewWidth - sidePadding * 2
        val slotCount = values.size
        val slotWidth = availableWidth / slotCount

        for (i in values.indices) {
            val value = values[i] ?: 0f

            val centerX = sidePadding + i * slotWidth + slotWidth / 2f

            val left = centerX - barWidth / 2f
            val top = baseY - (value * unitHeight)
            val right = centerX + barWidth / 2f
            val bottom = baseY.toFloat()

            canvas.drawRect(left, top, right, bottom, barPaint)

            val textX = centerX
            val textY = baseY + labelPaint.textSize + 8f

            drawMultilineText(canvas, labels[i], textX, textY, labelPaint, barWidth - 20f)
        }
        linePaint.isDither = false

        val halfStroke = linePaint.strokeWidth / 2f

        canvas.drawLine(
            0f,
            alignY(thresholdY + halfStroke),
            viewWidth.toFloat(),
            alignY(thresholdY + halfStroke),
            linePaint
        )

        canvas.drawLine(
            0f,
            alignY(baseY.toFloat()),
            viewWidth.toFloat(),
            alignY(baseY.toFloat()),
            linePaint
        )

        canvas.drawLine(
            0f,
            score80Y,
            viewWidth.toFloat(),
            score80Y,
            score80LinePaint
        )
    }
    private fun alignY(y: Float): Float {
        return kotlin.math.floor(y) + 0.5f
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        maxWidth: Float
    ) {
        val textLines = text.split("\n")

        var textY = y
        for (line in textLines) {
            canvas.drawText(line, x, textY, paint)
            textY += paint.textSize + 5
        }
    }
}
