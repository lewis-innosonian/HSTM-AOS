package com.example.hstm_aos.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OpenSkill(
    val Cert_Type: Int?,
    val Skill_Type: String?,
    val Skill_Type_id: Int?,
    val Passing_Score: Int?,
    val Due_Date: String?,
    val Assignment_ID: String?,
    val Reference_ID: String?,
    val CompVentRatio: String?   // 없는 경우 null
): Parcelable
