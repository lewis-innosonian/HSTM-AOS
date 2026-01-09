package com.example.hstm_aos

import android.os.Parcelable
import com.example.hstm_aos.ble.DeviceType
import com.example.hstm_aos.ble.TrainingType
import kotlinx.parcelize.Parcelize
import java.io.Serializable

sealed class ContentsItem : Serializable {
    @Parcelize
    data class Header(
        val iconRes: Int,
        val title: String,
        val createdAt: String
    ) : ContentsItem(), Parcelable

    @Parcelize
    data class Content(
        val text: String,
        val requiredDeviceTypes: Set<DeviceType>,
        val status: TrainingStatus,
        val trainingType : Set<TrainingType>
    ) : ContentsItem(), Parcelable
}
