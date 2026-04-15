package com.example.hstm_aos.customview

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.HorizontalScrollView

class CustomHorizontalScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    var isScrollEnabled: Boolean = true

    init {
        clipToPadding = true
        clipChildren = true
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER

        val radius = dpToPx(20f)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return if (isScrollEnabled) super.onInterceptTouchEvent(ev) else false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return if (isScrollEnabled) super.onTouchEvent(ev) else false
    }
}
