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

class CongratulationsDialog : DialogFragment() {

    private lateinit var binding: DialogCongratulationsBinding
    private var callback: (() -> Unit)? = null

    private var courseName: String? = null

    companion object {
        private const val ARG_COURSE_NAME = "ARG_COURSE_NAME"

        fun newInstance(courseName: String): CongratulationsDialog {
            return CongratulationsDialog().apply {
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
        binding = DialogCongratulationsBinding.inflate(inflater, container, false)

        val name = courseName ?: "course"

        binding.messageTextView.text =
            "You’ve successfully completed\nthe $name course.\nContinue to the next course to keep going."

        binding.reconnectButton.setOnClickListener {
            callback?.invoke()
            dismiss()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(750.dpToPx(), WindowManager.LayoutParams.WRAP_CONTENT)
            setDimAmount(0f)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
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