package com.example.hstm_aos.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.DeviceAdapter
import com.example.hstm_aos.MainActivity
import com.example.hstm_aos.R
import com.example.hstm_aos.ble.BleManager
import com.example.hstm_aos.ble.DeviceType
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var bleManager: BleManager
    private lateinit var adapter: DeviceAdapter
    private val deviceTypeMap = mutableMapOf<String, DeviceType>()

    @SuppressLint("UnsafeRepeatOnLifecycleDetector")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        bleManager = (requireActivity() as MainActivity).bleManager

        adapter = DeviceAdapter(mutableListOf()) { device ->
            bleManager.toggleConnection(device)
        }

        val rv = view.findViewById<RecyclerView>(R.id.deviceRecyclerView)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleManager.receivedPackets.collect { (address, data) ->
                    val type = deviceTypeMap[address] ?: DeviceType.UNKNOWN
                    handleBleResponse(address, type, data)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleManager.connectedDevice.collect { (address, connected, type) ->
                    if (!connected) return@collect
                    deviceTypeMap[address] = type
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    bleManager.scanResults.collect { devices ->
                        adapter.submitList(devices)
                    }
                }
                launch {
                    bleManager.connectionState.collect { stateMap ->
                        adapter.updateConnectionState(stateMap)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        bleManager.stopScan()
//        bleManager.disconnectAll()
    }

    private fun handleBleResponse(address: String, type: DeviceType, data: ByteArray) {


        when (type) {
            DeviceType.AED -> {
                Log.d("BLE_TYPE", "AED")
            }

            DeviceType.BABY -> {
                Log.d("BLE_TYPE", "BABY")
            }

            DeviceType.PRO -> {
                Log.d("BLE_TYPE", "PRO")
            }

            else -> {}
        }

        val versionBytes = data.copyOfRange(1, 9)
        val versionString = versionBytes.toString(Charsets.UTF_8).trim()

        adapter.updateVersion(address, versionString)

    }


}
