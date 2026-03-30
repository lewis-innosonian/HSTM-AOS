package com.example.hstm_aos

import android.os.Parcelable
import com.example.hstm_aos.ble.DeviceType
import com.example.hstm_aos.ble.TrainingType
import kotlinx.parcelize.Parcelize
import java.io.Serializable
import kotlin.time.Duration

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
        val duration: String,
        val requiredDeviceTypes: Set<DeviceType>,
        var status: TrainingStatus,
        val trainingType : Set<TrainingType>,
        val skillTypeId: Int,  // OpenSkill.Skill_Type_id
        val certType: Int,      // OpenSkill.Cert_Type
        val passing_Score : Int
    ) : ContentsItem(), Parcelable
}
