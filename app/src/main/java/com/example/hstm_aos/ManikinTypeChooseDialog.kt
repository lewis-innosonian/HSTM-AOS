package com.example.hstm_aos

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.example.hstm_aos.ble.DeviceType

class ManikinTypeChooseDialog(context: Context) {

    interface Listener {
        fun onSelected(type: DeviceType)
        fun onCancel()
    }

    lateinit var listener: Listener
    private val dlg = Dialog(context)
    private lateinit var radioGroup: RadioGroup

    fun start() {
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dlg.setContentView(R.layout.manikin_choose_dialog)
        dlg.setCanceledOnTouchOutside(false)

        radioGroup = dlg.findViewById(R.id.radioGroup)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val type = when (checkedId) {
                R.id.radioAdult -> DeviceType.PRO
                R.id.radioChild -> DeviceType.CHILD
                else -> return@setOnCheckedChangeListener
            }

            listener.onSelected(type)
            dlg.dismiss()
        }

        dlg.setOnDismissListener {
            if (radioGroup.checkedRadioButtonId == -1) {
                listener.onCancel()
            }
        }

        dlg.show()
    }
}
