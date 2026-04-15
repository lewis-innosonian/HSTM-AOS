package com.example.hstm_aos.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment
import com.example.hstm_aos.R

class WebViewPopupFragment : DialogFragment() {

    companion object {
        private const val ARG_URL = "arg_url"

        fun newInstance(url: String): WebViewPopupFragment {
            val fragment = WebViewPopupFragment()
            val bundle = Bundle().apply { putString(ARG_URL, url) }
            fragment.arguments = bundle
            return fragment
        }
    }

    private lateinit var webView: WebView
    private var urla: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        urla = arguments?.getString(ARG_URL)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_webview_popup, container, false)
        webView = view.findViewById(R.id.webView)

        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            urla?.let { loadUrl(it) }
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
