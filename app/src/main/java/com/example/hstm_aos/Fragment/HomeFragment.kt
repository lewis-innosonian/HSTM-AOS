// HomeFragment.kt
package com.example.hstm_aos.Fragment

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.*
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
    private val ALScontents = mutableMapOf<Int, OpenSkill>()
    private val BLScontents = mutableMapOf<Int, OpenSkill>()
    private val PALScontents = mutableMapOf<Int, OpenSkill>()
    //
    private lateinit var howtoConnectDialog: HowToConnectDialog

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)


        if (UserInfoManager.getVoiceGuide(requireContext())) {
            binding.voiceGuideSV.isChecked = true
        } else {
            binding.voiceGuideSV.isChecked = false
        }

        binding.howToDisconnectLayout.setOnClickListener{
            howtoConnectDialog = HowToConnectDialog.newInstance()
            howtoConnectDialog.show(parentFragmentManager, "howToConnectDialog")
        }

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

        skills.forEach { skill ->
            skill.Skill_Type_id?.let { id ->
                when (skill.Cert_Type) {
                    1 -> ALScontents[id] = skill
                    2 -> BLScontents[id] = skill
                    3 -> PALScontents[id] = skill
                }
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
        ALScontents.values.forEach { skill ->
            skill.Skill_Type_id?.let { id ->
                // 원래 Due_Date 문자열
                val originalDate = skill.Due_Date
                // 포맷 변환
                val formattedDate = originalDate?.let {
                    try {
                        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        val date = parser.parse(it)
                        val formatter = SimpleDateFormat("MMM dd. yyyy", Locale.US)
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
                        passing_Score = skill.Passing_Score ?:0
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
        BLScontents.values.forEach { skill ->
            skill.Skill_Type_id?.let { id ->
                val originalDate = skill.Due_Date
                // 포맷 변환
                val formattedDate = originalDate?.let {
                    try {
                        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        val date = parser.parse(it)
                        val formatter = SimpleDateFormat("MMM dd. yyyy", Locale.US)
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
                        passing_Score = skill.Passing_Score ?:0
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
        PALScontents.values.forEach { skill ->
            skill.Skill_Type_id?.let { id ->

                val originalDate = skill.Due_Date
                // 포맷 변환
                val formattedDate = originalDate?.let {
                    try {
                        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                        val date = parser.parse(it)
                        val formatter = SimpleDateFormat("MMM dd. yyyy", Locale.US)
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
                        passing_Score = skill.Passing_Score ?:0
                    )
                )
            }
        }

//        contentsAdapter.updateItems(originalItems)
        updateTrainingStatus()
    }


    private fun createDummyDevices(): List<BleDevice> {
        return listOf(
            BleDevice(
                device = mockBluetoothDevice("00:11:22:33:44:01", "Manikin PRO"),
                rssi = -45,
                isConnected = true,
                firmwareVersion = "v1.2.0",
                deviceType = DeviceType.PRO
            ),
            BleDevice(
                device = mockBluetoothDevice("00:11:22:33:44:02", "Manikin BABY"),
                rssi = -60,
                isConnected = false,
                firmwareVersion = "v1.1.3",
                deviceType = DeviceType.BABY
            ),
            BleDevice(
                device = mockBluetoothDevice("00:11:22:33:44:03", "Manikin CHILD"),
                rssi = -70,
                isConnected = false,
                firmwareVersion = null,
                deviceType = DeviceType.CHILD
            ),
            // 🔍 Searching 상태용 (타입 모름)
            BleDevice(
                device = mockBluetoothDevice("00:11:22:33:44:04", "Searching..."),
                rssi = -90,
                isConnected = false,
                firmwareVersion = null,
                deviceType = DeviceType.UNKNOWN
            )

        )
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

    private fun splitContentItems(items: List<ContentsItem.Content>): List<List<ContentsItem.Content>> {
        val size = items.size / 3

        return listOf(
            items.subList(0, size),
            items.subList(size, size * 2),
            items.subList(size * 2, items.size)
        )
    }



    override fun onResume() {
        super.onResume()
        updateTrainingStatus()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
