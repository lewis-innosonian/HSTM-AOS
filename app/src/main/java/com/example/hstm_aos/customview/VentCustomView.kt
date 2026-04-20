package com.example.hstm_aos.customview

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.hstm_aos.R
import kotlin.math.min

class VentCustomView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp = { value: Float ->
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        )
    }

    private val H_PADDING = dp(0f)


    private val lungBg by lazy {
        AppCompatResources.getDrawable(context, R.mipmap.inno_vent_background)
    }
    private val lungBgOver by lazy {
        AppCompatResources.getDrawable(context, R.mipmap.inno_vent_red_background)
    }

//    private val lungBg: Bitmap? = getBitmapFromDrawable(R.mipmap.inno_vent_background)
    private var trainingType: String = "CPR"
    private var normalRange: IntRange = 400..600
    private var maxValue: Int = normalRange.last + 200
    private var currentValue: Int = 0
    private var markerValue: Int? = null
    private var virtualData = false



    // 페인트
    private val bgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL; isAntiAlias = true }
    private val borderPaint = Paint().apply { color = Color.parseColor("#999999"); style = Paint.Style.STROKE; strokeWidth = dp(1f); isAntiAlias = true }
    private val dashedPaint = Paint().apply { color = Color.parseColor("#999999"); style = Paint.Style.STROKE; strokeWidth = dp(1f); pathEffect = DashPathEffect(floatArrayOf(15f,10f),0f); isAntiAlias = true }
    private val grayPaint = Paint().apply { color = Color.parseColor("#D9D9D9"); style = Paint.Style.FILL; isAntiAlias = true }
    private val virtualPaint = Paint().apply { color = ContextCompat.getColor(context, R.color.virtual_color); style = Paint.Style.FILL; isAntiAlias = true }
    private val greenPaint = Paint().apply { color = Color.parseColor("#56ED89"); style = Paint.Style.FILL; isAntiAlias = true }
    private val redPaint = Paint().apply { color = Color.parseColor("#FB9C9C"); style = Paint.Style.FILL; isAntiAlias = true }
    private val markerPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = dp(5f); isAntiAlias = true }
    private val labelPaint = Paint().apply { color = Color.parseColor("#666666"); textSize = dp(14f); isAntiAlias = true; typeface = ResourcesCompat.getFont(context, R.font.inter_medium) }

    private var showBadge = false
    private var badgeBgColor = Color.parseColor("#F5F5F5")
    private val badgeTextPaint = Paint().apply { color = Color.parseColor("#666666"); textSize = dp(14f); isAntiAlias = true; typeface = ResourcesCompat.getFont(context, R.font.inter_medium) }
    private var badgeIconColorFilter: ColorFilter? = null
    private val badgeIcon: Bitmap? = getBitmapFromDrawable(R.drawable.inno_vent_damaged_icon)

    // 화살표 아이콘
    private var slowIcon: Bitmap? = getBitmapFromDrawable(R.drawable.inno_up_arrow_icon_17)
    private var fastIcon: Bitmap? = getBitmapFromDrawable(R.drawable.inno_down_arrow_icon_17)
    private var slowIconColorFilter: ColorFilter? = null
    private var fastIconColorFilter: ColorFilter? = null

    private val handler = Handler(Looper.getMainLooper())
    private var resetBadgeRunnable: Runnable? = null
    private val VW = 280f
    private val VH = 280f
    private val LINE_TOP = 107f
    private val LINE_BOTTOM = 172.67f

    private val lungPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val size = min(viewWidth, viewHeight) // 내부 요소용 정사각형
        val offsetX = (viewWidth - size) / 2f // 중앙 정렬 X
        val offsetY = (viewHeight - size) / 2f // 중앙 정렬 Y

        val leftPad = dp(0f)
        val rightPad = size - dp(0f)

        // 내부 요소 정사각형 기준 스케일
        val scale = size / VW
        canvas.save()
        canvas.translate(offsetX, offsetY) // 중앙 정렬
        canvas.scale(scale, scale)

        /* ===================== Fill ===================== */
        val bottom = VH
        val part1H = bottom - LINE_BOTTOM
        val part2H = LINE_BOTTOM - LINE_TOP
        val part3H = LINE_TOP

        val part1Range = normalRange.first.toFloat()
        val part2Range = (normalRange.last - normalRange.first).toFloat()
        val part3Range = (maxValue - normalRange.last).toFloat()

        val fillPaint = if (virtualData) virtualPaint else when {
            currentValue >= normalRange.last -> redPaint
            currentValue >= normalRange.first -> greenPaint
            else -> grayPaint
        }

        var remain = currentValue.coerceIn(0, maxValue)
        var top = bottom

        fun drawFill(height: Float, ratio: Float) {
            val h = height * ratio
            top -= h
            canvas.drawRect(leftPad / scale, top, rightPad / scale, top + h, fillPaint)
        }

        if (remain > 0) {
            val v = remain.coerceAtMost(part1Range.toInt())
            drawFill(part1H, v / part1Range)
            remain -= v
        }
        if (remain > 0) {
            val v = remain.coerceAtMost(part2Range.toInt())
            drawFill(part2H, v / part2Range)
            remain -= v
        }
        if (remain > 0) {
            val v = remain.coerceAtMost(part3Range.toInt())
            drawFill(part3H, v / part3Range)
        }

        /* ===================== Marker ===================== */
        markerValue?.let { value ->
            val y = when {
                value <= normalRange.first ->
                    bottom - (value / part1Range) * part1H
                value <= normalRange.last ->
                    bottom - part1H - ((value - normalRange.first) / part2Range) * part2H
                else ->
                    bottom - part1H - part2H - ((value - normalRange.last) / part3Range) * part3H
            }
            canvas.drawLine(leftPad / scale, y, rightPad / scale, y, markerPaint)
        }

        /* ---- background ---- */
        val rect = RectF(leftPad / scale, 0f, rightPad / scale, VH)

        val alpha =
            if (trainingType.lowercase().contains("cco"))
                (255 * 0.16f).toInt()
            else
                255

        val save = canvas.saveLayerAlpha(rect, alpha)

        val currentBg = if (currentValue > normalRange.last) {
            AppCompatResources.getDrawable(context, R.mipmap.inno_vent_red_background)
        } else {
            lungBg
        }

        currentBg?.let { drawable ->
            drawable.bounds = Rect(
                rect.left.toInt(),
                rect.top.toInt(),
                rect.right.toInt(),
                rect.bottom.toInt()
            )
            drawable.draw(canvas)
        }

        canvas.restoreToCount(save)

        canvas.restore() // scale + translate 해제

        // ---- border 꽉 채우기 (뷰 전체 기준) ----
        val radius = dp(20f)
        val halfStroke = borderPaint.strokeWidth / 2f
        val borderRect = RectF(
            halfStroke,
            halfStroke,
            viewWidth - halfStroke,
            viewHeight - halfStroke
        )
        canvas.drawRoundRect(borderRect, radius, radius, borderPaint)

        drawBadge(canvas)
    }







    private fun drawLabels(canvas: Canvas) {
        val iconSize = 16f
        val padding = 10f
        val gap = 6f

        slowIcon?.let {
            val y = LINE_TOP - 20f
            canvas.drawBitmap(it, null,
                RectF(padding, y - iconSize / 2, padding + iconSize, y + iconSize / 2),
                null
            )
            canvas.drawText("Too Much", padding + iconSize + gap, y + 5f, labelPaint)
        }

        fastIcon?.let {
            val y = LINE_BOTTOM + 20f
            canvas.drawBitmap(it, null,
                RectF(padding, y - iconSize / 2, padding + iconSize, y + iconSize / 2),
                null
            )
            canvas.drawText("Too Little", padding + iconSize + gap, y + 5f, labelPaint)
        }
    }

    private fun drawSpeedLabels(canvas: Canvas) {
        val iconSize = dp(16f)
        val padding = dp(8f)
        val gap = dp(6f)
        val offset = dp(16f) // 점선과 겹치지 않게 약간 이동

        // 1/3, 2/3 점선 위치
        val b1 = height / 3f
        val b2 = height * 2 / 3f

        // Slow (Too Much) - 아이콘 중심을 점선보다 위로 offset
        slowIcon?.let {
            val icon = Bitmap.createScaledBitmap(it, iconSize.toInt(), iconSize.toInt(), true)
            val iconCenterY = b1 - offset
            val iconTop = iconCenterY - icon.height / 2f
            canvas.drawBitmap(icon, padding, iconTop, Paint().apply { isAntiAlias = true; colorFilter = slowIconColorFilter })

            val textY = iconTop + icon.height / 2f - (labelPaint.ascent() + labelPaint.descent()) / 2
            canvas.drawText("Too Much", padding + icon.width + gap, textY, labelPaint)
        }

        // Fast (Too Little) - 아이콘 중심을 점선보다 아래로 offset
        fastIcon?.let {
            val icon = Bitmap.createScaledBitmap(it, iconSize.toInt(), iconSize.toInt(), true)
            val iconCenterY = b2 + offset
            val iconTop = iconCenterY - icon.height / 2f
            canvas.drawBitmap(icon, padding, iconTop, Paint().apply { isAntiAlias = true; colorFilter = fastIconColorFilter })

            val textY = iconTop + icon.height / 2f - (labelPaint.ascent() + labelPaint.descent()) / 2
            canvas.drawText("Too Little", padding + icon.width + gap, textY, labelPaint)
        }
    }


    fun setVirtualData(isVirtual: Boolean) { virtualData = isVirtual; invalidate() }
    fun setNormalRange(range: IntRange) { normalRange = range; maxValue = range.last+200; invalidate() }
    fun addValue(value: Int) { currentValue = value.coerceIn(0,maxValue); invalidate() }
    fun markValue(value: Int?) { markerValue = value?.coerceIn(0,maxValue); invalidate() }
    fun setBadgeVisible(visible: Boolean) { showBadge = false; invalidate() }

    fun highlightBadgeRed(durationMs: Long = 500L) {
        showBadge = true

        badgeBgColor = Color.parseColor("#FD1708")
        badgeTextPaint.color = Color.WHITE
        badgeIconColorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        invalidate()

        // 기존 타이머 제거
        resetBadgeRunnable?.let { handler.removeCallbacks(it) }

        resetBadgeRunnable = Runnable {
            badgeBgColor = Color.parseColor("#F5F5F5")
            badgeTextPaint.color = Color.parseColor("#666666")
            badgeIconColorFilter = null
            showBadge = false
            invalidate()
        }

        handler.postDelayed(resetBadgeRunnable!!, durationMs)
    }

    fun setTrainingType(type: String) {
        trainingType = type
        applyCcoGray()
        invalidate()
    }

    /** CCO 모드 시 전체 회색 처리 + 화살표까지 적용 */
    private fun applyCcoGray() {
        if(trainingType.lowercase().contains("cco")) {
            val gray = Color.parseColor("#EEEEEE")
            borderPaint.color = gray
            dashedPaint.color = gray
            grayPaint.color = gray
            greenPaint.color = gray
            redPaint.color = gray
            markerPaint.color = gray
            labelPaint.color = gray
            badgeTextPaint.color = gray
            slowIconColorFilter = PorterDuffColorFilter(gray, PorterDuff.Mode.SRC_IN)
            fastIconColorFilter = PorterDuffColorFilter(gray, PorterDuff.Mode.SRC_IN)
            badgeIconColorFilter = PorterDuffColorFilter(gray, PorterDuff.Mode.SRC_IN)
            lungPaint.colorFilter = null
//            lungPaint.colorFilter = PorterDuffColorFilter(gray, PorterDuff.Mode.SRC_IN)
            lungPaint.alpha = (255 * 1f).toInt()
        } else {
            borderPaint.color = Color.parseColor("#999999")
            dashedPaint.color = Color.parseColor("#999999")
            grayPaint.color = Color.parseColor("#D9D9D9")
            greenPaint.color = Color.parseColor("#56ED89")
            redPaint.color = Color.parseColor("#FB9C9C")
            markerPaint.color = Color.parseColor("#0061F2")
            labelPaint.color = Color.parseColor("#666666")
            badgeTextPaint.color = Color.parseColor("#666666")
            slowIconColorFilter = null
            fastIconColorFilter = null
            badgeIconColorFilter = null
            lungPaint.colorFilter = null
        }
    }

    private fun drawBadge(canvas: Canvas) {
        if (!showBadge) return

        val marginBottom = dp(20f)
        val paddingH = dp(14f)
        val paddingV = dp(6f)
        val radius = dp(50f)
        val iconSize = dp(20f)
        val iconGap = dp(6f)
        val label = context.getString(R.string.damaged)

        val textBounds = Rect()
        badgeTextPaint.getTextBounds(label, 0, label.length, textBounds)
        val textW = textBounds.width().toFloat()

        val badgeW = iconSize + iconGap + textW + paddingH * 2
        val badgeH = maxOf(iconSize, textBounds.height().toFloat()) + paddingV * 2

        // ⭐ 중앙 정렬 X
        val left = (width - badgeW) / 2f


        val liftUp = dp(20f)   // ↑ 이 값으로 위치 미세조정

        val top = height - marginBottom - badgeH - liftUp


        val badgeRect = RectF(left, top, left + badgeW, top + badgeH)

        canvas.drawRoundRect(
            badgeRect,
            radius,
            radius,
            Paint().apply {
                color = badgeBgColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }
        )

        badgeIcon?.let {
            val iconLeft = left + paddingH
            val iconTop = top + (badgeH - iconSize) / 2f

            val icon = Bitmap.createScaledBitmap(
                it,
                iconSize.toInt(),
                iconSize.toInt(),
                true
            )

            val iconPaint = Paint().apply {
                isAntiAlias = true
                colorFilter = badgeIconColorFilter
            }

            canvas.drawBitmap(icon, iconLeft, iconTop, iconPaint)

            val textX = iconLeft + iconSize + iconGap
            val textY = top + badgeH / 2f -
                    (badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2

            canvas.drawText(label, textX, textY, badgeTextPaint)
        }
    }


    private fun getBitmapFromDrawable(resId: Int): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context,resId) ?: return null
        val bmp = Bitmap.createBitmap(drawable.intrinsicWidth,drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0,0,canvas.width,canvas.height)
        drawable.draw(canvas)
        return bmp
    }
}
