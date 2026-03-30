package com.example.hstm_aos.customview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import com.example.hstm_aos.R
import java.util.Locale

class DepthDashedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var trainingType: String = "CPR"
    private var virtualData = false
    private val backgroundColor = Color.WHITE

    /* ================= Paint ================= */

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#999999")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        isAntiAlias = true
    }

    private val dashedPaint = Paint().apply {
        color = Color.parseColor("#999999")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
        isAntiAlias = true
    }

    private val grayPaint = Paint().apply {
        color = Color.parseColor("#D9D9D9")   // ⭐ virtual 색
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val greenPaint = Paint().apply {
        color = Color.parseColor("#56ED89")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val redPaint = Paint().apply {
        color = Color.parseColor("#FB9C9C")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val markerPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(5f)
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#666666")
        textSize = dpToPx(14f)
        isAntiAlias = true
        typeface = getLocalizedFont()
    }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = true
    }

    /* ================= Data ================= */

    private var labelIcon: Bitmap? = null
    private var labelIcon2: Bitmap? = null

    private var currentValue: Int = 0
    private var markerValue: Int? = null

    private var normalMin = 50
    private var normalMax = 60
    private var overRange = 20

    /* ================= Public API ================= */

    fun setNormalRange(min: Int, max: Int, over: Int = 20) {
        normalMin = min
        normalMax = max
        overRange = normalMax + 20
        invalidate()
    }

    fun addValue(value: Int) {
        currentValue = value.coerceAtLeast(0)
        invalidate()
    }

    fun markValue(value: Int?) {
        markerValue = value?.coerceAtLeast(0)
        invalidate()
    }

    fun setTrainingType(type: String) {
        trainingType = type
        applyVentStyle()
        clearIconCache()
        invalidate()
    }

    fun setVirtualData(isVirtual: Boolean) {
        virtualData = isVirtual
        invalidate()
    }

    /* ================= Draw ================= */

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val radius = dpToPx(20f)
        val inset = borderPaint.strokeWidth / 2
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        val clipPath = Path().apply {
            addRoundRect(rect, radius, radius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)

        canvas.drawRoundRect(rect, radius, radius, Paint().apply {
            color = backgroundColor
            style = Paint.Style.FILL
        })

        loadIconsIfNeeded()

        val third = height / 3f
        val t1 = 0f
        val b1 = third
        val t2 = third
        val b2 = third * 2
        val t3 = third * 2
        val b3 = height.toFloat()

        when {
            currentValue > normalMax -> {
                val paint = if (virtualData) grayPaint else redPaint
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }

            currentValue in normalMin..normalMax -> {
                val paint = if (virtualData) grayPaint else greenPaint
                val ratio =
                    (currentValue - normalMin) / (normalMax - normalMin).toFloat()
                val y = b1 + (b2 - b1) * ratio
                canvas.drawRect(0f, 0f, width.toFloat(), y, paint)
            }

            else -> {
                val ratio = currentValue / (normalMin - 1).toFloat()
                val y = t1 + (b1 - t1) * ratio
                canvas.drawRect(0f, 0f, width.toFloat(), y, grayPaint)
            }
        }

        canvas.drawLine(0f, b1, width.toFloat(), b1, dashedPaint)
        canvas.drawLine(0f, b2, width.toFloat(), b2, dashedPaint)

        drawLabel(canvas, labelIcon, "Too Shallow", b1 - dpToPx(18f))
        drawLabel(canvas, labelIcon2, "Too Deep", b2 + dpToPx(18f))

        markerValue?.let {
            drawMarker(canvas, it, t1, b1, t2, b2, t3, b3)
        }

        canvas.restore()

        canvas.drawRoundRect(
            RectF(inset, inset, width - inset, height - inset),
            radius, radius,
            borderPaint
        )
    }

    /* ================= Helpers ================= */

    private fun drawLabel(
        canvas: Canvas,
        icon: Bitmap?,
        text: String,
        centerY: Float
    ) {
        val padding = dpToPx(12f)
        val gap = dpToPx(6f)

        val fm = textPaint.fontMetrics
        val textOffset = (fm.ascent + fm.descent) / 2

        icon?.let {
            canvas.drawBitmap(
                it,
                padding,
                centerY - it.height / 2,
                bitmapPaint
            )
        }

        val isVent = trainingType.contains("vent", true)
        textPaint.color = if (isVent) Color.parseColor("#EEEEEE") else Color.parseColor("#666666")

        canvas.drawText(
            text,
            padding + (icon?.width ?: 0) + gap,
            centerY - textOffset,
            textPaint
        )
    }


    private fun drawMarker(
        canvas: Canvas,
        value: Int,
        t1: Float, b1: Float,
        t2: Float, b2: Float,
        t3: Float, b3: Float
    ) {
        markerPaint.color = if (virtualData) {
            Color.parseColor("#BDBDBD")
        } else when {
            value < normalMin -> Color.GRAY
            value in normalMin..normalMax -> Color.parseColor("#0061F2")
            else -> Color.parseColor("#E300E8")
        }

        val y = when {
            value < normalMin -> {
                val r = value / (normalMin - 1).toFloat()
                t1 + (b1 - t1) * r
            }
            value in normalMin..normalMax -> {
                val r =
                    (value - normalMin) / (normalMax - normalMin).toFloat()
                t2 + (b2 - t2) * r
            }
            else -> {
                val r =
                    ((value - normalMax).coerceAtMost(overRange)) /
                            overRange.toFloat()
                t3 + (b3 - t3) * r
            }
        }

        canvas.drawLine(0f, y, width.toFloat(), y, markerPaint)
    }

    private fun loadIconsIfNeeded() {
        val isVent = trainingType.contains("vent", true)
        val iconColor = if (isVent) Color.parseColor("#EEEEEE") else textPaint.color

        if (labelIcon == null)
            labelIcon = getBitmapSafe(R.drawable.inno_up_arrow_icon_17, iconColor)
        if (labelIcon2 == null)
            labelIcon2 = getBitmapSafe(R.drawable.inno_down_arrow_icon_17, iconColor)
    }

    private fun clearIconCache() {
        labelIcon = null
        labelIcon2 = null
    }

    private fun getBitmapSafe(resId: Int, tint: Int? = null): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, resId) ?: return null
        tint?.let { drawable.setTint(it) }

        val target = dpToPx(16f)
        val dw = drawable.intrinsicWidth
        val dh = drawable.intrinsicHeight
        if (dw <= 0 || dh <= 0) return null

        val ratio = dw.toFloat() / dh.toFloat()
        val w: Int
        val h: Int

        if (dw > dh) {
            w = target.toInt()
            h = (target / ratio).toInt()
        } else {
            h = target.toInt()
            w = (target * ratio).toInt()
        }

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)

        return bitmap
    }

    private fun applyVentStyle() {
        val isVent = trainingType.contains("vent", true)

        if (isVent) {
            val dim = Color.parseColor("#EEEEEE")
            borderPaint.color = dim
            dashedPaint.color = dim
            textPaint.color = Color.parseColor("#666666")
        } else {
            borderPaint.color = Color.parseColor("#999999")
            dashedPaint.color = Color.parseColor("#999999")
            textPaint.color = Color.parseColor("#666666")
        }
    }

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )

    private fun getLocalizedFont(): Typeface? =
        when (Locale.getDefault().language) {
            "ko" -> ResourcesCompat.getFont(context, R.font.pretendard_medium)
            else -> ResourcesCompat.getFont(context, R.font.inter_medium)
        }
}
