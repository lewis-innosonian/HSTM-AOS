package com.example.hstm_aos.Fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.example.hstm_aos.R
import com.example.hstm_aos.ScoreData
import com.example.hstm_aos.adapter.CycleHistoryAdapter
import com.example.hstm_aos.ble.TrainingType
import com.example.hstm_aos.databinding.FragmentScoreCycleBinding
import com.example.hstm_aos.model.CycleWithScore
import com.example.hstm_aos.model.HstmResponse

class ScoreByCycleFragment : Fragment(R.layout.fragment_score_cycle) {

    private var _binding: FragmentScoreCycleBinding? = null
    private val binding get() = _binding!!
    private lateinit var trainingTypes: ArrayList<TrainingType>
    private lateinit var cycleHistoryAdapter: CycleHistoryAdapter
    private var cycleList: List<CycleWithScore>? = null
    private var trainingType : String =""
    private var hstmResponse: HstmResponse? = null
    private var manikinType : String =""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentScoreCycleBinding.bind(view)


        trainingTypes = arguments?.getSerializable(ARG_TRAINING_TYPES) as? ArrayList<TrainingType>
            ?: arrayListOf(TrainingType.CPR)

        manikinType = arguments?.getString(ARG_MANIKIN_TYPES)?:""
//        trainingTypes = arrayListOf(TrainingType.VO)

        trainingTypes = ArrayList(trainingTypes.map {
            if (it == TrainingType.TWORESCUER) TrainingType.CPR else it
        })


        hstmResponse = arguments?.getSerializable(ARG_HSTM_RESPONSE) as? HstmResponse

        // 실제 데이터 리스트
        cycleList = hstmResponse?.cpr_score?.part_scores?.flatMap { it.cycle_with_score_list }


        trainingType = when {
            trainingTypes.contains(TrainingType.CPR) ->
                "CPR Training"

            trainingTypes.contains(TrainingType.CCO) ->
                "Chest Compression Only"

            trainingTypes.contains(TrainingType.VO) ->
                "Ventilation Only"

            else -> ""
        }

        if (trainingType.contains("chest Compression Only",true)){
            binding.ventScoreByCycle.visibility = View.GONE
            binding.cycleInfoRecyclerView.visibility = View.GONE
            binding.scoreByCycleCompCountLayout.visibility = View.GONE
            val params = binding.overallScoreLayout.layoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            binding.overallScoreLayout.layoutParams = params
            val rootLayout = binding.root
            updateLayoutWeights(rootLayout)
//            setTextWithNullCheck(binding.byCycleVentilationRateScore, hstmResponse?.cpr_score?.total_score?.score_vent_count)

        } else if (trainingType.contains("Ventilation Only",true)){
            binding.chestScoreByCycle.visibility = View.GONE
            binding.cycleInfoRecyclerView.visibility = View.GONE
            val params = binding.overallScoreLayout.layoutParams
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            binding.overallScoreLayout.layoutParams = params
            val rootLayout = binding.root
            updateLayoutWeights(rootLayout)

            binding.byCycleVentCountTextview.text = getString(R.string.ventilation_rate)
            setTextWithNullCheck(binding.byCycleVentilationRateScore, hstmResponse?.cpr_score?.total_score?.score_vent_rate)
        } else {
            setTextWithNullCheck(binding.byCycleVentilationRateScore, hstmResponse?.cpr_score?.total_score?.score_vent_count)
        }


        binding.byCycleOverallScoreTextview.text = hstmResponse?.cpr_score?.total_score?.overall.toString()
        // 개별 TextView 초기화 (예시 값)
        setTextWithNullCheck(binding.byCycleCompressionDepthScore, hstmResponse?.cpr_score?.total_score?.score_comp_depth)
        setTextWithNullCheck(binding.byCycleCompressionReleaseScore, hstmResponse?.cpr_score?.total_score?.score_recoil)
        setTextWithNullCheck(binding.byCycleCompressionRateScore, hstmResponse?.cpr_score?.total_score?.score_comp_rate)
        setTextWithNullCheck(binding.byCycleHandPositionScore, hstmResponse?.cpr_score?.total_score?.score_hand_position)
        setTextWithNullCheck(binding.byCycleCompressionFractionScore, hstmResponse?.cpr_score?.total_score?.score_ccf)
        setTextWithNullCheck(binding.byCycleNoCompressionScore, hstmResponse?.cpr_score?.total_score?.score_comp_no)
        setTextWithNullCheck(binding.byCycleVentilationVolumeScore, hstmResponse?.cpr_score?.total_score?.score_vent_vol)

        if (manikinType.contains("infant",true)){
            binding.byCycleVentilationSpeedLayout.visibility = View.VISIBLE
            setTextWithNullCheck(binding.byCycleVentilationSpeedScore, hstmResponse?.cpr_score?.total_score?.score_vent_speed)
        }
        // RecyclerView에 더미 데이터 연결
        binding.root.post {
            binding.cycleInfoRecyclerView.doOnLayout {
                setupRecyclerView(it.width)
                binding.cycleInfoRecyclerView.requestLayout()
            }
        }
    }


    fun updateLayoutWeights(root: View) {
        // 재귀로 LinearLayout 안에 있는 모든 뷰 탐색
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                val params = child.layoutParams
                // LinearLayout.LayoutParams인지 확인 후 weight 변경
                if (params is LinearLayout.LayoutParams) {
                    if (params.weight == 1.6f) {
                        params.weight = 1f
                        child.layoutParams = params
                    }
                }
                // 자식 뷰 재귀 탐색
                updateLayoutWeights(child)
            }
        }
    }

    private fun setupRecyclerView(recyclerViewWidth: Int) {
        val actualData = cycleList?.map { cycle ->
            ScoreData().apply {
                total = cycle.overall.toString()
                compression_depth = cycle.score_comp_depth
                handposition = cycle.score_hand_position
                compression_rate = cycle.score_comp_rate
                compression_recoil = cycle.score_recoil
                ccf = cycle.score_ccf
                ventilation_volume = cycle.score_vent_vol
                ventilation_rate = cycle.score_vent_rate
                ventilation_count = cycle.score_vent_count
                compression_count = cycle.score_comp_count
                ventilation_speed = cycle.score_vent_speed
            }
        } ?: emptyList()

        waitForValidHeight {

            val adapter = CycleHistoryAdapter(
                cycleInfo = actualData,
                recyclerViewWidth = recyclerViewWidth,
                context = requireContext(),
                trainingType = trainingType,
                manikinType = manikinType
            )

            adapter.setReleaseRowHeight(binding.compReleaseByCycleTitle.height)
            adapter.setRowHeight(binding.chestScoreByCycleTitle.height)
            adapter.setCompRateRowHeight(binding.byCycleCompressionRateTitle.height)
            adapter.setCompPositionRowHeight(binding.cprScoreHandPostionTitle.height)
            adapter.setCompFractionRowHeight(binding.byCycleCompressionFractionTitle.height)
            adapter.setCompNoRowHeight(binding.byCycleNoCompressionTitle.height)
            adapter.setVentVolumeHeight(binding.byCycleVentilationVolumeTitle.height)
            adapter.setVentCountHeight(binding.byCycleVentCountTextview.height)

            binding.cycleInfoRecyclerView.adapter = adapter
        }
    }

    private fun waitForValidHeight(onReady: () -> Unit) {
        binding.root.post {
            val h1 = binding.byCycleNoCompressionTitle.height
            val h2 = binding.byCycleVentilationVolumeTitle.height
            val h3 = binding.byCycleVentCountTextview.height

            if (h1 > 0 && h2 > 0 && h3 > 0) {
                onReady()
            } else {
                waitForValidHeight(onReady)
            }
        }
    }

    private fun setTextWithNullCheck(textView: android.widget.TextView, data: Int?) {
        if (!isAdded) return
        if (data == null) {
            textView.text = "-"
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black2))
        } else {
            textView.text = data.toString()
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black1))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TRAINING_TYPES = "arg_training_types"
        private const val ARG_MANIKIN_TYPES = "arg_manikin_types"
        private const val ARG_HSTM_RESPONSE = "arg_hstm_response"

        fun newInstance(
            types: ArrayList<TrainingType>,
            hstmResponse: HstmResponse?,
            manikin : String
        ): ScoreByCycleFragment {
            return ScoreByCycleFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_TRAINING_TYPES, types)
                    putString(ARG_MANIKIN_TYPES, manikin)
                    putSerializable(ARG_HSTM_RESPONSE, hstmResponse)
                }
            }
        }
    }



}
