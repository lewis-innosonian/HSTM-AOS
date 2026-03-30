package com.example.hstm_aos.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.hstm_aos.Fragment.MetricCriteriaFragment
import com.example.hstm_aos.Fragment.ScoreByCycleFragment
import com.example.hstm_aos.Fragment.TimeSeriesFragment
import com.example.hstm_aos.ble.DeviceType
import com.example.hstm_aos.ble.TrainingType
import com.example.hstm_aos.model.CycleWithScore
import com.example.hstm_aos.model.HstmResponse

class ChartPagerAdapter(
    activity: FragmentActivity,
    private val trainingTypes: ArrayList<TrainingType>,
    private val hstmResponse: HstmResponse?,
    private val DeviceTypes: ArrayList<DeviceType>?,
    private val manikinType : String
) : FragmentStateAdapter(activity) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {

            0 -> TimeSeriesFragment.newInstance(trainingTypes,hstmResponse,DeviceTypes)
            1 -> MetricCriteriaFragment.newInstance(trainingTypes,hstmResponse,manikinType)
            else -> ScoreByCycleFragment.newInstance(trainingTypes, hstmResponse,manikinType)
        }
    }
}
