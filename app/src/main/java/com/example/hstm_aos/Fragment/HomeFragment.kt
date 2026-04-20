// HomeFragment.kt
package com.example.hstm_aos.Fragment

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.*
import com.example.hstm_aos.activity.BaseActivity
import com.example.hstm_aos.activity.MainActivity
import com.example.hstm_aos.activity.TrainingActivity
import com.example.hstm_aos.activity.TwoRescuerTrainingActivity
import com.example.hstm_aos.adapter.ContentsAdapter
import com.example.hstm_aos.adapter.DeviceAdapter
import com.example.hstm_aos.ble.BleDevice
import com.example.hstm_aos.ble.BleManager
import com.example.hstm_aos.ble.DeviceType
import com.example.hstm_aos.ble.TrainingType
import com.example.hstm_aos.databinding.FragmentHomeBinding
import com.example.hstm_aos.model.OpenSkill
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.Locale
import kotlin.math.ceil

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var bleManager: BleManager
    private lateinit var deviceAdapter: DeviceAdapter
    private val contentsAdapter= mutableListOf<ContentsAdapter>()

    private val deviceTypeMap = mutableMapOf<String, DeviceType>()
    private val connectionMap = mutableMapOf<String, Boolean>()
    private val originalItems = mutableListOf<ContentsItem>()

    // 서버에서 전달받은 스킬
    private val ALScontents = mutableListOf<OpenSkill>()
    private val BLScontents = mutableListOf<OpenSkill>()
    private val PALScontents = mutableListOf<OpenSkill>()
    //
    private lateinit var howtoConnectDialog: HowToConnectDialog
    private lateinit var congratulationsDialog : CongratulationsDialog
    private val completedCourseLogged = mutableSetOf<Int>()
    private var lastCompletedCourseCount = 0

    private val shownCourseDialogs = mutableSetOf<Int>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)


        if (UserInfoManager.getVoiceGuide(requireContext())) {
            binding.voiceGuideSV.isChecked = true
        } else {
            binding.voiceGuideSV.isChecked = false
        }
        congratulationsDialog = CongratulationsDialog()

        binding.howToDisconnectLayout.setOnClickListener{
            showTooltip(binding.howToConnectIcon)
//            howtoConnectDialog = HowToConnectDialog.newInstance()
//            howtoConnectDialog.show(parentFragmentManager, "howToConnectDialog")

        }

        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedProgress = prefs.getInt("font_step", 0)

        binding.seekBar.progress = savedProgress

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
                prefs.edit().putInt("font_step", progress).apply()

                val scale = FontScaleManager.getScale(progress)

                (requireActivity() as BaseActivity).updateScale(scale)
                (requireActivity() as MainActivity).tabselectTab()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })


        binding.voiceGuideSV.onCheckedChangeListener = { isChecked ->
            if (isChecked) {
                UserInfoManager.setVoiceGuide(requireContext(),true)
                Log.d("kimtest", "on")
            } else {
                UserInfoManager.setVoiceGuide(requireContext(),false)
                Log.d("kimtest", "OFF!")
            }
        }

        bleManager = (requireActivity().application as MainApplication).bleManager

        setupDeviceRecyclerView()
//        deviceAdapter.submitList(createDummyDevices())
        restoreBleState()
        observeBle()
//        observeCompletedTraining()

        val skills = arguments?.getSerializable("skills") as? List<OpenSkill> ?: emptyList()
        categorizeSkills(skills)
        fillContentItems()
        setupContentsAdapter()

        view.post {
            adjustSearchingPosition()
        }

    }
    private fun adjustSearchingPosition() {
//        val recyclerView = binding.deviceRecyclerView
//        val searchingLayout = binding.searchingLayout
//        val container = binding.deviceContainer
//
//        recyclerView.post {
//            // remove 먼저
//            if (searchingLayout.parent != null) {
//                (searchingLayout.parent as? ViewGroup)?.removeView(searchingLayout)
//            }
//
//            // 항상 container 위에 붙이되, 맨 아래로
//            container.addView(
//                searchingLayout,
//                FrameLayout.LayoutParams(
//                    FrameLayout.LayoutParams.WRAP_CONTENT,
//                    FrameLayout.LayoutParams.WRAP_CONTENT,
//                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
//                )
//            )
//        }
    }
    // -----------------------------
    // Training 완료 이벤트 구독
    // -----------------------------
//    private fun observeCompletedTraining() {
//        lifecycleScope.launch {
//            (requireActivity().application as MainApplication)
//                .completedTrainingFlow.collect { completedItem ->
//                    markContentCompleted(completedItem)
//                }
//        }
//    }

//    fun markContentCompleted(completedItem: ContentsItem.Content) {
//        val updated = originalItems.map { item ->
//            if (item is ContentsItem.Content && item.text == completedItem.text) {
//                item.copy(status = TrainingStatus.COMPLETED)
//            } else item
//        }
//        contentsAdapter.updateItems(updated)
//    }

    private fun setupDeviceRecyclerView() {



        deviceAdapter = DeviceAdapter(mutableListOf()) { device ->
            bleManager.toggleConnection(device)
        }

        deviceAdapter.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {

            override fun onChanged() {
                adjustSearchingPosition()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                adjustSearchingPosition()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                adjustSearchingPosition()
            }
        })
        binding.deviceRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
    }



    private fun setupContentsAdapter() {

        val contentList = originalItems.filterIsInstance<ContentsItem.Content>()
        val splitLists = splitContentItems(contentList)

        contentsAdapter.clear()

        val adapters = splitLists.map { list ->
            ContentsAdapter(list.toMutableList()) { contentItem ->
                if (contentItem.trainingType.contains(TrainingType.TWORESCUER)) {
                    val intent = Intent(requireContext(), TwoRescuerTrainingActivity::class.java).apply {
                        putExtra("contentItem", contentItem as Serializable)
                    }
                    startActivity(intent)
                } else {
                    val intent = Intent(requireContext(), TrainingActivity::class.java).apply {
                        putExtra("contentItem", contentItem as Serializable)
                    }
                    startActivity(intent)
                }
            }
        }

        contentsAdapter.addAll(adapters)

        binding.recyclerView1.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView2.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView3.layoutManager = LinearLayoutManager(requireContext())

        binding.recyclerView1.adapter = adapters.getOrNull(0)
        binding.recyclerView2.adapter = adapters.getOrNull(1)
        binding.recyclerView3.adapter = adapters.getOrNull(2)
    }

//    private fun setupContentsAdapter() {
//        contentsAdapter = ContentsAdapter(originalItems.toMutableList()) { contentItem ->
//            if (contentItem.trainingType.contains(TrainingType.TWORESCUER)){
//                val intent = Intent(requireContext(), TwoRescuerTrainingActivity::class.java).apply {
//                    putExtra("contentItem", contentItem as Serializable)
//                }
//                requireContext().startActivity(intent)
//            }else {
//                val intent = Intent(requireContext(), TrainingActivity::class.java).apply {
//                    putExtra("contentItem", contentItem as Serializable)
//                }
//                requireContext().startActivity(intent)
//            }
//        }
//        binding.rightRecyclerView.apply {
//            layoutManager = LinearLayoutManager(requireContext())
//            adapter = contentsAdapter
//            addItemDecoration(StickyHeaderDecoration(contentsAdapter))
//        }
//    }

    private fun restoreBleState() {
        connectionMap.clear()
        connectionMap.putAll(bleManager.getCurrentConnectionMap())

        deviceTypeMap.clear()
        deviceTypeMap.putAll(bleManager.getCurrentDeviceTypeMap())

        updateTrainingStatus()
    }

    private fun categorizeSkills(skills: List<OpenSkill>) {
        ALScontents.clear()
        BLScontents.clear()
        PALScontents.clear()

        skills
            .distinctBy { "${it.Cert_Type}_${it.Skill_Type_id}" }
            .forEach { skill ->

                when (skill.Cert_Type) {
                    1 -> ALScontents.add(skill)
                    2 -> BLScontents.add(skill)
                    3 -> PALScontents.add(skill)
                }
            }
    }

    private fun fillContentItems() {
        val compressionIds = listOf(1, 16, 256)
        val ventillationIds = listOf(2, 32, 512)
        val CPRIds = listOf(4, 64, 1024, )
        val twoRescuerIds = listOf(8,128,2048)
        val aed = listOf(10101)

        val babySkill = listOf(256, 512, 1024, 2048)
        val adultSkill = listOf(1, 2, 4, 8)
        val childSkill = listOf(16, 32, 64,128)

        val aedSkill = listOf(10101)
        originalItems.clear()

        fun getDeviceTypes(skillId: Int): Set<DeviceType> {
            return when {
                babySkill.contains(skillId) -> setOf(DeviceType.BABY)
                adultSkill.contains(skillId) -> setOf(DeviceType.PRO)
                childSkill.contains(skillId) -> setOf(DeviceType.CHILD)
                aedSkill.contains(skillId) -> setOf(DeviceType.AED,DeviceType.PRO)
                else -> emptySet()
            }
        }

        fun convertToLocalTime(utcString: String): String {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")

            val date = inputFormat.parse(utcString) ?: return ""

            val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            outputFormat.timeZone = java.util.TimeZone.getDefault()

            return outputFormat.format(date)
        }

        fun getTrainingType(skillId: Int): Set<TrainingType> {
            val types = mutableSetOf<TrainingType>()
            if (compressionIds.contains(skillId)) types.add(TrainingType.CCO)
            if (ventillationIds.contains(skillId)) types.add(TrainingType.VO)
            if (CPRIds.contains(skillId)) types.add(TrainingType.CPR)
            if (twoRescuerIds.contains(skillId)) types.add(TrainingType.TWORESCUER)
            if (aed.contains(skillId)) types.addAll(listOf(TrainingType.AED, TrainingType.CPR))
            return types
        }

        // ALS
        originalItems.add(
            ContentsItem.Header(
                iconRes = R.drawable.inno_header_title_icon,
                title = "ALS",
                createdAt = "Due: TBD"
            )
        )
        ALScontents.forEach { skill ->
            skill.Skill_Type_id?.let { id ->
                // 원래 Due_Date 문자열
                val originalDate = skill.Due_Date?.let { convertToLocalTime(it) }
                // 포맷 변환
                val formattedDate = originalDate?.let {
                    try {
                        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        val date = parser.parse(it)
                        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                        "Due: ${formatter.format(date)}"
                    } catch (e: Exception) {
                        "Due: TBD"
                    }
                } ?: "Due: TBD"

                originalItems.add(
                    ContentsItem.Content(
                        text = skill.Skill_Type ?: "",
                        duration = formattedDate,
                        requiredDeviceTypes = getDeviceTypes(id),
                        status = TrainingStatus.AVAILABLE,
                        trainingType = getTrainingType(id),
                        skillTypeId = id,
                        certType = skill.Cert_Type ?: 0,
                        passing_Score = skill.Passing_Score ?:0,
                        Assignment_ID = skill.Assignment_ID?:""
                    )
                )
            }
        }

        // BLS
        originalItems.add(
            ContentsItem.Header(
                iconRes = R.drawable.inno_header_title_icon,
                title = "BLS",
                createdAt = "Due: TBD"
            )
        )
        BLScontents.forEach { skill ->
            skill.Skill_Type_id?.let { id ->
                val originalDate = skill.Due_Date?.let { convertToLocalTime(it) }
                // 포맷 변환
                val formattedDate = originalDate?.let {
                    try {
                        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        val date = parser.parse(it)
                        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                        "Due: ${formatter.format(date)}"
                    } catch (e: Exception) {
                        "Due: TBD"
                    }
                } ?: "Due: TBD"


                originalItems.add(
                    ContentsItem.Content(
                        text = skill.Skill_Type ?: "",
                        duration = formattedDate,
                        requiredDeviceTypes = getDeviceTypes(id),
                        status = TrainingStatus.AVAILABLE,
                        trainingType = getTrainingType(id),
                        skillTypeId = id,              // 고유 ID
                        certType = skill.Cert_Type ?: 0,
                        passing_Score = skill.Passing_Score ?:0,
                        Assignment_ID = skill.Assignment_ID?:""
                    )
                )
            }
        }

        // PALS
        originalItems.add(
            ContentsItem.Header(
                iconRes = R.drawable.inno_header_title_icon,
                title = "PALS",
                createdAt = "Due: TBD"
            )
        )
        PALScontents.forEach { skill ->
            skill.Skill_Type_id?.let { id ->

                val originalDate = skill.Due_Date?.let { convertToLocalTime(it) }
                // 포맷 변환
                val formattedDate = originalDate?.let {
                    try {
                        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        val date = parser.parse(it)
                        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                        "Due: ${formatter.format(date)}"
                    } catch (e: Exception) {
                        "Due: TBD"
                    }
                } ?: "Due: TBD"


                originalItems.add(
                    ContentsItem.Content(
                        text = skill.Skill_Type ?: "",
                        duration = formattedDate,
                        requiredDeviceTypes = getDeviceTypes(id),
                        status = TrainingStatus.AVAILABLE,
                        trainingType = getTrainingType(id),
                        skillTypeId = id,              // 고유 ID
                        certType = skill.Cert_Type ?: 0,
                        passing_Score = skill.Passing_Score ?:0,
                        Assignment_ID = skill.Assignment_ID?:""
                    )
                )
            }
        }

//        contentsAdapter.updateItems(originalItems)
        updateTrainingStatus()
    }


    private fun checkCourseCompletion() {

        val all = originalItems.filterIsInstance<ContentsItem.Content>()

        val als = all.filter { it.certType == 1 }
        val bls = all.filter { it.certType == 2 }
        val pals = all.filter { it.certType == 3 }

        fun isAllCompleted(list: List<ContentsItem.Content>): Boolean {
            return list.isNotEmpty() &&
                    list.all { UserTrainingState.isCompleted(it) }
        }

        val alsDone = isAllCompleted(als)
        val blsDone = isAllCompleted(bls)
        val palsDone = isAllCompleted(pals)

        Log.d("CHECK", "ALS = ${als.count { UserTrainingState.isCompleted(it) }} / ${als.size}")



        if (alsDone && blsDone && palsDone) {

            if (alsDone) {
                Log.d("TRAINING_COMPLETE", "ALS 전체 완료")

                if (!shownCourseDialogs.contains(1)) {
                    shownCourseDialogs.add(1)

                    (requireActivity() as MainActivity).showLottie()

                    val dialog = FinishCourseDialog.newInstance("ALS").apply {
                        setCallback {
                            (requireActivity() as MainActivity).hideLottie()
                            (requireActivity() as MainActivity).setupLogoutButton()
                        }
                    }

                    dialog.show(parentFragmentManager, "als_done_dialog")
                }
            }

            if (blsDone) {
                Log.d("TRAINING_COMPLETE", "BLS 전체 완료")

                if (!shownCourseDialogs.contains(2)) {   // 🔥 FIX
                    shownCourseDialogs.add(2)

                    (requireActivity() as MainActivity).showLottie()

                    val dialog = FinishCourseDialog.newInstance("BLS").apply {
                        setCallback {
                            (requireActivity() as MainActivity).hideLottie()
                            (requireActivity() as MainActivity).setupLogoutButton()
                        }
                    }

                    dialog.show(parentFragmentManager, "bls_done_dialog")
                }
            }

            if (palsDone) {
                Log.d("TRAINING_COMPLETE", "PALS 전체 완료")

                if (!shownCourseDialogs.contains(3)) {   // 🔥 FIX
                    shownCourseDialogs.add(3)

                    (requireActivity() as MainActivity).showLottie()

                    val dialog = FinishCourseDialog.newInstance("PALS Program").apply {
                        setCallback {
                            (requireActivity() as MainActivity).hideLottie()
                            (requireActivity() as MainActivity).setupLogoutButton()
                        }
                    }

                    dialog.show(parentFragmentManager, "pals_done_dialog")
                }
            }

        } else {

            if (alsDone) {
                Log.d("TRAINING_COMPLETE", "ALS 전체 완료")

                if (!shownCourseDialogs.contains(1)) {
                    shownCourseDialogs.add(1)

                    (requireActivity() as MainActivity).showLottie()

                    val dialog = CongratulationsDialog.newInstance("ALS").apply {
                        setCallback {
                            (requireActivity() as MainActivity).hideLottie()
                        }
                    }

                    dialog.show(parentFragmentManager, "als_done_dialog")
                }
            }

            if (blsDone) {
                Log.d("TRAINING_COMPLETE", "BLS 전체 완료")

                if (!shownCourseDialogs.contains(2)) {   // 🔥 FIX
                    shownCourseDialogs.add(2)

                    (requireActivity() as MainActivity).showLottie()

                    val dialog = CongratulationsDialog.newInstance("BLS").apply {
                        setCallback {
                            (requireActivity() as MainActivity).hideLottie()
                        }
                    }

                    dialog.show(parentFragmentManager, "bls_done_dialog")
                }
            }

            if (palsDone) {
                Log.d("TRAINING_COMPLETE", "PALS 전체 완료")

                if (!shownCourseDialogs.contains(3)) {   // 🔥 FIX
                    shownCourseDialogs.add(3)

                    (requireActivity() as MainActivity).showLottie()

                    val dialog = CongratulationsDialog.newInstance("PALS Program").apply {
                        setCallback {
                            (requireActivity() as MainActivity).hideLottie()
                        }
                    }

                    dialog.show(parentFragmentManager, "pals_done_dialog")
                }
            }

        }
    }

    private fun mockBluetoothDevice(address: String, name: String): BluetoothDevice {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val device = adapter.getRemoteDevice(address)

        // 이름 강제로 세팅 (리플렉션)
        try {
            val method = device.javaClass.getMethod("setName", String::class.java)
            method.invoke(device, name)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return device
    }




    private fun observeBle() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    bleManager.scanResults.collect { devices ->
                        deviceAdapter.submitList(devices)
                        adjustSearchingPosition()
                    }
                }

                launch {
                    bleManager.connectionState.collect { stateMap ->
                        connectionMap.clear()
                        connectionMap.putAll(stateMap)
                        deviceAdapter.updateConnectionState(stateMap)
                        updateTrainingStatus()
                    }
                }

                launch {
                    bleManager.connectedDevice.collect { (device, connected, type) ->
                        if (connected) {

                            Log.d("kimtest","addr == ${device.device.address} type == ${type}")
                            if (type == DeviceType.UNKNOWN) {
                                showManikinChooseDialog(device.device.address)
                                return@collect
                            }

                            deviceTypeMap[device.device.address] = type
                        } else {
                            deviceTypeMap.remove(device.device.address)
                        }
                        updateTrainingStatus()
                    }
                }
            }
        }
    }

    private fun showManikinChooseDialog(address: String) {
        val dialog = ManikinTypeChooseDialog(requireContext())

        dialog.listener = object : ManikinTypeChooseDialog.Listener {
            override fun onSelected(type: DeviceType) {
                bleManager.setManualDeviceType(address, type)
                deviceTypeMap[address] = type
                updateTrainingStatus()
            }

            override fun onCancel() {
                // 선택 안 하면 연결 끊어도 되고 그냥 둬도 됨
                Log.d("ManikinDialog", "User cancelled manikin type selection")
            }
        }

        dialog.start()
    }


    private fun updateTrainingStatus() {

        val connectedDeviceTypes = deviceTypeMap.filter { (addr, _) ->
            connectionMap[addr] == true
        }.values.toSet()

        val updated = originalItems.mapNotNull { item ->
            if (item is ContentsItem.Content) {
                val newStatus = when {
                    !connectedDeviceTypes.containsAll(item.requiredDeviceTypes) -> TrainingStatus.LOCKED
                    UserTrainingState.isCompleted(item) -> TrainingStatus.COMPLETED
                    else -> TrainingStatus.AVAILABLE
                }
                item.copy(status = newStatus)
            } else null
        }

        val splitLists = splitContentItems(updated)

        contentsAdapter.forEachIndexed { index, adapter ->
            adapter.updateItems(splitLists.getOrNull(index) ?: emptyList())
        }


    }

    fun showTooltip(anchor: View) {

        val popupView = LayoutInflater.from(anchor.context)
            .inflate(R.layout.tooltip_layout, null)


        val textView = popupView.findViewById<TextView>(R.id.tooltipText)

        // 👉 핵심: 텍스트 기준 width 측정
        val text = textView.text.toString()
        val paint1 = textView.paint
        val textWidth = paint1.measureText(text)

        val padding = 80.dpToPx()
        val finalWidth = (textWidth + padding).toInt()

        val popup = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isClippingEnabled = false
        }


        val content = popupView.findViewById<View>(R.id.contentBox)

        content.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        val radius = 30f
        val blur = 20f
        val dx = -4f
        val dy = 4f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(
                blur,
                dx,
                dy,
                Color.parseColor("#1A000000")
            )
        }

        content.background = object : Drawable() {

            override fun draw(canvas: Canvas) {

                val rect = RectF(
                    blur,
                    blur,
                    bounds.width() - blur,
                    bounds.height() - blur
                )

                canvas.drawRoundRect(rect, radius, radius, paint)
            }

            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: ColorFilter?) {}
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }

        anchor.post {

            val displayMetrics = anchor.context.resources.displayMetrics
            val maxWidth = (displayMetrics.widthPixels * 0.9).toInt()

            // 👉 layout 기준으로 제대로 측정
            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.UNSPECIFIED
            )

            val popupWidth = popupView.measuredWidth
            val popupHeight = popupView.measuredHeight

            val popup = PopupWindow(
                popupView,
                popupWidth,   // 👉 layout 그대로 반영
                popupHeight,
                true
            ).apply {
                isOutsideTouchable = true
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                isClippingEnabled = false
            }

            val location = IntArray(2)
            anchor.getLocationOnScreen(location)

            val anchorX = location[0]
            val anchorY = location[1]

            val margin = 10.dpToPx()
            val blur = 20f

            val x = (anchorX - blur).toInt()
            val y = (anchorY - popupHeight - margin + blur).toInt()

            popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
        }
    }

    fun Int.dpToPx(): Int =
        (this * Resources.getSystem().displayMetrics.density).toInt()

    private fun splitContentItems(items: List<ContentsItem.Content>): List<List<ContentsItem.Content>> {

        val als = items.filter { it.certType == 1 }
        val bls = items.filter { it.certType == 2 }
        val pals = items.filter { it.certType == 3 }

        return listOf(als, bls, pals)
    }



    override fun onResume() {
        super.onResume()
        updateTrainingStatus()
        checkCourseCompletion()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
