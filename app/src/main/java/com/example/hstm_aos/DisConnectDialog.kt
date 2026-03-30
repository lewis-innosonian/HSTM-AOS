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

class DisConnectDialog : DialogFragment() {
    private lateinit var binding: DialogDisconnectBinding
    private var callback: (() -> Unit)? = null
    private var cancelCallback: (() -> Unit)? = null
    companion object {
        fun newInstance(): DisConnectDialog {
            val dialog = DisConnectDialog()

            return dialog
        }
    }

    fun setCallback(callback: () -> Unit) {
        this.callback = callback
    }

    fun setCancelCallback(callback: () -> Unit) {
        this.cancelCallback = callback
    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        binding = DialogDisconnectBinding.inflate(inflater, container, false)


        binding.reconnectButton.setOnClickListener {
            callback?.invoke()
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
            cancelCallback?.invoke()
            dismiss()
        }
        isCancelable = true
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
