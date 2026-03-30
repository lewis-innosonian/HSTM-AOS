package com.example.hstm_aos.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.io.Serializable

data class HstmResponse(
    val cpr_score: CprScore,
    val metrics: Metrics,
    val aed_score: AedScore,
    val training_stats: TrainingStats,
    val action_count: ActionCount,
    val timeseries_chart_data_key: String?,
    val chart_dataset_url : String,
    val guide_prompts: List<String>,
    val submit_hstm: SubmitHstm
) : Serializable

data class CprScore(
    val total_score: TotalScore,
    val part_scores: List<PartScore>
) : Serializable

data class TotalScore(
    val score_comp_depth: Int,
    val score_comp_rate: Int,
    val score_comp_no: Int,
    val score_comp_count: Int,
    val score_recoil: Int,
    val score_hand_position: Int,
    val score_vent_vol: Int,
    val score_vent_rate: Int?,
    val score_vent_count: Int,
    val score_vent_speed: Int?,
    val score_ccf: Int,
    val score_rescue_vent: Int?,
    val overall: Int,
    val judg_result: String
) : Serializable

data class PartScore(
    val part_num: Int,
    val action_with_score_list: List<ActionWithScore>,
    val cycle_with_score_list: List<CycleWithScore>,
    val score: TotalScore
) : Serializable

data class ActionWithScore(
    val comp_depth: ScoreDetail? = null,
    val comp_rate: ScoreDetail? = null,
    val recoil: ScoreDetail? = null,
    val hand_position: HandPosition? = null,
    val vent_vol: ScoreDetail? = null,
    val vent_rate: ScoreDetail? = null
) : Serializable

data class ScoreDetail(
    val grade: Int,
    val criterion: String,
    val value: Any
) : Serializable

data class HandPosition(
    val grade: Int,
    val criterion: String,
    val value: String
) : Serializable

data class CycleWithScore(
    val calc_case: String,
    val score_comp_depth: Int,
    val score_comp_rate: Int,
    val score_comp_no: Int,
    val score_comp_count: Int,
    val score_recoil: Int,
    val score_hand_position: Int,
    val score_vent_vol: Int,
    val score_vent_rate: Int?,
    val score_vent_count: Int,
    val score_vent_speed: Int?,
    val score_ccf: Int,
    val total_action_ms: Int,
    val overall: Int
) : Serializable

data class Metrics(
    val CompressionDepth: MetricDetail,
    val Recoil: MetricDetail,
    val CompressionRate: MetricDetail,
    val ScoreOfCCF: Int,
    val HandPosition: MetricDetail,
    val CompressionNo: MetricDetail,
    val CompressionCount: MetricDetail,
    val VentilationVolume: MetricDetail,
    val VentilationRate: MetricDetail,
    val VentilationCount: MetricDetail,
    val VentilationSpeed: MetricDetail?,
    val CompressionActionNumber: Int,
    val CompressionCycleNumber: Int,
    val VentilationCycleNumber: Int,
    val VentilationActionNumber: Int,
    val TotalEventTime: Int,
    val TotalHandsOffTime: Int,
    val CCF: CcfMetric
) : Serializable

data class MetricDetail(
    @SerializedName("%_TooShallow") val tooShallow: Int? = null,
    @SerializedName("%_Good") val good: Int? = null,
    @SerializedName("%_TooDeep") val tooDeep: Int? = null,
    @SerializedName("n_TooShallow") val nTooShallow: Int? = null,
    @SerializedName("n_Good") val nGood: Int? = null,
    @SerializedName("n_TooDeep") val nTooDeep: Int? = null,
    @SerializedName("%_Incomplete") val incomplete: Int? = null,
    @SerializedName("n_Incomplete") val nIncomplete: Int? = null,
    @SerializedName("%_TooSlow") val tooSlow: Int? = null,
    @SerializedName("%_TooFast") val tooFast: Int? = null,
    @SerializedName("n_TooSlow") val nTooSlow: Int? = null,
    @SerializedName("n_TooFast") val nTooFast: Int? = null,
    @SerializedName("%_IncorrectLR") val incorrectLR: Int? = null,
    @SerializedName("%_IncorrectStomach") val incorrectStomach: Int? = null,
    @SerializedName("n_IncorrectLR") val nIncorrectLR: Int? = null,
    @SerializedName("n_IncorrectStomach") val nIncorrectStomach: Int? = null,
    @SerializedName("%_TooFew") val tooFew: Int? = null,
    @SerializedName("%_TooMany") val tooMany: Int? = null,
    @SerializedName("n_TooFew") val nTooFew: Int? = null,
    @SerializedName("n_TooMany") val nTooMany: Int? = null,
    @SerializedName("%_TooLittle") val tooLittle: Int? = null,
    @SerializedName("%_TooMuch") val tooMuch: Int? = null,
    @SerializedName("n_TooLittle") val nTooLittle: Int? = null,
    @SerializedName("n_TooMuch") val nTooMuch: Int? = null,
    @SerializedName("%_InFrequently") val inFrequently: Int? = null,
    @SerializedName("%_TooFrequently") val tooFrequently: Int? = null
) : Serializable

data class CcfMetric(
    @SerializedName("%_CCF") val ccf: Int? = null,
    val score: Int,
    val sum_score: Int,
    val ccf_count: Int,
    val sum_ccf: Int
) : Serializable

data class AedScore(
    val overall: Int,
    val part_scores: List<Any>
) : Serializable

data class TrainingStats(
    val cycle_count: Int,
    val elapsed_seconds: Double
) : Serializable

data class ActionCount(
    val comp: Int,
    val vent: Int
) : Serializable

data class SubmitHstm(
    val ok: Boolean,
    val attempts: Int,
    val status_code: Int,
    val response_text: String,
    val access_token_url: String,
    val send_result_url: String,
    val refreshed: Boolean
) : Serializable
