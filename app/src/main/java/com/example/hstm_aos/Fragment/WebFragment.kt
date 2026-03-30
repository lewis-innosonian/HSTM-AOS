package com.example.hstm_aos.Fragment

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.example.hstm_aos.R

class WebFragment : Fragment(R.layout.fragment_web) {

    companion object {
        private const val ARG_URL = "url"

        fun newInstance(url: String) = WebFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_URL, url)
            }
        }
    }

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        val url = arguments?.getString(ARG_URL) ?: return

        val web = v.findViewById<WebView>(R.id.webView)
        web.settings.javaScriptEnabled = true
        web.webViewClient = WebViewClient() // 내부에서 링크 열리도록
        web.loadUrl(url)
    }
}
