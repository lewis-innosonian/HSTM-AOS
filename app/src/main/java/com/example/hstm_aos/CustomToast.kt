package com.example.hstm_aos

import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.hstm_aos.customview.RoundedLinearLayout

class CustomToast(private val activity: Activity) {

    fun show(
        message: String,
        iconRes: Int? = null,
        bgColor: Int,
        stay: Long = 1800L
    ) {

        val decor = activity.window.decorView as ViewGroup
        val view = LayoutInflater.from(activity)
            .inflate(R.layout.custom_toast, decor, false)

        val root = view.findViewById<RoundedLinearLayout>(R.id.toastRoot)
        val text = view.findViewById<TextView>(R.id.toastText)
        val icon = view.findViewById<ImageView>(R.id.toastIcon)

        root.setBackgroundColorInt(bgColor)
        root.alpha = 0.95f

        text.text = ""
        text.alpha = 0f
        icon.visibility = ImageView.GONE
        icon.alpha = 0f
        iconRes?.let { icon.setImageResource(it) }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 140
        }

        view.translationY = 200f
        decor.addView(view, params)

        view.post {

            view.pivotX = view.width / 2f
            view.pivotY = view.height / 2f

            val startY = view.translationY

            val appearCurve = OvershootInterpolator(1.03f)
            val collapseCurve = AccelerateDecelerateInterpolator()

            view.animate()
                .translationY(0f)
                .setDuration(540)
                .setInterpolator(appearCurve)
                .withEndAction {

                    text.text = message
                    text.scaleX = 0.9f
                    text.scaleY = 0.9f
                    text.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(240)
                        .setInterpolator(DecelerateInterpolator())
                        .start()

                    iconRes?.let {
                        icon.visibility = ImageView.VISIBLE
                        icon.scaleX = 0.9f
                        icon.scaleY = 0.9f
                        icon.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(240)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }

                    view.postDelayed({

                        val collapseDuration = 520L
                        val moveDuration = 480L

                        text.animate()
                            .alpha(0f)
                            .scaleX(0.92f)
                            .scaleY(0.92f)
                            .setDuration(collapseDuration)
                            .setInterpolator(collapseCurve)
                            .withEndAction { text.text = "" }
                            .start()

                        icon.animate()
                            .alpha(0f)
                            .scaleX(0.92f)
                            .scaleY(0.92f)
                            .setDuration(collapseDuration)
                            .setInterpolator(collapseCurve)
                            .withEndAction { icon.visibility = ImageView.GONE }
                            .start()

                        view.animate()
                            .translationY(startY + 100f)
                            .scaleX(0.92f)
                            .scaleY(0.92f)
                            .setStartDelay((collapseDuration - 10f).toLong())
                            .setDuration(moveDuration)
                            .setInterpolator(AnticipateInterpolator(1.5f))
                            .withEndAction {
                                decor.removeView(view)
                            }
                            .start()

                    }, stay)
                }
                .start()
        }
    }
}
