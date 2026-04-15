package com.example.hstm_aos.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.hstm_aos.ContentsItem
import com.example.hstm_aos.DetailedResultsDialog
import com.example.hstm_aos.adapter.ChartPagerAdapter
import com.example.hstm_aos.customview.CustomBarChartView
import com.example.hstm_aos.R
import com.example.hstm_aos.TrainingStatus
import com.example.hstm_aos.UserInfoManager
import com.example.hstm_aos.UserTrainingState
import com.example.hstm_aos.ble.DeviceType
import com.example.hstm_aos.ble.TrainingType
import com.example.hstm_aos.databinding.ActivityResultBinding
import com.example.hstm_aos.model.HstmResponse

class ResultActivity : BaseActivity() {

    private lateinit var binding: ActivityResultBinding
    private lateinit var trainingTypes : ArrayList<TrainingType>
    private var trainingType : String = ""
    private var includeAed : Boolean = false
    private var mannequin : String = ""
    //TODO 타입에 따라 이름 변경
    private var tabTitles = listOf(
        "Time Series Chart",
        "CPR Metric Criteria Chart",
        "CPR Score By Cycle"
    )
    private var hstmResponse: HstmResponse? = null
    private lateinit var tabViews: List<View>
    private var deviceType: ArrayList<DeviceType>? = null
    private lateinit var detailedResultsDialog: DetailedResultsDialog
    private var passingScore = 0
    private var contentItem: ContentsItem.Content? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_result)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 상태바 아이콘을 검정색으로
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        window.statusBarColor = ContextCompat.getColor(this, R.color.black6)

        deviceType = intent.getSerializableExtra("deviceTypes") as? ArrayList<DeviceType>

        contentItem = intent.getSerializableExtra("contentItem") as? ContentsItem.Content

        passingScore = intent.getIntExtra("passing_score",0)

        if (deviceType?.contains(DeviceType.BABY) == true){
            mannequin = "Infant"
        } else if (deviceType?.contains(DeviceType.CHILD)== true){
            mannequin = "Child"
        } else if (deviceType?.contains(DeviceType.PRO)==true){
            mannequin = "Adult"
        }

        binding.passingScoreTextView.text = "Passing Score : ${passingScore}"


        trainingTypes =
            (intent.getSerializableExtra("trainingTypes") as? ArrayList<TrainingType>)
                ?: arrayListOf(TrainingType.CPR,TrainingType.AED)   // 기본값

            if (trainingTypes.toString().contains("cpr",true)){
                binding.trainingTypeTextView.text = "${mannequin} ${contentItem?.text}"
            }else if (trainingTypes.toString().contains("cco",true)){
                binding.trainingTypeTextView.text = "${mannequin} ${contentItem?.text}"
            }else if (trainingTypes.toString().contains("vo",true)){
                binding.trainingTypeTextView.text = "${mannequin} ${contentItem?.text}"
            }


        detailedResultsDialog = DetailedResultsDialog()


        binding.lookUpTermTextView.setOnClickListener {
            detailedResultsDialog.show(supportFragmentManager, "detailedResultsDialog")
        }

        hstmResponse = intent.getSerializableExtra("hstmResponse") as? HstmResponse

        Log.d("kimtest44","sss = ${hstmResponse?.cpr_score?.total_score?.overall?:0} , ${passingScore}")


        contentItem?.let {
            Log.d("kimtest44","complete???? ${it.text}, ${it.Assignment_ID}, ${it.certType}")
            it.status = TrainingStatus.COMPLETED
            UserTrainingState.markCompleted(it)
        }


        if (passingScore < hstmResponse?.cpr_score?.total_score?.overall?:0){

            contentItem?.let {
                Log.d("kimtest44","complete???? ${it.text}, ${it.Assignment_ID}, ${it.certType}")
                it.status = TrainingStatus.COMPLETED
                UserTrainingState.markCompleted(it)
            }

            binding.guidePromptsTextView.text = "Well done! You passed!\n ${hstmResponse?.guide_prompts?.joinToString("\n")}"
        }else {
            binding.guidePromptsTextView.text = hstmResponse?.guide_prompts?.joinToString("\n")
        }
        Log.d("kimtest1","@#@#@?? ${hstmResponse?.timeseries_chart_data_key}")
        hstmResponse?.cpr_score?.part_scores?.forEachIndexed { partIndex, partScore ->
            Log.d("kimtest1", "===== Part ${partIndex + 1} =====")
            partScore.cycle_with_score_list.forEachIndexed { cycleIndex, cycle ->
                Log.d("kimtest1", "Cycle ${cycleIndex + 1}: " +
                        "score_comp_depth=${cycle.score_comp_depth}, " +
                        "score_comp_rate=${cycle.score_comp_rate}, " +
                        "score_comp_no=${cycle.score_comp_no}, " +
                        "score_comp_count=${cycle.score_comp_count}, " +
                        "score_recoil=${cycle.score_recoil}, " +
                        "score_hand_position=${cycle.score_hand_position}, " +
                        "score_vent_vol=${cycle.score_vent_vol}, " +
                        "score_vent_rate=${cycle.score_vent_rate}, " +
                        "score_vent_count=${cycle.score_vent_count}, " +
                        "score_vent_speed=${cycle.score_vent_speed}, " +
                        "score_ccf=${cycle.score_ccf}, " +
                        "total_action_ms=${cycle.total_action_ms}, " +
                        "overall=${cycle.overall}"
                )
            }
        }

        if (trainingTypes.contains(TrainingType.AED)){
            includeAed = true
            binding.aedScoreLayout.visibility = View.VISIBLE
            binding.aedScoreLineView.visibility = View.VISIBLE
            binding.aedTScoreTextView.text = hstmResponse?.aed_score?.overall.toString()
        }
        trainingType = when {
            trainingTypes.contains(TrainingType.CPR) ->
                "CPR Training"

            trainingTypes.contains(TrainingType.CCO) ->
                "Chest Compression Only"

            trainingTypes.contains(TrainingType.VO) ->
                "Ventilation Only"

            else -> "CPR Training"
        }

        tabTitles = when {
            trainingTypes.contains(TrainingType.CPR) -> listOf(
                "Time Series Chart",
                "CPR Metric Criteria Chart",
                "CPR Score By Cycle"
            )

            trainingTypes.contains(TrainingType.CCO) -> listOf(
                "Time Series Chart",
                "Chest Compression Metric Criteria Chart",
                "Chest Compression Score"
            )

            trainingTypes.contains(TrainingType.VO) -> listOf(
                "Time Series Chart",
                "Ventilation Metric Criteria Chart",
                "Ventilation Score"
            )

            else -> listOf(
                "Time Series Chart",
                "CPR Metric Criteria Chart",
                "CPR Score By Cycle"
            )
        }

        binding.userIDTextView.text = "${UserInfoManager.getFirstName(this)} ${UserInfoManager.getLastName(this)}, ${UserInfoManager.getOrganizations(this)}"

        binding.cprOverallScoreTextView.text = hstmResponse?.cpr_score?.total_score?.overall.toString()
        setCPRTrainingScoreData()
        setupTabs()
        setupViewPager()

        binding.BackLayout.setOnClickListener { finish() }

        binding.startStopButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
//                putExtra("completedItem", completedItem)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }



    }

    private fun setupTabs() {

        tabViews = listOf(
            binding.tabContainer.getChildAt(0),
            binding.tabContainer.getChildAt(1),
            binding.tabContainer.getChildAt(2)
        )

        tabViews.forEachIndexed { index, view ->
            val tv = view.findViewById<TextView>(R.id.tvTab)
            tv.text = tabTitles[index]

            view.setOnClickListener {
                binding.viewPager.setCurrentItem(index, false)
                binding.viewPager.post {
                    adjustViewPagerHeight(index)
                }
            }
        }

        updateTabUI(0)
    }

    private fun adjustViewPagerHeight(position: Int) {

        val recyclerView = binding.viewPager.getChildAt(0) as RecyclerView
        val holder =
            recyclerView.findViewHolderForAdapterPosition(position) ?: return

        holder.itemView.post {

            val wSpec = View.MeasureSpec.makeMeasureSpec(
                binding.viewPager.width,
                View.MeasureSpec.EXACTLY
            )

            val hSpec = View.MeasureSpec.makeMeasureSpec(
                0,
                View.MeasureSpec.UNSPECIFIED
            )

            holder.itemView.measure(wSpec, hSpec)

            val lp = binding.viewPager.layoutParams
            lp.height = holder.itemView.measuredHeight
            binding.viewPager.layoutParams = lp
        }
    }


    private fun updateTabUI(selectedPos: Int) {

        tabViews.forEachIndexed { index, view ->

            val text = view.findViewById<TextView>(R.id.tvTab)
            val indicator = view.findViewById<View>(R.id.indicator)

            if (index == selectedPos) {
                text.setTextColor(ContextCompat.getColor(this, R.color.black1))

                text.post {
                    val textWidth = text.paint.measureText(text.text.toString())

                    val params = indicator.layoutParams
                    params.width = textWidth.toInt()
                    indicator.layoutParams = params
                }

                indicator.visibility = View.VISIBLE
            } else {
                text.setTextColor(ContextCompat.getColor(this, R.color.black4))
                indicator.visibility = View.GONE
            }
        }
    }


    private fun setupViewPager() {
        val allCycles = hstmResponse?.cpr_score?.part_scores
            ?.flatMap { it.cycle_with_score_list } ?: emptyList()

        binding.viewPager.adapter = ChartPagerAdapter(this, trainingTypes, hstmResponse,deviceType,mannequin)
        binding.viewPager.setUserInputEnabled(false)
        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateTabUI(position)
                    adjustViewPagerHeight(position)
                }
            }
        )
    }


    private fun setCPRTrainingScoreData() {

        binding.customBarChart.setType(mannequin, trainingType, "ARC",includeAed = includeAed)

        val chartLabels = binding.customBarChart.getLabelKeys()

        val scoreMap = mapOf(
            CustomBarChartView.MetricType.RESCUE_VENT_VOL to 10f,
            CustomBarChartView.MetricType.RESCUE_VENT_COUNT to 20f,
            CustomBarChartView.MetricType.RESCUE_VENT_SPEED to 30f,

            CustomBarChartView.MetricType.COMP_DEPTH to hstmResponse?.cpr_score?.total_score?.score_comp_depth?.toFloat(),
            CustomBarChartView.MetricType.COMP_RELEASE to hstmResponse?.cpr_score?.total_score?.score_recoil?.toFloat(),
            CustomBarChartView.MetricType.COMP_RATE to hstmResponse?.cpr_score?.total_score?.score_comp_rate?.toFloat(),
            CustomBarChartView.MetricType.HAND_POS to hstmResponse?.cpr_score?.total_score?.score_hand_position?.toFloat(),
            CustomBarChartView.MetricType.COMP_FRACTION to hstmResponse?.cpr_score?.total_score?.score_ccf?.toFloat(),
            CustomBarChartView.MetricType.COMP_COUNT to hstmResponse?.cpr_score?.total_score?.score_comp_count?.toFloat(),

            CustomBarChartView.MetricType.VENT_VOL to hstmResponse?.cpr_score?.total_score?.score_vent_vol?.toFloat(),
            CustomBarChartView.MetricType.VENT_COUNT to hstmResponse?.cpr_score?.total_score?.score_vent_count?.toFloat(),
            CustomBarChartView.MetricType.VENT_RATE to hstmResponse?.cpr_score?.total_score?.score_vent_rate?.toFloat(),
            CustomBarChartView.MetricType.VENT_SPEED to hstmResponse?.cpr_score?.total_score?.score_vent_speed?.toFloat(),
            CustomBarChartView.MetricType.AED_SCORE to hstmResponse?.aed_score?.overall?.toFloat(),
        )

        val orderedScores = chartLabels.map { scoreMap[it] }
        binding.customBarChart.setChartData(orderedScores)
    }
}
