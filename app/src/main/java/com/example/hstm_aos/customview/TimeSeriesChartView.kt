package com.example.hstm_aos.customview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.example.hstm_aos.AedRange
import com.example.hstm_aos.CprData
import com.example.hstm_aos.R
import com.example.hstm_aos.ble.TrainingType
import kotlin.math.max

class TimeSeriesChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val DEPTH_MAX = 80f
        private const val VENT_MAX = 100f
        private const val BAR_WIDTH_DP = 10f
        private const val BAR_GAP_DP = 0f
        private const val START_MARGIN_DP = 10f
        private const val AED_WIDTH_DP = 160f

        private const val PX_PER_SEC = 80f   // ★ 이 값이 클수록 차이 확 남
        private const val MIN_SPACING_DP = 8f

    }
    private val barXList = mutableListOf<Float>()
    private fun getVentMax(): Float = if (isBaby) 100f else 1000f
    private var setTimestamp: Boolean = false
    private val aedXMap = mutableMapOf<AedRange, Pair<Float, Float>>()
    private val basePadding = dp(20f)
    private val timeline = mutableListOf<CprData>()
    private val aedList = mutableListOf<AedRange>()
    private var trainingTypes: ArrayList<TrainingType> = ArrayList()
    private var isBaby: Boolean = true
    var skipSpacingOnce = false

    private val barWidth by lazy { dp(BAR_WIDTH_DP) }
    private val barGap by lazy { dp(BAR_GAP_DP) }
    private val itemWidth by lazy { barWidth + barGap }
    private val startMargin by lazy { dp(START_MARGIN_DP) }
    private val aedWidth by lazy { dp(AED_WIDTH_DP) }

    private val aedGap by lazy { barGap * 0.5f } // ← 여기 조절

    private fun p(c: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c }
    private val green = p(Color.parseColor("#56ED89"))
    private val gray = p(Color.parseColor("#D9D9D9"))
    private val red = p(Color.parseColor("#A52F1D"))
    private val blue = p(Color.parseColor("#618AF5"))
    private val linePaint = p(Color.parseColor("#1AAF0D")).apply { strokeWidth = dp(1f) }
    private val ventLinePaint = p(Color.parseColor("#0061F2")).apply { strokeWidth = dp(1f) }
    private val aedPaint = p(Color.parseColor("#1AFB6B24"))
    private val normalChestBgPaint = p(Color.parseColor("#0D56ED89"))
    private val normalVentBgPaint = p(Color.parseColor("#F2F7FE"))
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#666666")
        strokeWidth = dp(1f)
    }

    private val aedIcon: Bitmap? by lazy {
        vectorToBitmap(R.drawable.inno_aed_t_icon, dp(24f).toInt(), dp(24f).toInt())
    }

    private fun vectorToBitmap(drawableRes: Int, width: Int, height: Int): Bitmap? {
        val drawable: Drawable = ContextCompat.getDrawable(context, drawableRes) ?: return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    fun setTrainingType(types: ArrayList<TrainingType>) {
        trainingTypes = types
        requestLayout()
        invalidate()
    }

    fun setIsBaby(baby: Boolean) {
        isBaby = baby
        invalidate()
    }

    fun setData(cpr: List<CprData>, aed: List<AedRange>) {
        timeline.clear()
        timeline.addAll(cpr.sortedBy { it.timestamp })
        aedList.clear()
        aedList.addAll(aed)
        requestLayout()
        invalidate()
    }

    private fun getItemSpacing(index: Int, data: CprData): Float {
        if (!setTimestamp || index == 0) return itemWidth

        val prev = timeline[index - 1]

        val deltaSec = (data.timestamp - prev.timestamp)
            .toFloat()
            .coerceAtLeast(0.01f)

        val spacing = deltaSec * itemWidth * 2.5f

        // 너무 과하지 않게 상/하한
        return spacing.coerceIn(
            itemWidth * 0.8f,   // 최소 간격도 살짝 넓힘
            itemWidth * 10f     // 최대는 확 벌어지게
        )
    }


    private fun getAedStartIndex(range: AedRange): Int {
        val idx = timeline.indexOfFirst { it.timestamp >= range.start }
        return if (idx == -1) timeline.lastIndex else idx
    }



    fun setUseTimestampSpacing(use: Boolean) {
        setTimestamp = use
        requestLayout()
        invalidate()
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var needWidth = startMargin

        // timeline bar 폭 + spacing
        timeline.forEachIndexed { index, data ->
            val spacing = if (index == 0) 0f else getItemSpacing(index, data)
            needWidth += spacing + barWidth
        }

        // AED 폭 계산 (timestamp 기반)
        aedList.forEach { aed ->
            if (setTimestamp) {
                val left = getXForTimestamp(aed.start)
                val right = getXForTimestamp(aed.end)

                // 최소폭 보장 + 확대
                val minWidth = dp(40f)
                val expandRatio = 1.3f
                needWidth += max(right - left, minWidth) * expandRatio
            } else {
                needWidth += aedWidth
            }
        }

        val finalWidth = max(needWidth.toInt(), MeasureSpec.getSize(widthMeasureSpec))
        val finalHeight = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(finalWidth, finalHeight)
    }

    // 2️⃣ getXForTimestamp: timestamp → px 변환
    private fun getXForTimestamp(ts: Double): Float {
        if (timeline.isEmpty()) return startMargin

        var x = startMargin
        for (i in timeline.indices) {
            if (i == 0) continue
            val prev = timeline[i - 1]
            val cur = timeline[i]
            val spacing = getItemSpacing(i, cur)
            x += spacing
            if (cur.timestamp >= ts) {
                // timestamp 위치 계산 (선형 보간)
                val ratio = ((ts - prev.timestamp) / (cur.timestamp - prev.timestamp)).toFloat()
                return x - spacing + spacing * ratio
            }
        }
        // 마지막 timestamp 이후면 마지막 위치
        return x
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        barXList.clear()
        aedXMap.clear()

        val isCCO = trainingTypes.contains(TrainingType.CCO)
        val isVO = trainingTypes.contains(TrainingType.VO) && !trainingTypes.contains(TrainingType.CPR)
        val showChest = trainingTypes.contains(TrainingType.CPR) || isCCO
        val showVent = trainingTypes.contains(TrainingType.CPR) || isVO

        canvas.save()
        canvas.translate(0f, basePadding)
        val drawHeight = height - basePadding * 2

        val topHeight = if (isCCO) drawHeight else if (showChest) drawHeight * 0.5f else 0f
        val bottomHeight = if (isVO) drawHeight else if (showVent) drawHeight * 0.5f else 0f
        val bottomStart = drawHeight - bottomHeight

        if (showChest) {
            drawNormalChest(canvas, topHeight)
            drawDepthLines(canvas, topHeight)
        }

        if (showVent && !isCCO) {
            drawNormalVent(canvas, bottomStart, bottomHeight)
            drawVentLines(canvas, bottomStart, bottomHeight)
        }

        drawFrame(canvas, drawHeight, isCCO || isVO)

        var xOffset = startMargin
        var aedIndex = 0
        var skipUntilIndex = -1

        timeline.forEachIndexed { index, data ->

            // AED 블록 안이면 완전 스킵 (공간도 차지 X)
            if (index <= skipUntilIndex) return@forEachIndexed

            // bar spacing (첫 bar 제외)
            if (index > 0) {
                if (!skipSpacingOnce) {
                    xOffset += if (setTimestamp) {
                        getItemSpacing(index, data)
                    } else {
                        itemWidth
                    }
                }
                skipSpacingOnce = false
            }
            // AED 시작 체크
            val currentAed = aedList.getOrNull(aedIndex)
            if (currentAed != null) {
                val s = getAedStartIndex(currentAed)
                val e = timeline.indexOfLast { it.timestamp <= currentAed.end }
                    .takeIf { it != -1 } ?: s

                if (index == s) {
                    val left = xOffset
                    val right = if (setTimestamp) {
                        getXForTimestamp(currentAed.end) // timestamp 기반으로 AED 끝 계산
                    } else {
                        left + aedWidth                  // 고정 폭
                    }

                    aedXMap[currentAed] = left to right

                    if (!setTimestamp) {
                        xOffset = right + dp(MIN_SPACING_DP) // false일 때만 spacing
                    }
                    // setTimestamp=true면 다음 bar 위치는 getXForTimestamp() 계산으로 자동 처리
                }
            }

            // overlapped bar 완전 제거
            if (data.aed_overlapped_type != null) return@forEachIndexed

            // bar 위치 기록
            barXList.add(xOffset)




            // CHEST BAR
            // CHEST BAR
            if (data.actionType == "comp" && showChest) {
                val minDepth = if (isBaby) 30f else 50f
                val maxDepth = if (isBaby) 40f else 60f

                val paint = when {
                    data.is_virtual_action -> gray
                    data.depthMax / 2f < minDepth -> gray
                    data.depthMax / 2f in minDepth..maxDepth -> green
                    else -> red
                }

                val top = (data.depthMin / 2f / DEPTH_MAX) * topHeight
                val bottom = (data.depthMax / 2f / DEPTH_MAX) * topHeight

                if (bottom > top) {
                    val right = xOffset + barWidth
                    canvas.drawRect(xOffset, top, right, bottom, paint)
                    barXList.add(xOffset)
                    xOffset += barWidth  // ← 여기가 핵심
                }
            }


            // VENT BAR
            if (data.actionType == "vent" && showVent && !isCCO) {
                val minVent = if (isBaby) 20f else 400f
                val maxVent = if (isBaby) 40f else 700f
                val ventMaxValue = if (isBaby) 100f else 1000f

                val rawH = (data.ventMax / ventMaxValue * bottomHeight).coerceAtMost(bottomHeight)
                val h = max(rawH, 4f)


                val paint = when {
                    data.is_virtual_action -> gray
                    data.ventMax  < minVent -> gray
                    data.ventMax.toFloat() in minVent..maxVent -> blue
                    else -> red
                }

//                val paint = if (data.is_virtual_action) gray
//                else if (data.ventMax.toFloat() in minVent..maxVent) blue
//                else red

                val right = xOffset + barWidth
                canvas.drawRect(xOffset, bottomStart + bottomHeight - h, right, bottomStart + bottomHeight - ventLinePaint.strokeWidth, paint)
                barXList.add(xOffset)
                xOffset += barWidth
            }



        }

        if (trainingTypes.contains(TrainingType.AED)) {
            drawAed(canvas, drawHeight)
        }

        canvas.restore()
    }

    private fun drawNormalChest(c: Canvas, topH: Float) {
        val minDepth = if (isBaby) 30f else 50f
        val maxDepth = if (isBaby) 40f else 60f
        val yMin = minDepth / DEPTH_MAX * topH + linePaint.strokeWidth / 2f
        val yMax = maxDepth / DEPTH_MAX * topH + linePaint.strokeWidth / 2f
        c.drawRect(0f, yMin, width.toFloat(), yMax, normalChestBgPaint)
    }

    private fun drawNormalVent(c: Canvas, bottomStart: Float, bottomH: Float) {
        val minVent = if (isBaby) 20f else 400f
        val maxVent = if (isBaby) 40f else 700f
        val ventMaxValue = if (isBaby) 100f else 1000f

        val yMin = bottomStart + bottomH - bottomH * maxVent / ventMaxValue - ventLinePaint.strokeWidth / 2f
        val yMax = bottomStart + bottomH - bottomH * minVent / ventMaxValue - ventLinePaint.strokeWidth / 2f
        c.drawRect(0f, yMin, width.toFloat(), yMax, normalVentBgPaint)
    }

    private fun drawDepthLines(c: Canvas, h: Float) {
        val minDepth = if (isBaby) 30f else 50f
        val maxDepth = if (isBaby) 40f else 60f
        val yMin = h * (minDepth / DEPTH_MAX) + linePaint.strokeWidth / 2f
        val yMax = h * (maxDepth / DEPTH_MAX) + linePaint.strokeWidth / 2f
        c.drawLine(0f, yMin, width.toFloat(), yMin, linePaint)
        c.drawLine(0f, yMax, width.toFloat(), yMax, linePaint)
    }

    private fun drawVentLines(c: Canvas, start: Float, h: Float) {
        val minVent = if (isBaby) 20f else 400f
        val maxVent = if (isBaby) 40f else 700f

        val ventMaxValue = if (isBaby) 100f else 1000f

        val yMin = start + h - h * maxVent / ventMaxValue - ventLinePaint.strokeWidth / 2f
        val yMax = start + h - h * minVent / ventMaxValue - ventLinePaint.strokeWidth / 2f
        c.drawLine(0f, yMin, width.toFloat(), yMin, ventLinePaint)
        c.drawLine(0f, yMax, width.toFloat(), yMax, ventLinePaint)
    }

    private fun drawFrame(c: Canvas, h: Float, isCCO: Boolean) {
        val half = borderPaint.strokeWidth / 2f
        val top = half
        val bottom = h - half
        val middle = if (isCCO) bottom else h * 0.45f
        val startBottom = h * 0.55f
        val left = half
        val right = width - half

        c.drawLine(left, top, right, top, borderPaint)
        c.drawLine(left, top, left, middle, borderPaint)
        c.drawLine(right, top, right, middle, borderPaint)

        if (isCCO) return

        c.drawLine(left, startBottom, left, bottom, borderPaint)
        c.drawLine(right, startBottom, right, bottom, borderPaint)
        c.drawLine(left, bottom, right, bottom, borderPaint)
    }

    private fun drawAed(c: Canvas, h: Float) {
        aedXMap.forEach { (_, pos) ->
            val (left, right) = pos
            val cx = (left + right) / 2f
            val cy = h / 2f

            c.drawRect(left, 0f, right, h, aedPaint)

            aedIcon?.let {
                val dst = RectF(
                    cx - it.width / 2f,
                    cy - it.height / 2f,
                    cx + it.width / 2f,
                    cy + it.height / 2f
                )
                c.drawBitmap(it, null, dst, null)
            }
        }
    }

    private fun dp(v: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
}


class TimeSeriesGuideView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val DEPTH_MAX = 80f
        private const val VENT_MAX = 100f
    }

    private val basePadding = dp(20f)
    private val iconSize = dp(16f)
    private val iconGap = dp(4f)
    private var trainingTypes: ArrayList<TrainingType> = ArrayList()
    private var isBaby: Boolean = true

    private val inter500: Typeface? = try {
        Typeface.createFromAsset(context.assets, "fonts/Inter-Medium.ttf")
    } catch (e: Exception) {
        Typeface.DEFAULT
    }

    private val textPaintChest = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34C759")
        textSize = sp(14f)
        typeface = inter500
        textAlign = Paint.Align.LEFT
    }

    private val textPaintVent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B82F6")
        textSize = sp(14f)
        typeface = inter500
        textAlign = Paint.Align.LEFT
    }

    private val lineStrokeWidth = dp(1f)
    private val compIcon: Bitmap? by lazy {
        vectorToBitmap(R.drawable.inno_timeseries_comp_icon, iconSize.toInt(), iconSize.toInt())
    }
    private val ventIcon: Bitmap? by lazy {
        vectorToBitmap(R.drawable.inno_timeseries_vent_icon, iconSize.toInt(), iconSize.toInt())
    }

    fun setTrainingType(types: ArrayList<TrainingType>, baby: Boolean = true) {
        trainingTypes = types
        isBaby = baby
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val isCCO = trainingTypes.contains(TrainingType.CCO)
        val isVO = trainingTypes.contains(TrainingType.VO) && !trainingTypes.contains(TrainingType.CPR)
        val showChest = trainingTypes.contains(TrainingType.CPR) || isCCO
        val showVent = trainingTypes.contains(TrainingType.CPR) || isVO

        val chartHeight = when {
            trainingTypes.contains(TrainingType.CPR) -> dp(400f)
            isCCO || isVO -> dp(200f)
            else -> dp(200f)
        }

        var desiredWidth = 0f
        if (showChest) {
            desiredWidth = maxOf(
                textPaintChest.measureText(if (isBaby) "40mm" else "60mm"),
                textPaintChest.measureText(if (isBaby) "30mm" else "50mm"),
                textPaintChest.measureText("0mm")
            )
        }
        if (showVent) {
            val ventWidth = maxOf(
                textPaintVent.measureText(if (isBaby) "40ml" else "700ml"),
                textPaintVent.measureText(if (isBaby) "20ml" else "400ml"),
                textPaintVent.measureText("0ml")
            )
            desiredWidth = maxOf(desiredWidth, ventWidth)
        }
        desiredWidth += iconSize + iconGap + dp(12f)

        setMeasuredDimension(
            resolveSize(desiredWidth.toInt(), widthMeasureSpec),
            resolveSize((chartHeight + basePadding * 2).toInt(), heightMeasureSpec)
        )
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        val isCCO = trainingTypes.contains(TrainingType.CCO)
        val isVO = trainingTypes.contains(TrainingType.VO) && !trainingTypes.contains(TrainingType.CPR)
        val showChest = trainingTypes.contains(TrainingType.CPR) || isCCO
        val showVent = trainingTypes.contains(TrainingType.CPR) || isVO

        c.save()
        c.translate(0f, basePadding)
        val drawHeight = height - basePadding * 2

        val topH = when {
            trainingTypes.contains(TrainingType.CPR) -> drawHeight / 2f
            isCCO -> drawHeight
            else -> 0f
        }
        val bottomH = when {
            trainingTypes.contains(TrainingType.CPR) -> drawHeight / 2f
            isVO -> drawHeight
            else -> 0f
        }
        val bottomStart = drawHeight - bottomH

        if (showChest) {
            val maxDepth = if (isBaby) 40f else 60f
            val minDepth = if (isBaby) 30f else 50f
            val yMaxLine = topH * maxDepth / DEPTH_MAX + lineStrokeWidth / 2f
            val yMinLine = topH * minDepth / DEPTH_MAX + lineStrokeWidth / 2f
            val yZeroLine = lineStrokeWidth / 2f

            drawIconText(c, compIcon, "${maxDepth.toInt()}mm", centerYToBaseline(yMaxLine, textPaintChest), textPaintChest)
            drawIconText(c, compIcon, "${minDepth.toInt()}mm", centerYToBaseline(yMinLine, textPaintChest), textPaintChest)
            drawIconText(c, compIcon, "0mm", centerYToBaseline(yZeroLine, textPaintChest), textPaintChest)
        }

        if (showVent && !isCCO) {
            val maxVent = if (isBaby) 40f else 700f
            val minVent = if (isBaby) 20f else 400f
            val ventBase = if (isVO) 0f else bottomStart

            val ventMaxValue = if (isBaby) 100f else 1000f

            // Chest처럼 비율 계산
            val yMaxLine = ventBase + bottomH * (1 - maxVent / ventMaxValue) + lineStrokeWidth / 2f
            val yMinLine = ventBase + bottomH * (1 - minVent / ventMaxValue) + lineStrokeWidth / 2f
            val yZeroLine = ventBase + bottomH - lineStrokeWidth / 2f

            drawIconText(c, ventIcon, "${maxVent.toInt()}ml", centerYToBaseline(yMaxLine, textPaintVent), textPaintVent)
            drawIconText(c, ventIcon, "${minVent.toInt()}ml", centerYToBaseline(yMinLine, textPaintVent), textPaintVent)
            drawIconText(c, ventIcon, "0ml", centerYToBaseline(yZeroLine, textPaintVent), textPaintVent)
        }



        c.restore()
    }

    private fun centerYToBaseline(centerY: Float, paint: Paint): Float {
        val fm = paint.fontMetrics
        return centerY - (fm.ascent + fm.descent) / 2f
    }

    private fun drawIconText(c: Canvas, icon: Bitmap?, text: String, baselineY: Float, paint: Paint) {
        val fm = paint.fontMetrics
        val textCenterY = baselineY + (fm.ascent + fm.descent) / 2f
        val iconX = dp(4f)
        val iconY = textCenterY - iconSize / 2f
        icon?.let { c.drawBitmap(it, iconX, iconY, null) }
        val textX = iconX + iconSize + iconGap
        c.drawText(text, textX, baselineY, paint)
    }

    private fun dp(v: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
    private fun sp(v: Float): Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    private fun vectorToBitmap(res: Int, w: Int, h: Int): Bitmap? {
        val d: Drawable = ContextCompat.getDrawable(context, res) ?: return null
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        d.setBounds(0, 0, w, h)
        d.draw(c)
        return b
    }
}
