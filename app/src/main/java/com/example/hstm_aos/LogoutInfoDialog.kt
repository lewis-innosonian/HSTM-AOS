package com.example.hstm_aos

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.example.hstm_aos.databinding.DialogCongratulationsBinding
import com.example.hstm_aos.databinding.DialogDisconnectBinding
import com.example.hstm_aos.databinding.DialogHowToConnectBinding
import com.example.hstm_aos.databinding.DialogLogoutinfoBinding

class LogoutInfoDialog : DialogFragment() {

    private lateinit var binding:DialogLogoutinfoBinding
    private var callback: (() -> Unit)? = null

    private var courseName: String? = null

    companion object {
        private const val ARG_COURSE_NAME = "ARG_COURSE_NAME"

        fun newInstance(courseName: String): LogoutInfoDialog {
            return LogoutInfoDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_COURSE_NAME, courseName)
                }
            }
        }
    }

    fun setCallback(callback: () -> Unit) {
        this.callback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        courseName = arguments?.getString(ARG_COURSE_NAME)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        binding = DialogLogoutinfoBinding.inflate(inflater, container, false)


        binding.logoutInfo.setOnClickListener {
            callback?.invoke()
            dismiss()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(750.dpToPx(), WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callback?.invoke()
    }
}