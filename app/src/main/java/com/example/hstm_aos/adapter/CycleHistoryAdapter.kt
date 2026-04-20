package com.example.hstm_aos.adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.R
import com.example.hstm_aos.ScoreData
import com.example.hstm_aos.databinding.ItemCycleHistoryBinding

class CycleHistoryAdapter(
    private val cycleInfo: List<ScoreData>,
    private val recyclerViewWidth: Int,
    private val context: Context,
    private val trainingType: String?,
    private val manikinType : String
) : RecyclerView.Adapter<CycleHistoryAdapter.ViewHolder>() {

    private var releaseRowHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT

    private var compDeathRowHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    private var compRateRowHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT

    private var compHandPositionRowHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    private var compFractionRowHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    private var compNoRowHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    private var ventVolumeRowHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    private var ventCountRowHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT



    inner class ViewHolder(private val binding: ItemCycleHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: ScoreData, position: Int) {
            // 사이클 번호 설정
//            if (cardData?.cpr_guideline?.title?.contains("ERC") == true && !cardData.manikin_type.equals(
//                    "adult"
//                ) && cardData.training_type.equals("CPR Training")
//            ) {
//                if (position == 0) {
//                    binding.cycleNumber.text = "Rescue vent"
//                } else {
//                    binding.cycleNumber.text =
//                        context.getString(R.string.cycle) + "${(position).toString()}"
//                }
//            } else {
                binding.cycleNumber.text =
                    "Cycle" + "${(position + 1).toString()}"
//            }

//            if (cardData?.aed != null) {
//                binding.cycleNumber.text =
//                    "Part" + "${(position + 1).toString()}"
//            }
            // 데이터 초기화
            if (data.total?.toInt() == null) {


                binding.cycleScore.text = "No Result"
                binding.scoreBackground.setBackgroundColor(Color.parseColor("#F5F5F5"))
                binding.cycleScore.setTextColor(ContextCompat.getColor(context, R.color.black2))
                binding.cycleNumber.setTextColor(ContextCompat.getColor(context, R.color.black2))
            } else {
                // Total 값이 있을 때 처리
                binding.cycleScore.text = data.total.toString()
                binding.scoreBackground.setBackgroundColor(Color.parseColor("#1A75CCFD"))
                binding.cycleScore.setTextColor(Color.parseColor("#2072ED"))
                binding.cycleNumber.setTextColor(Color.parseColor("#2072ED"))
            }

            // 각 항목의 점수 설정
            setTextWithNullCheck(binding.compressionDepthScore, data.compression_depth)
            setTextWithNullCheck(binding.handPositionScore, data.handposition)
            setTextWithNullCheck(binding.compressionRateScore, data.compression_rate)
            setTextWithNullCheck(binding.compressionRelease, data.compression_recoil)
            setTextWithNullCheck(binding.compressionFraction, data.ccf)
            setTextWithNullCheck(binding.ventilationVolume, data.ventilation_volume)
            if (trainingType.equals("ventilation only", true)) {
                setTextWithNullCheck(binding.ventilationRate, data.ventilation_rate)
            } else {
                setTextWithNullCheck(binding.ventilationRate, data.ventilation_count)
            }
            setTextWithNullCheck(binding.noOtCompression, data.compression_count)

            if (manikinType.equals("baby") || manikinType.equals(
                    "infant",
                    true
                )
            ) {
                binding.ventilationSpeed.visibility = View.VISIBLE
                setTextWithNullCheck(binding.ventilationSpeed, data.ventilation_speed)
//                binding.ventilationSpeed.text = data.ventilation_speed.toString()
            }


            if (trainingType.equals("ventilation only", true)) {
                binding.compressionDepthScore.visibility = View.GONE
                binding.compressionRelease.visibility = View.GONE
                binding.compressionRateScore.visibility = View.GONE
                binding.handPositionScore.visibility = View.GONE
                binding.compressionFraction.visibility = View.GONE
                binding.noOtCompression.visibility = View.GONE
            } else if (trainingType.equals("chest compression only", true)) {
                binding.ventilationVolume.visibility = View.GONE
                binding.ventilationRate.visibility = View.GONE
                binding.noOtCompression.visibility = View.GONE
            }


            val layoutParams = binding.historyLayout.layoutParams
            if (cycleInfo.size == 1) {
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                Log.d(
                    "kimtest",
                    "recyclerViewWidth == ${recyclerViewWidth}, size == ${cycleInfo.size} , itmeWidth == ${recyclerViewWidth / cycleInfo.size}"
                )
                val itemWidth = recyclerViewWidth / cycleInfo.size
                layoutParams.width = if (itemWidth < 150.dpToPx(context)) {
                    150.dpToPx(context)
                } else {
                    itemWidth
                }
            }
            binding.historyLayout.layoutParams = layoutParams


            val params = binding.compressionRelease.layoutParams
            params.height = releaseRowHeight
            binding.compressionRelease.layoutParams = params

            val params1 = binding.compressionDepthScore.layoutParams
            params1.height = compDeathRowHeight
            binding.compressionDepthScore.layoutParams = params1


            val params2 = binding.compressionRateScore.layoutParams
            params2.height = compRateRowHeight
            binding.compressionRateScore.layoutParams = params2


            val params3 = binding.handPositionScore.layoutParams
            params3.height = compHandPositionRowHeight
            binding.handPositionScore.layoutParams = params3


            val params5 = binding.noOtCompression.layoutParams
            params5.height = compNoRowHeight
            binding.noOtCompression.layoutParams = params5



            val params4 = binding.compressionFraction.layoutParams
            params4.height = compFractionRowHeight
            binding.compressionFraction.layoutParams = params4


            val params6 = binding.ventilationVolume.layoutParams
            params6.height = ventVolumeRowHeight
            binding.ventilationVolume.layoutParams = params6


            val params7 = binding.ventilationRate.layoutParams
            params7.height = ventCountRowHeight
            binding.ventilationRate.layoutParams = params7


            Log.d("kimtest", "noOt height = $compNoRowHeight")
            Log.d("kimtest", "ventVol height = $ventVolumeRowHeight")
            Log.d("kimtest", "ventRate height = $ventCountRowHeight")

        }
    }


    fun setReleaseRowHeight(height: Int) {
        releaseRowHeight = height
    }

    fun setRowHeight(height: Int) {
        compDeathRowHeight = height
    }
    fun setCompRateRowHeight(height: Int) {
        compRateRowHeight = height
    }

    fun setCompPositionRowHeight(height: Int) {
        compHandPositionRowHeight = height
    }

    fun setCompFractionRowHeight(height: Int) {
        compFractionRowHeight = height
    }

    fun setCompNoRowHeight(height: Int) {
        compNoRowHeight = height
    }


    fun setVentVolumeHeight(height: Int) {
        ventVolumeRowHeight = height
    }

    fun setVentCountHeight(height: Int) {
        ventCountRowHeight = height
    }



    private fun setTextWithNullCheck(textView: TextView, data: Int?) {
        if (data == null) {
            textView.text = "-"
            textView.setTextColor(ContextCompat.getColor(context, R.color.black4))
        } else {
            textView.text = data.toString()
            textView.setTextColor(ContextCompat.getColor(context, R.color.black1))
        }
    }

    fun Int.dpToPx(context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemCycleHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cycleInfo[position], position)
    }

    override fun getItemCount(): Int {
        return cycleInfo.size
    }
}