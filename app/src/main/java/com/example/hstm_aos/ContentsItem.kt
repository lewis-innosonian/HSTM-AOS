package com.example.hstm_aos

import com.example.hstm_aos.ble.DeviceType

sealed class ContentsItem {
    data class Header(
        val iconRes: Int,
        val title: String,
        val createdAt: String
    ) : ContentsItem()

    data class Content(
        val text: String,
        val requiredDeviceTypes: Set<DeviceType>,
        val status: TrainingStatus
    ) : ContentsItem()
}
