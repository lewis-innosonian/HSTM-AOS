package com.example.hstm_aos

sealed class ContentsItem {
    data class Header(
        val iconRes: Int,
        val title: String,
        val createdAt: String
    ) : ContentsItem()

    data class Content(
        val text: String
    ) : ContentsItem()
}
