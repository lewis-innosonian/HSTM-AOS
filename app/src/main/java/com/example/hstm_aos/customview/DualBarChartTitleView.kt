package com.example.hstm_aos.customview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.example.hstm_aos.R

class DualBarChartTitleView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    private var trainingType: String = "CPR"
    private val strokeWidth = dpToPx(1f)
    private val cornerRadius = dpToPx(20f)
    private val ventColor = Color.parseColor("#EEEEEE")
    private var tintedTopIcon: Bitmap? = null
    private var tintedBottomIcon: Bitmap? = null
    private val chartPaint = Paint().apply {
        color = Color.parseColor("#999999")
        style = Paint.Style.STROKE
        strokeWidth = this@DualBarChartTitleView.strokeWidth
        isAntiAlias = true
    }

    private val dashedLinePaint = Paint().apply {
        color = Color.parseColor("#999999")
        style = Paint.Style.STROKE
        strokeWidth = this@DualBarChartTitleView.strokeWidth
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val textPaint = Paint().apply {
        color = Color.parseColor("#666666")
        textSize = dpToPx(14f)
        isAntiAlias = true
    }

    private val iconPadding = dpToPx(4f)

    private fun getBitmapFromDrawable(resId: Int): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, resId) ?: return null

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    private var topIcon: Bitmap? =
        getBitmapFromDrawable(R.drawable.inno_up_arrow_icon_17)

    private var bottomIcon: Bitmap? =
        getBitmapFromDrawable(R.drawable.inno_down_arrow_icon_17)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        applyTrainingStyle()

        val stroke = chartPaint.strokeWidth

        val rect = RectF(
            stroke / 2,
            stroke / 2,
            width - stroke / 2,
            height - stroke / 2
        )


        val path = Path().apply {
            addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.drawPath(path, chartPaint)

        val chartHeight = height.toFloat()

        val line1 = chartHeight / 3f
        val line2 = chartHeight * 2f / 3f

        // dashed lines
        canvas.drawLine(0f, line1, width.toFloat(), line1, dashedLinePaint)
        canvas.drawLine(0f, line2, width.toFloat(), line2, dashedLinePaint)

        val baseX = dpToPx(6f)

        // =========================
        // ICON + TEXT (TOP)
        // =========================
        tintedTopIcon?.let { icon ->

            val margin = dpToPx(6f)

            val text = context.getString(R.string.feedback_too_shallow)

            val iconX = dpToPx(6f)

            val textBaseline = line1 - margin

            val iconY = textBaseline - icon.height + dpToPx(4f)

            canvas.drawBitmap(icon, iconX, iconY, null)

            canvas.drawText(
                text,
                iconX + icon.width + iconPadding,
                textBaseline,
                textPaint
            )
        }

        // =========================
        // ICON + TEXT (BOTTOM)
        // =========================
        tintedBottomIcon?.let { icon ->

            val margin = dpToPx(6f)

            val text = context.getString(R.string.feedback_too_deep)

            val iconX = dpToPx(6f)

            val textBaseline = line2 + margin + textPaint.textSize

            val iconY = textBaseline - icon.height + dpToPx(4f)

            canvas.drawBitmap(icon, iconX, iconY, null)

            canvas.drawText(
                text,
                iconX + icon.width + iconPadding,
                textBaseline,
                textPaint
            )
        }
    }

    private fun applyTrainingStyle() {
        if (trainingType.equals("VENT", true)) {
            chartPaint.color = ventColor
            dashedLinePaint.color = ventColor
            textPaint.color = ventColor
        } else {
            chartPaint.color = Color.parseColor("#999999")
            dashedLinePaint.color = Color.parseColor("#999999")
            textPaint.color = Color.parseColor("#666666")
        }
    }

    private fun getCenteredTextBaseline(centerY: Float): Float {
        val fm = textPaint.fontMetrics
        return centerY - (fm.ascent + fm.descent) / 2f
    }

    fun setTrainingType(type: String) {
        trainingType = type

        val color = if (trainingType.equals("VENT", true)) {
            Color.parseColor("#EEEEEE")
        } else {
            Color.parseColor("#999999")
        }

        tintedTopIcon = tintBitmap(topIcon, color)
        tintedBottomIcon = tintBitmap(bottomIcon, color)

        invalidate()
    }

    private fun tintBitmap(bitmap: Bitmap?, color: Int): Bitmap? {
        if (bitmap == null) return null

        val tinted = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tinted)

        val paint = Paint()
        paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)

        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return tinted
    }
}