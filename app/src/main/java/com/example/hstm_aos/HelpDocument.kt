package com.example.hstm_aos

import java.io.Serializable

data class HelpDocument(
    val fileName: String,
    val type: String, // VIDEO, PDF, WEB
    val url: String?, // VIDEO나 WEB의 경우
    val base64Content: String? // PDF 또는 파일 저장용
) : Serializable