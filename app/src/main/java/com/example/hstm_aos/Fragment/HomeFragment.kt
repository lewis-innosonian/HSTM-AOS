package com.example.hstm_aos.Fragment

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hstm_aos.*
import com.example.hstm_aos.ble.BleManager
import com.example.hstm_aos.ble.DeviceType
import com.example.hstm_aos.ble.TrainingType
import com.example.hstm_aos.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch
import java.io.Serializable

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var bleManager: BleManager
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var contentsAdapter: ContentsAdapter
    private val deviceTypeMap = mutableMapOf<String, DeviceType>()
    private val connectionMap = mutableMapOf<String, Boolean>()
    private val originalItems = mutableListOf<ContentsItem>()

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeBinding.bind(view)

        bleManager = (requireActivity().application as MainApplication).bleManager

        deviceAdapter = DeviceAdapter(mutableListOf()) { device ->
            bleManager.toggleConnection(device)
        }

        binding.deviceRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }

        setupContents()
        contentsAdapter = ContentsAdapter(originalItems.toMutableList()) { contentItem ->
            val intent = android.content.Intent(requireContext(), TrainingActivity::class.java).apply {
                putExtra("contentItem", contentItem as Serializable)
            }
            requireContext().startActivity(intent)
        }


        binding.rightRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contentsAdapter
            addItemDecoration(StickyHeaderDecoration(contentsAdapter))
        }
        restoreBleState()
        observeBle()
    }

    private fun restoreBleState() {
        connectionMap.clear()
        connectionMap.putAll(bleManager.getCurrentConnectionMap())

        deviceTypeMap.clear()
        deviceTypeMap.putAll(bleManager.getCurrentDeviceTypeMap())

        updateTrainingStatus()
    }

    //TODO 더미데이터..TrainingType도 정의해서 Training화면으로 전달해야함
    private fun setupContents() {
        originalItems.clear()
        originalItems.addAll(
            listOf(
                ContentsItem.Header(
                    iconRes = R.drawable.inno_header_title_icon,
                    title = "ALS",
                    createdAt = "Due: Feb 29. 2028"
                ),
                ContentsItem.Content(
                    text = "Adult Compressions",
                    requiredDeviceTypes = setOf(DeviceType.PRO),
                    status = TrainingStatus.COMPLETED,
                    trainingType = setOf(TrainingType.CCO)
                ),
                ContentsItem.Content(
                    text = "Adult Compressions with AED-Trainer",
                    requiredDeviceTypes = setOf(DeviceType.PRO, DeviceType.AED),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CCO, TrainingType.AED),
                ),
                ContentsItem.Content(
                    text = "Adult Ventilation",
                    requiredDeviceTypes = setOf(DeviceType.PRO),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.VO)
                ),
                ContentsItem.Content(
                    text = "Adult 1-Provider CPR",
                    requiredDeviceTypes = setOf(DeviceType.PRO),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CPR)
                ),
                ContentsItem.Content(
                    text = "Adult 1-Provider CPR with AED-Trainer",
                    requiredDeviceTypes = setOf(DeviceType.PRO, DeviceType.AED),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CPR, TrainingType.AED)
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CCO)
                ),
                ContentsItem.Content(
                    text = "Infant Ventilation",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.VO)
                ),
                ContentsItem.Content(
                    text = "Infant 1-Provider CPR",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CPR)
                ),
                ContentsItem.Header(
                    iconRes = R.drawable.inno_header_title_icon,
                    title = "BLS",
                    createdAt = "Due: Feb 29. 2028"
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CCO)
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CCO)
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CCO)
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CCO)
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CCO)
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CCO)
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CCO)
                ),
                ContentsItem.Header(
                    iconRes = R.drawable.inno_header_title_icon,
                    title = "PALS Program",
                    createdAt = "Due: Feb 29. 2028"
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE,
                    trainingType = setOf(TrainingType.CCO)
                )
            )
        )
    }

    private fun observeBle() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    bleManager.scanResults.collect { devices ->
                        deviceAdapter.submitList(devices)
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
                    bleManager.connectedDevice.collect { (address, connected, type) ->
                        if (connected) {
                            deviceTypeMap[address] = type
                            CustomToast(requireActivity()).show(
                                message = "Device connected",
                                iconRes = R.drawable.inno_toast_connect_icon,
                                bgColor = Color.parseColor("#1AAF0D")
                            )
                        } else {
                            deviceTypeMap.remove(address)
                            CustomToast(requireActivity()).show(
                                message = "Device disconnected",
                                iconRes = R.drawable.inno_disconnect_icon,
                                bgColor = Color.parseColor("#FD1708")
                            )
                        }

                        updateTrainingStatus()
                    }
                }
            }
        }
    }

    private fun updateTrainingStatus() {
        val connectedDeviceTypes = deviceTypeMap.filter { (addr, _) ->
            connectionMap[addr] == true
        }.values.toSet()

        val updated = originalItems.map { item ->
            if (item is ContentsItem.Content) {
                val newStatus = if (!connectedDeviceTypes.containsAll(item.requiredDeviceTypes)) {
                    TrainingStatus.LOCKED
                } else if (item.status == TrainingStatus.COMPLETED) {
                    TrainingStatus.COMPLETED
                } else {
                    TrainingStatus.AVAILABLE
                }
                item.copy(status = newStatus)
            } else item
        }

        contentsAdapter.updateItems(updated)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
