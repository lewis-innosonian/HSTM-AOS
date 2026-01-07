package com.example.hstm_aos.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hstm_aos.*
import com.example.hstm_aos.ble.BleManager
import com.example.hstm_aos.ble.DeviceType
import com.example.hstm_aos.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

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

        bleManager = (requireActivity() as MainActivity).bleManager

        deviceAdapter = DeviceAdapter(mutableListOf()) { device ->
            bleManager.toggleConnection(device)
        }

        binding.deviceRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }

        setupContents()
        contentsAdapter = ContentsAdapter(originalItems.toMutableList())

        binding.rightRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contentsAdapter
            addItemDecoration(StickyHeaderDecoration(contentsAdapter))
        }

        observeBle()
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
                    status = TrainingStatus.COMPLETED
                ),
                ContentsItem.Content(
                    text = "Adult Compressions with AED-Trainer",
                    requiredDeviceTypes = setOf(DeviceType.PRO, DeviceType.AED),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Content(
                    text = "Adult Ventilation",
                    requiredDeviceTypes = setOf(DeviceType.PRO),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Content(
                    text = "Adult 1-Provider CPR",
                    requiredDeviceTypes = setOf(DeviceType.PRO),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Content(
                    text = "Adult 1-Provider CPR with AED-Trainer",
                    requiredDeviceTypes = setOf(DeviceType.PRO, DeviceType.AED),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Content(
                    text = "Infant Ventilation",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Content(
                    text = "Infant 1-Provider CPR",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Header(
                    iconRes = R.drawable.inno_header_title_icon,
                    title = "BLS",
                    createdAt = "Due: Feb 29. 2028"
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE
                ),
                ContentsItem.Header(
                    iconRes = R.drawable.inno_header_title_icon,
                    title = "PALS Program",
                    createdAt = "Due: Feb 29. 2028"
                ),
                ContentsItem.Content(
                    text = "Infant Compressions",
                    requiredDeviceTypes = setOf(DeviceType.BABY),
                    status = TrainingStatus.AVAILABLE
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
                        } else {
                            deviceTypeMap.remove(address)
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
