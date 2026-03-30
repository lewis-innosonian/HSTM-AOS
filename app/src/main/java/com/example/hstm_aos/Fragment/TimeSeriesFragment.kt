package com.example.hstm_aos.Fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.hstm_aos.*
import com.example.hstm_aos.Fragment.MetricCriteriaFragment.Companion
import com.example.hstm_aos.ble.DeviceType
import com.example.hstm_aos.customview.TimeSeriesChartView
import com.google.gson.Gson
import com.example.hstm_aos.ble.TrainingType
import com.example.hstm_aos.customview.TimeSeriesGuideView
import com.example.hstm_aos.model.HstmResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class TimeSeriesFragment : Fragment(R.layout.fragment_time_series) {

    private lateinit var chart: TimeSeriesChartView
    private lateinit var dd : CheckBox
    private lateinit var checkBoxLayout : LinearLayout
    private lateinit var timeSeriesGuideView: TimeSeriesGuideView
    private lateinit var trainingTypes: ArrayList<TrainingType>
    private var hstmResponse: HstmResponse? = null
    private val DATA_URL =""
    private lateinit var deviceType : ArrayList<DeviceType>
    private var isBaby = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hstmResponse = arguments?.getSerializable(ARG_HSTM_RESPONSE) as? HstmResponse

        trainingTypes =
            arguments?.getSerializable(ARG_TRAINING_TYPES) as? ArrayList<TrainingType>
                ?: arrayListOf(TrainingType.CPR)


        trainingTypes = ArrayList(trainingTypes.map {
            if (it == TrainingType.TWORESCUER) TrainingType.CPR else it
        })

        deviceType =
            arguments?.getSerializable(ARG_DEVICE_TYPE) as? ArrayList<DeviceType>
                ?: arrayListOf(DeviceType.PRO)

        if (deviceType.contains(DeviceType.BABY)){
            isBaby = true
        }
        chart = view.findViewById(R.id.timeSeriesChart)
        checkBoxLayout = view.findViewById(R.id.checkBoxLayout)

        dd = view.findViewById(R.id.dd)

        checkBoxLayout.setOnClickListener {
            if (dd.isChecked) {
                dd.isChecked = false
            }else {
                dd.isChecked = true
            }
        }
        dd.setOnCheckedChangeListener { compoundButton, b ->
            chart.setUseTimestampSpacing(b)
        }

        Log.d("kimtest","@#@# Tr ==${trainingTypes}")

        Log.d("kimtest","@#@# dT ==${deviceType}")

        Log.d("kimtest","@#@# res ==${hstmResponse}")

        chart.setTrainingType(trainingTypes)

        timeSeriesGuideView = view.findViewById(R.id.timeSeriesGuideView)
        timeSeriesGuideView.setTrainingType(trainingTypes,isBaby)
        chart.setIsBaby(isBaby)

        lifecycleScope.launch {
            Log.d("kimtest","he ==${hstmResponse?.chart_dataset_url}")
            try {
                val jsonString = hstmResponse?.chart_dataset_url?.toString()
                    ?.let { loadJsonFromUrl(it) }

//                val jsonString = loadJsonFromUrl("https://brayden-online-v2-api-storage.s3.amazonaws.com/calculator_result/interpreted_rtdata/hstm_v2/production/CPR-ACTION-1769578443-19876dec-a7fb-4256-80b1-6ebfe320b138.json?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIASGEJYPV26X54CVLQ%2F20260128%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Date=20260128T053406Z&X-Amz-Expires=300&X-Amz-SignedHeaders=host&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEJ7%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCXVzLWVhc3QtMiJHMEUCID3BWlQhGn%2BsmqnUyuxxa8eRjuXtZpJ4nASSHubnNmelAiEApE0kSo6puB%2FREUHHlV406Sj6GWp0IoLMaCw8AfVbhLwq9gIIZxAEGgwxNTA2MTI3NzAxNjUiDLEjU8epeBvS9E%2BkIyrTAjfqU9ePwEOxCj0Lamkj2EtA1dPDD8HHgoqgSyU8pYz3AiY1BYytYGUC9JKNq%2BeklTDyi9NPrwMijnpMwG78bqq%2BKw7ckVZ8FqC%2F%2FoHQuVoFxWQdP9m8ksfFRaELdVsXM2c91UCPfcsaY6l5abyqcu32RjJ1fikrm%2BmAP66IvkYuaMFLCCjAwzuc1PUxSGaePKXlsx8is6Nrfb9ErdiJ1%2BQwIPE2u%2FENx2HED2hvIkpGa9YHWV2Jsq5EdqKJf9osVM0mk2VAb87ATm2856KzTJsUXwQvzYmB5CS27i5YHZQuQlZHC3kNAE3z3AcFdjFkaWbB0hFngrhHHt4T1SgIAVBf7%2Fp0IFBCJxe%2Bag6acI1MKj4P4mKqygnE7TAVcUWqM7LBosX8asy1vIOzjVM7k%2B%2FI%2BTpwF70S5DT0K1o7yY8CHDRd%2FAiATf35uCK85kV450D9YjDJv%2BbLBjqeAZT30zTX3vfH5mMGoGqXKIufqkKkMDFsw%2BI9UWJzN1V%2F6H2C61JDK5m%2FvuA%2Bzz86yx6ZACrsA2cDeUJkOo5ZSPH5PFUhYMYRJcr43JsZX631znyyEzDHIkJ6tvvoYgstXWDG6p%2F2mU33B5gJwDYwymIv33M6DjRtWOvmK0TTST7D1fwXjY%2BejeBURFWX9%2BzYeu5zxlNN9HAhPTvYIcjh&X-Amz-Signature=d340dd5df8d68345583f2acef69a0c03180015e32ae169ea46a087378a2f8a4d")

//                val jsonString = loadJson(requireContext(), "cpr2.json")

                val root = Gson().fromJson(jsonString, RootData::class.java)

                val cprList = root.cpr_data_set?.map {
                    CprData(
                        actionType = it.action_type,
                        depthMax = it.comp_depth_max,
                        depthMin = it.comp_depth_min,
                        ventMax = it.vent_vol_max,
                        timestamp = it.timestamp,
                        is_virtual_action = it.is_virtual_action,
                        aed_overlapped_type = it.aed_overlapped_type
                    )
                } ?: emptyList()

                val aedList = root.aed_data_set?.map {
                    AedRange(
                        start = it.start_timestamp,
                        end = it.end_timestamp
                    )
                } ?: emptyList()

                chart.setData(cprList, aedList)

            } catch (e: Exception) {
                e.printStackTrace()
                // 필요하면 여기서 토스트 / 팝업 처리
            }
        }
    }

    fun loadJson(context: Context, fileName: String): String {
        val inputStream = context.assets.open(fileName)
        return inputStream.bufferedReader().use { reader ->
            reader.readText()
        }
    }


    // ⭐ ADD : 인터넷에서 JSON 다운로드
    private suspend fun loadJsonFromUrl(url: String): String =
        withContext(Dispatchers.IO) {

            val client = OkHttpClient()

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }

            response.body?.string()
                ?: throw Exception("Empty response")
        }

    companion object {
        private const val ARG_TRAINING_TYPES = "arg_training_types"
        private const val ARG_HSTM_RESPONSE = "arg_hstm_response"
        private const val ARG_DEVICE_TYPE = "arg_device_type"

        fun newInstance(
            types: ArrayList<TrainingType>?,
            hstmResponse: HstmResponse?,
            deviceType : ArrayList<DeviceType>?
        ): TimeSeriesFragment {
            return TimeSeriesFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_DEVICE_TYPE, deviceType)
                    putSerializable(ARG_TRAINING_TYPES, types)
                    putSerializable(ARG_HSTM_RESPONSE, hstmResponse)
                }
            }
        }
    }
}
