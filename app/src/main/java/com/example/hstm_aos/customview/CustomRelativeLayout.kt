package com.example.hstm_aos.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.RelativeLayout


class CustomRelativeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    var isTouchEnabled: Boolean = true

    override fun dispatchDraw(canvas: Canvas) {
        val path: Path = Path()
        path.addRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            20f, 20f, Path.Direction.CW
        )

        canvas.clipPath(path)
        super.dispatchDraw(canvas)
    }

//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
//        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
//        val desiredHeight = if (heightMode == MeasureSpec.UNSPECIFIED) 600 else heightSize
//        val desiredWidth = desiredHeight * 2
//
//        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
//        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
//
//        val finalWidth = when (widthMode) {
//            MeasureSpec.EXACTLY -> widthSize.coerceAtMost(desiredWidth.toInt())
//            MeasureSpec.AT_MOST -> desiredWidth.coerceAtMost(widthSize)
//            else -> desiredWidth
//        }
//
//        val finalHeight = when (heightMode) {
//            MeasureSpec.EXACTLY -> heightSize
//            MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(heightSize)
//            else -> desiredHeight
//        }
//
//        val childWidthSpec = MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY)
//        val childHeightSpec = MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY)
//
//        for (i in 0 until childCount) {
//            getChildAt(i).measure(childWidthSpec, childHeightSpec)
//        }
//
//        setMeasuredDimension(finalWidth, finalHeight)
//    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val width = right - left
        val height = bottom - top

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.layout(0, 0, width, height)
        }
    }

    init {
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (isTouchEnabled) super.onInterceptTouchEvent(ev) else false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return if (isTouchEnabled) super.onTouchEvent(ev) else false
    }
}
