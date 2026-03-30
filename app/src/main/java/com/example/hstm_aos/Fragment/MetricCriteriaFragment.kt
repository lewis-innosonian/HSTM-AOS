package com.example.hstm_aos.Fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.hstm_aos.R
import com.example.hstm_aos.ble.TrainingType
import com.example.hstm_aos.databinding.FragmentMetricCriteriaBinding
import com.example.hstm_aos.model.HstmResponse
import com.example.hstm_aos.model.Metrics

class MetricCriteriaFragment : Fragment(R.layout.fragment_metric_criteria) {

    private var _binding: FragmentMetricCriteriaBinding? = null
    private val binding get() = _binding!!
    private lateinit var trainingTypes: ArrayList<TrainingType>
    private var trainingType: String = ""
    private var hstmResponse: HstmResponse? = null
    private var manikinType: String = ""
    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        _binding = FragmentMetricCriteriaBinding.bind(view)
        hstmResponse = arguments?.getSerializable(ARG_HSTM_RESPONSE) as? HstmResponse

        trainingTypes =
            arguments?.getSerializable("arg_training_types") as? ArrayList<TrainingType>
                ?: arrayListOf(TrainingType.CPR)
        manikinType = arguments?.getString(ARG_MANIKIN_TYPE) ?: ""

        trainingType = when {
            trainingTypes.contains(TrainingType.CPR) ->
                "CPR Training"

            trainingTypes.contains(TrainingType.CCO) ->
                "Chest Compression Only"

            trainingTypes.contains(TrainingType.VO) ->
                "Ventilation Only"

            else -> ""
        }

        hstmResponse?.metrics?.let {
            setCPRMetricsData(
                it,
                trainingType,
                manikinType = manikinType
            )
        }


//        setUIandTitle(trainingType, "adult")
    }


    private fun setCPRMetricsData(data: Metrics, type: String, manikinType: String) {
        Log.d("kimtest3", "speeddata= ${data.VentilationSpeed} type == ${manikinType}  t= ${type}")
        if ((manikinType.equals("baby", ignoreCase = true) || manikinType.equals(
                "infant",
                ignoreCase = true
            ))
        ) {
            binding.ventilationSpeedLayout.visibility = View.VISIBLE

            binding.ventilationSpeedGood.text = (data.VentilationSpeed?.good ?: 0).toString() + "%"
            binding.ventilationSpeedTooFastData.text =
                (data.VentilationSpeed?.tooFast ?: 0).toString() + "%"
            binding.ventilationSpeedTooSlowData.text =
                (data.VentilationSpeed?.tooSlow ?: 0).toString() + "%"

            setTextViewNullCheck(
                binding.ventilationSpeedTooFastData,
                binding.ventilationSpeedTooFastTitle,
                data.VentilationSpeed?.tooFast ?: 0
            )
            setTextViewNullCheck(
                binding.ventilationSpeedTooSlowData,
                binding.ventilationSpeedTooSlowTitle,
                data.VentilationSpeed?.tooSlow ?: 0
            )
        }

        if (type.contains("Chest Compression Only", true)) {
            setNullCheck(binding.dimCompressionNoTextView, binding.compressionNoLayout, null)
            setNullCheck(binding.dimVentilationRateTextView, binding.ventilationRateLayout, null)
            setNullCheck(
                binding.dimVentilationVolumeTextView,
                binding.ventilationVolumeLayout,
                null
            )
            setNullCheck(
                binding.dimVentilationSpeedTextView,
                binding.ventilationSpeedShowView,
                null
            )
            setNullCheck(
                binding.dimCompressionDepthTextView,
                binding.CompressionDepthLayout,
                data?.CompressionDepth
            )
            setNullCheck(binding.dimRecoilTextView, binding.recoilLayout, data?.Recoil)
            setNullCheck(
                binding.dimCompressionRateTextView,
                binding.compressionRateLayout,
                data?.CompressionRate
            )
            setNullCheck(binding.dimScoreOfCcfTextView, binding.scoreOfCcfLayout, data?.ScoreOfCCF)
            setNullCheck(
                binding.dimHandPositionTextView,
                binding.handPositionLayout,
                data?.HandPosition
            )

            binding.ventilationRateGood.text = data?.VentilationCount?.good.toString() + "%"
            binding.ventilationRateInfrequently.text =
                data?.VentilationCount?.tooFew.toString() + "%"
            binding.ventilationRateTooFrequently.text =
                data?.VentilationCount?.tooMany.toString() + "%"
            setTextViewNullCheck(
                binding.ventilationRateInfrequently,
                binding.ventilationRateInfrequentlyTitle,
                data?.VentilationCount?.tooFew
            )
            setTextViewNullCheck(
                binding.ventilationRateTooFrequently,
                binding.ventilationRateTooFrequentlyTitle,
                data?.VentilationCount?.tooMany
            )
            binding.cprMetricCriteriaVentLayout.visibility = View.GONE
            binding.cprMetricCriteriaChestCountLayout.visibility = View.GONE
        } else if (type.contains("ventilation only", true)) {
            binding.ventCountTextView.text = getString(R.string.ventilation_rate)
            binding.cprMetricCriteriaChestLayout.visibility = View.GONE
            setNullCheck(binding.dimCompressionNoTextView, binding.compressionNoLayout, null)
            setNullCheck(
                binding.dimVentilationRateTextView,
                binding.ventilationRateLayout,
                data?.VentilationRate
            )
            setNullCheck(
                binding.dimVentilationVolumeTextView,
                binding.ventilationVolumeLayout,
                data?.VentilationVolume
            )
            setNullCheck(
                binding.dimVentilationSpeedTextView,
                binding.ventilationSpeedShowView,
                data?.VentilationSpeed
            )
            setNullCheck(binding.dimCompressionDepthTextView, binding.CompressionDepthLayout, null)
            setNullCheck(binding.dimRecoilTextView, binding.recoilLayout, null)
            setNullCheck(binding.dimCompressionRateTextView, binding.compressionRateLayout, null)
            setNullCheck(binding.dimScoreOfCcfTextView, binding.scoreOfCcfLayout, null)
            setNullCheck(binding.dimHandPositionTextView, binding.handPositionLayout, null)

            binding.ventilationRateGood.text = data?.VentilationRate?.good.toString() + "%"
            binding.ventilationRateInfrequently.text =
                data?.VentilationRate?.inFrequently.toString() + "%"
            binding.ventilationRateTooFrequently.text =
                data?.VentilationRate?.tooFrequently.toString() + "%"
            setTextViewNullCheck(
                binding.ventilationRateInfrequently,
                binding.ventilationRateInfrequentlyTitle,
                data?.VentilationRate?.inFrequently
            )
            setTextViewNullCheck(
                binding.ventilationRateTooFrequently,
                binding.ventilationRateTooFrequentlyTitle,
                data?.VentilationRate?.tooFrequently
            )

        } else {
            setNullCheck(
                binding.dimCompressionNoTextView,
                binding.compressionNoLayout,
                data?.CompressionCount
            )
            setNullCheck(
                binding.dimVentilationRateTextView,
                binding.ventilationRateLayout,
                data?.VentilationCount
            )
//            setNullCheck(binding.dimVentilationSpeedTextView, binding.ventilationSpeedShowView,data?.VentilationSpeed)
            setNullCheck(
                binding.dimVentilationVolumeTextView,
                binding.ventilationVolumeLayout,
                data?.VentilationVolume
            )
            setNullCheck(
                binding.dimCompressionDepthTextView,
                binding.CompressionDepthLayout,
                data?.CompressionDepth
            )
            setNullCheck(binding.dimRecoilTextView, binding.recoilLayout, data?.Recoil)
            setNullCheck(
                binding.dimCompressionRateTextView,
                binding.compressionRateLayout,
                data?.CompressionRate
            )
            setNullCheck(binding.dimScoreOfCcfTextView, binding.scoreOfCcfLayout, data?.ScoreOfCCF)
            setNullCheck(
                binding.dimHandPositionTextView,
                binding.handPositionLayout,
                data?.HandPosition
            )

            binding.ventilationRateGood.text = data?.VentilationCount?.good.toString() + "%"
            binding.ventilationRateInfrequently.text =
                data?.VentilationCount?.tooFew.toString() + "%"
            binding.ventilationRateTooFrequently.text =
                data?.VentilationCount?.tooMany.toString() + "%"
            setTextViewNullCheck(
                binding.ventilationRateInfrequently,
                binding.ventilationRateInfrequentlyTitle,
                data?.VentilationCount?.tooFew
            )
            setTextViewNullCheck(
                binding.ventilationRateTooFrequently,
                binding.ventilationRateTooFrequentlyTitle,
                data?.VentilationCount?.tooMany
            )

        }


        binding.scoreOfCcf.text = data?.ScoreOfCCF.toString()

        binding.compressionDepthGood.text = data?.CompressionDepth?.good.toString() + "%"
        binding.compressionDepthTooShallow.text =
            data?.CompressionDepth?.tooShallow.toString() + "%"
        binding.compressionDepthTooDeep.text = data?.CompressionDepth?.tooDeep.toString() + "%"
        setTextViewNullCheck(
            binding.compressionDepthTooShallow,
            binding.compressionDepthTooShallowTitle,
            data?.CompressionDepth?.tooShallow
        )
        setTextViewNullCheck(
            binding.compressionDepthTooDeep,
            binding.compressionDepthTooDeepTitle,
            data?.CompressionDepth?.tooDeep
        )

        binding.recoilGood.text = data?.Recoil?.good.toString() + "%"
        binding.recoilIncomplete.text = data?.Recoil?.incomplete.toString() + "%"
        setTextViewNullCheck(
            binding.recoilIncomplete,
            binding.recoilIncompleteTitle,
            data?.Recoil?.incomplete
        )

        binding.compressionRateGood.text = data?.CompressionRate?.good.toString() + "%"
        binding.compressionRateTooFast.text = data?.CompressionRate?.tooFast.toString() + "%"
        binding.compressionRateTooSlow.text = data?.CompressionRate?.tooSlow.toString() + "%"
        setTextViewNullCheck(
            binding.compressionRateTooFast,
            binding.compressionRateTooFastTitle,
            data?.CompressionRate?.tooFast
        )
        setTextViewNullCheck(
            binding.compressionRateTooSlow,
            binding.compressionRateTooSlowTitle,
            data?.CompressionRate?.tooSlow
        )

        binding.handPositionGood.text = data?.HandPosition?.good.toString() + "%"
        binding.handPositionIncorrectLr.text = data?.HandPosition?.incorrectLR.toString() + "%"
        binding.handPositionIncorrectStomach.text =
            data?.HandPosition?.incorrectStomach.toString() + "%"
        setTextViewNullCheck(
            binding.handPositionIncorrectLr,
            binding.handPositionIncorrectLrTitle,
            data?.HandPosition?.incorrectLR
        )
        setTextViewNullCheck(
            binding.handPositionIncorrectStomach,
            binding.handPositionIncorrectStomachTitle,
            data?.HandPosition?.incorrectStomach
        )

        binding.compressionNoGood.text = data?.CompressionCount?.good.toString() + "%"
        binding.compressionNoTooFew.text = data?.CompressionCount?.tooFew.toString() + "%"
        binding.compressionNoTooMany.text = data?.CompressionCount?.tooMany.toString() + "%"
        setTextViewNullCheck(
            binding.compressionNoTooFew,
            binding.compressionNoTooFewTitle,
            data?.CompressionCount?.tooFew
        )
        setTextViewNullCheck(
            binding.compressionNoTooMany,
            binding.compressionNoTooManyTitle,
            data?.CompressionCount?.tooMany
        )

        binding.ventilationVolumeGood.text = data?.VentilationVolume?.good.toString() + "%"
        binding.ventilationVolumeTooLittle.text =
            data?.VentilationVolume?.tooLittle.toString() + "%"
        binding.ventilationVolumeTooMuch.text = data?.VentilationVolume?.tooMuch.toString() + "%"
        setTextViewNullCheck(
            binding.ventilationVolumeTooLittle,
            binding.ventilationVolumeTooLittleTitle,
            data?.VentilationVolume?.tooLittle
        )
        setTextViewNullCheck(
            binding.ventilationVolumeTooMuch,
            binding.ventilationVolumeTooMuchTitle,
            data?.VentilationVolume?.tooMuch
        )


    }


    private fun setNullCheck(dimView: View, view: View, data: Any?) {
        if (data == null) {
            view.visibility = View.GONE
            dimView.visibility = View.VISIBLE
        } else {
            view.visibility = View.VISIBLE
            dimView.visibility = View.GONE
        }
    }

    private fun setTextViewNullCheck(titleView: TextView, dataView: TextView, data: Int?) {
        if (data == 0) {
            if (isAdded) {
                dataView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black4))
                titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black4))
            }
        } else {
            if (isAdded) {
                titleView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black2))
                dataView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black2))
            }
        }
    }

    private fun setUIandTitle(type: String, manikinType: String) {


        if (type.contains("chest compression only", true)) {

            binding.cprMetricCriteriaVentLayout.visibility = View.GONE
            binding.cprMetricCriteriaChestCountLayout.visibility = View.GONE
        } else if (type.contains("ventilation only", true)) {
            binding.cprMetricCriteriaChestLayout.visibility = View.GONE

        } else {

        }
    }


    companion object {
        private const val ARG_TRAINING_TYPES = "arg_training_types"
        private const val ARG_HSTM_RESPONSE = "arg_hstm_response"
        private const val ARG_MANIKIN_TYPE = "arg_manikin_type"

        fun newInstance(
            types: ArrayList<TrainingType>,
            hstmResponse: HstmResponse?,
            manikinType: String
        ): MetricCriteriaFragment {
            return MetricCriteriaFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_MANIKIN_TYPE, manikinType)
                    putSerializable(ARG_TRAINING_TYPES, types)
                    putSerializable(ARG_HSTM_RESPONSE, hstmResponse)
                }
            }
        }
    }


}
