package com.example.hstm_aos

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment
import com.example.hstm_aos.databinding.DialogLookuptermBinding
import java.util.Locale

class DetailedResultsDialog : DialogFragment() {
    private lateinit var binding: DialogLookuptermBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogLookuptermBinding.inflate(inflater, container, false)

        setupWebView() // WebView 설정

        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = binding.webview // XML에 WebView 추가 필요
        webView.settings.apply {
            javaScriptEnabled = true // JavaScript 활성화
            domStorageEnabled = true // 로컬 저장소 활성화
            cacheMode = WebSettings.LOAD_NO_CACHE // 캐시 없이 로드
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        val url = when (Locale.getDefault().language) {
            "ko" -> "https://innosonian.notion.site/Korean-1a466162145d805d80fdc33deede3933"
            else -> "https://innosonian.notion.site/English-USA-1a466162145d8016b1c0f07a004b085a"
        }

        webView.loadUrl(url)
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.80).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.88).toInt()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setLayout(width, height)
    }
}
