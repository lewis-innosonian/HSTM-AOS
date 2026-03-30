package com.example.hstm_aos.customview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.res.ResourcesCompat
import com.example.hstm_aos.R
import java.util.Locale
import kotlin.math.min

class CustomHandPositionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var trainingType: String = "CPR"
    private var manikinType : String ="Adult"
    private var virtualData = false

    private var stateValue: Int = 0
    private var animatedDotRadius = 0f
    private val maxDotRadius = dpToPx(20f)

    private var dotAnimator: ValueAnimator? = null

    private var isShowingDot = false

    private val handler = Handler(Looper.getMainLooper())
    private val clearDotRunnable = Runnable {
        stateValue = 0
        invalidate()
    }


    private var bgDrawable: Drawable? = AppCompatResources.getDrawable(context, R.mipmap.inno_handposition_icon)
//    private val bgDrawable by lazy {
//        ResourcesCompat.getDrawable(resources, R.drawable.inno_handposition_background, null)
//    }

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics
        )

    private fun spToPx(sp: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics
        )

    private fun getLocalizedFont(): Typeface? =
        when (Locale.getDefault().language) {
            "ko" -> ResourcesCompat.getFont(context, R.font.pretendard_medium)
            else -> ResourcesCompat.getFont(context, R.font.inter_medium)
        }

    private val greenPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = Color.parseColor("#56ED89")
    }

    private val redPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = Color.parseColor("#EE6B6B")
    }

    private val grayPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = Color.parseColor("#D9D9D9")
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = spToPx(14f)
        typeface = getLocalizedFont()
        color = Color.parseColor("#666666")
    }

    private val depthTextPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = spToPx(20f)
        typeface = getLocalizedFont()
        color = Color.parseColor("#666666")
    }

    fun updateState(value: Int) {
        stateValue = value

        dotAnimator?.cancel()

        dotAnimator = ValueAnimator.ofFloat(dpToPx(2f), maxDotRadius).apply {
            duration = 250
            addUpdateListener {
                animatedDotRadius = it.animatedValue as Float
                postInvalidateOnAnimation()
            }
            start()
        }

        handler.removeCallbacks(clearDotRunnable)
        handler.postDelayed({
            stateValue = 0
            animatedDotRadius = 0f
            invalidate()
        }, 1000)
    }


    fun setTrainingType(type: String) {
        trainingType = type
        invalidate()
    }

    fun setManikinType(type: String) {
        manikinType = type
        invalidate()
    }

    fun setVirtualData(isVirtual: Boolean) {
        virtualData = isVirtual
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)

        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> MeasureSpec.getSize(heightMeasureSpec)
            else -> suggestedMinimumHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height)
        val left = (width - size) / 2
        val top = (height - size) / 2
        val right = left + size
        val bottom = top + size

        if (manikinType.contains("infant")){
            bgDrawable = AppCompatResources.getDrawable(context, R.mipmap.inno_handposition_icon_infant)

        }

        if (trainingType.contains("vent",true)){
            bgDrawable?.let { drawable ->
                drawable.alpha = (255 * 0.16f).toInt()
                drawable.setBounds(left, top, right, bottom)
                drawable.draw(canvas)
            }
        } else {
            bgDrawable?.let { drawable ->
                drawable.alpha = (255 * 1f).toInt()
                drawable.setBounds(left, top, right, bottom)
                drawable.draw(canvas)
            }
        }


        drawContent(canvas, left.toFloat(), top.toFloat(), size.toFloat())
    }

    private fun drawContent(canvas: Canvas, left: Float, top: Float, size: Float) {

        val viewport = 250f

        fun vx(x: Float) = left + size * (x / viewport)
        fun vy(y: Float) = top + size * (y / viewport)

        val centerX = vx(125f)
        val centerY = vy(125f)

        Log.d("kimtest","stateValue == ${stateValue}")

        // 중앙 텍스트
//        val centerTextY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
//        canvas.drawText(context.getString(R.string.center), centerX, centerTextY, textPaint)

        // 깊이 텍스트 (중앙선 기준 10dp 위)
//        val depthTextY = centerY - dpToPx(10f) - (depthTextPaint.descent() + depthTextPaint.ascent()) / 2
//        canvas.drawText("80", centerX, depthTextY, depthTextPaint)

        val radius = getCurrentRadius()

        val useGreen = if (virtualData) grayPaint else greenPaint
        val useRed = if (virtualData) grayPaint else redPaint

        val downX = vx(125f)
        val downY = vy(210f) - radius   // 위로 radius만큼

        val leftX = vx(40f) + radius    // 오른쪽으로 radius만큼
        val leftY = vy(125f)

        val rightX = vx(210f) - radius  // 왼쪽으로 radius만큼
        val rightY = vy(125f)

        when (stateValue) {
            0 -> null
            1 -> drawDot(canvas, centerX, centerY, useGreen, radius)

            2 -> drawDot(canvas, downX, downY, useRed, radius)
            4 -> drawDot(canvas, leftX, leftY, useRed, radius)
            8 -> drawDot(canvas, rightX, rightY, useRed, radius)

            10 -> drawDot(canvas, centerX, centerY, useRed, radius)

            else -> drawDot(canvas, centerX, centerY, useRed, radius)
        }

    }


    private fun getCurrentRadius(): Float {
        return if (animatedDotRadius == 0f) maxDotRadius else animatedDotRadius
    }

    private fun drawDot(
        canvas: Canvas,
        x: Float,
        y: Float,
        paint: Paint,
        radius: Float
    ) {
        canvas.drawCircle(x, y, radius, paint)
    }
}
