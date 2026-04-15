package com.example.hstm_aos.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout

class RoundedFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val path = Path()
    private val rect = RectF()
    private val radius = dpToPx(20f)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }


    override fun onDraw(canvas: Canvas) {
        val save = canvas.save()

        val path = Path().apply {
            addRoundRect(
                RectF(0f, 0f, width.toFloat(), height.toFloat()),
                radius,
                radius,
                Path.Direction.CW
            )
        }

        canvas.clipPath(path)

        super.onDraw(canvas)
        canvas.restoreToCount(save)
    }

    private fun dpToPx(dp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

    override fun dispatchDraw(canvas: Canvas) {
        val save = canvas.save()

        rect.set(0f, 0f, width.toFloat(), height.toFloat())

        path.reset()
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)

        canvas.clipPath(path)

        super.dispatchDraw(canvas)

        canvas.restoreToCount(save)
    }

}