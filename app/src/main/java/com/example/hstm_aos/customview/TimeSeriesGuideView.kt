//package com.example.hstm_aos.customview
//
//import android.content.Context
//import android.graphics.*
//import android.graphics.drawable.Drawable
//import android.util.AttributeSet
//import android.util.TypedValue
//import android.view.View
//import androidx.core.content.ContextCompat
//import com.example.hstm_aos.R
//import com.example.hstm_aos.ble.TrainingType
//
//class TimeSeriesGuideView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null
//) : View(context, attrs) {
//
//    companion object {
//        private const val DEPTH_MAX = 80f
//        private const val VENT_MAX = 1000f
//    }
//
//    private val basePadding = dp(20f)
//    private val iconSize = dp(16f)
//    private val iconGap = dp(4f)
//
//    private var trainingTypes: ArrayList<TrainingType> = ArrayList()
//
//    fun setTrainingType(types: ArrayList<TrainingType>) {
//        trainingTypes = types
//        requestLayout()
//        invalidate()
//    }
//
//    private val inter500: Typeface? = try {
//        Typeface.createFromAsset(context.assets, "fonts/Inter-Medium.ttf")
//    } catch (e: Exception) {
//        Typeface.DEFAULT
//    }
//
//    private val textPaintChest = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = Color.parseColor("#34C759")
//        textSize = sp(14f)
//        typeface = inter500
//        textAlign = Paint.Align.LEFT
//    }
//
//    private val textPaintVent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = Color.parseColor("#3B82F6")
//        textSize = sp(14f)
//        typeface = inter500
//        textAlign = Paint.Align.LEFT
//    }
//
//    private val lineStrokeWidth = dp(1f)
//
//    private val compIcon: Bitmap? by lazy {
//        vectorToBitmap(
//            R.drawable.inno_timeseries_comp_icon,
//            iconSize.toInt(),
//            iconSize.toInt()
//        )
//    }
//
//    private val ventIcon: Bitmap? by lazy {
//        vectorToBitmap(
//            R.drawable.inno_timeseries_vent_icon,
//            iconSize.toInt(),
//            iconSize.toInt()
//        )
//    }
//
//    private fun vectorToBitmap(res: Int, w: Int, h: Int): Bitmap? {
//        val d: Drawable = ContextCompat.getDrawable(context, res) ?: return null
//        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
//        val c = Canvas(b)
//        d.setBounds(0, 0, w, h)
//        d.draw(c)
//        return b
//    }
//
//    // ---------- MEASURE (그대로 유지) ----------
//
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//
//        val showChest =
//            trainingTypes.contains(TrainingType.CPR) ||
//                    trainingTypes.contains(TrainingType.CCO)
//
//        val showVent =
//            trainingTypes.contains(TrainingType.CPR) ||
//                    trainingTypes.contains(TrainingType.VO)
//
//        var desiredWidth = 0f
//
//        if (showChest) {
//            desiredWidth = maxOf(
//                textPaintChest.measureText("60mm"),
//                textPaintChest.measureText("50mm"),
//                textPaintChest.measureText("0mm")
//            )
//        }
//
//        if (showVent) {
//            val ventWidth = maxOf(
//                textPaintVent.measureText("700ml"),
//                textPaintVent.measureText("400ml"),
//                textPaintVent.measureText("0ml")
//            )
//            desiredWidth = maxOf(desiredWidth, ventWidth)
//        }
//
//        desiredWidth += iconSize + iconGap + dp(12f)
//
//        val chartHeight = dp(400f)
//        val desiredHeight = chartHeight + basePadding * 2
//
//        setMeasuredDimension(
//            resolveSize(desiredWidth.toInt(), widthMeasureSpec),
//            resolveSize(desiredHeight.toInt(), heightMeasureSpec)
//        )
//    }
//
//    // ---------- DRAW ----------
//
//    override fun onDraw(c: Canvas) {
//        super.onDraw(c)
//
//        val isCCO = trainingTypes.contains(TrainingType.CCO)
//
//        val showChest =
//            trainingTypes.contains(TrainingType.CPR) || isCCO
//
//        val showVent =
//            trainingTypes.contains(TrainingType.CPR) ||
//                    trainingTypes.contains(TrainingType.VO)
//
//        c.save()
//        c.translate(0f, basePadding)
//
//        val drawHeight = height - basePadding * 2
//
//        // 기존 비율 그대로 유지
//        val topH = drawHeight * 0.45f
//        val bottomStart = drawHeight * 0.55f
//        val h = drawHeight * 0.45f
//
//        // -------- CHEST --------
//        if (showChest) {
//
//            val y60Line = topH * 60 / DEPTH_MAX + lineStrokeWidth / 2f
//            val y50Line = topH * 50 / DEPTH_MAX + lineStrokeWidth / 2f
//            val y0Line  = lineStrokeWidth / 2f
//
//            drawIconText(
//                c, compIcon, "60mm",
//                centerYToBaseline(y60Line, textPaintChest),
//                textPaintChest
//            )
//
//            drawIconText(
//                c, compIcon, "50mm",
//                centerYToBaseline(y50Line, textPaintChest),
//                textPaintChest
//            )
//
//            drawIconText(
//                c, compIcon, "0mm",
//                centerYToBaseline(y0Line, textPaintChest),
//                textPaintChest
//            )
//        }
//
//        // -------- VENT --------
//        // ⭐ CCO면 아예 안 그림
//        if (showVent && !isCCO) {
//
//            val fix = dp(8f)
//
//            val y700Line =
//                bottomStart + h - h * 700 / VENT_MAX - lineStrokeWidth / 2f + fix
//
//            val y400Line =
//                bottomStart + h - h * 400 / VENT_MAX - lineStrokeWidth / 2f + fix
//
//            val y0VentLine =
//                bottomStart + h - lineStrokeWidth / 2f
//
//            drawIconText(
//                c, ventIcon, "700ml",
//                centerYToBaseline(y700Line, textPaintVent),
//                textPaintVent
//            )
//
//            drawIconText(
//                c, ventIcon, "400ml",
//                centerYToBaseline(y400Line, textPaintVent),
//                textPaintVent
//            )
//
//            drawIconText(
//                c, ventIcon, "0ml",
//                centerYToBaseline(y0VentLine, textPaintVent),
//                textPaintVent
//            )
//        }
//
//        c.restore()
//    }
//
//    // ---------- UTIL ----------
//
//    private fun centerYToBaseline(centerY: Float, paint: Paint): Float {
//        val fm = paint.fontMetrics
//        return centerY - (fm.ascent + fm.descent) / 2f
//    }
//
//    private fun drawIconText(
//        c: Canvas,
//        icon: Bitmap?,
//        text: String,
//        baselineY: Float,
//        paint: Paint
//    ) {
//        val fm = paint.fontMetrics
//        val textCenterY = baselineY + (fm.ascent + fm.descent) / 2f
//
//        val iconX = dp(4f)
//        val iconY = textCenterY - iconSize / 2f
//
//        icon?.let {
//            c.drawBitmap(it, iconX, iconY, null)
//        }
//
//        val textX = iconX + iconSize + iconGap
//        c.drawText(text, textX, baselineY, paint)
//    }
//
//    private fun sp(v: Float): Float =
//        TypedValue.applyDimension(
//            TypedValue.COMPLEX_UNIT_SP,
//            v,
//            resources.displayMetrics
//        )
//
//    private fun dp(v: Float): Float =
//        TypedValue.applyDimension(
//            TypedValue.COMPLEX_UNIT_DIP,
//            v,
//            resources.displayMetrics
//        )
//}
