package com.example.hstm_aos

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView

class RetryDialog(context: Context, failReasonString: String, isInternetError : Boolean) {

    lateinit var listener: ReTryDialogClickedListener
    lateinit var noBtn: Button
    lateinit var tryAgainBtn: Button
    lateinit var failReason: TextView
    var failResason = failReasonString
    val isInternetError : Boolean = isInternetError
    var context: Context = context

    interface ReTryDialogClickedListener {
        fun noButton()
        fun tryAgainButton()

    }

    private val dlg = Dialog(context)

    fun start() {
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dlg.setContentView(R.layout.retry_dialog)
        dlg.setCanceledOnTouchOutside(false)

        noBtn = dlg.findViewById(R.id.no_btn)
        noBtn.setOnClickListener {
            listener.noButton()
            dlg.dismiss()
        }

        failReason = dlg.findViewById(R.id.failReason)
        failReason.text = failResason

        tryAgainBtn = dlg.findViewById(R.id.try_again_btn)
        if (!isInternetError) {
            noBtn.visibility = View.GONE
            tryAgainBtn.setOnClickListener {
                listener.noButton()
                dlg.dismiss()
            }

        } else {
            noBtn.text = "Cancel"
            noBtn.visibility = View.VISIBLE
            tryAgainBtn.setOnClickListener {
                listener.tryAgainButton()
                dlg.dismiss()
            }
            noBtn.setOnClickListener {
                listener.noButton()
                dlg.dismiss()
            }

        }

        dlg.show()
    }

}