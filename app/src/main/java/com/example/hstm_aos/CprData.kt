package com.example.hstm_aos

data class RootData(
    val cpr_data_set: List<CprRaw>,
    val aed_data_set: List<AedRaw>
)

data class CprRaw(
    val action_type: String,   // comp, vent
    val comp_depth_max: Int,
    val comp_depth_min: Int,
    val vent_vol_max: Int,
    val timestamp: Double,
    val is_virtual_action :Boolean,
    val aed_overlapped_type : String?
)

data class AedRaw(
    val part_num: Int,
    val start_timestamp: Double,
    val end_timestamp: Double
)


// 차트용 가공 데이터
data class CprData(
    val actionType: String,
    val depthMax: Int,
    val depthMin: Int,
    val ventMax: Int,
    val timestamp: Double,
    val is_virtual_action : Boolean,
    val aed_overlapped_type : String?
)

data class AedRange(
    val start: Double,
    val end: Double
)
