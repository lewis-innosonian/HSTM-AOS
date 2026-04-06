package com.example.hstm_aos

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.example.hstm_aos.databinding.DialogDisconnectBinding
import com.example.hstm_aos.databinding.DialogHowToConnectBinding

class HowToConnectDialog : DialogFragment() {
    private lateinit var binding: DialogHowToConnectBinding

    companion object {
        fun newInstance(): HowToConnectDialog {
            val dialog = HowToConnectDialog()

            return dialog
        }
    }



    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        binding = DialogHowToConnectBinding.inflate(inflater, container, false)


        binding.reconnectButton.setOnClickListener {
            dismiss()
        }
        return binding.root
    }


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(750.dpToPx(), WindowManager.LayoutParams.WRAP_CONTENT)

    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

    }


    override fun onDestroy() {
        super.onDestroy()

    }

}
