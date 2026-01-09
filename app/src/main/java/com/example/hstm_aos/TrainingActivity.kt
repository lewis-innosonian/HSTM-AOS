package com.example.hstm_aos

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.hstm_aos.Fragment.TrainingFragment
import com.example.hstm_aos.ble.BleDevice
import com.example.hstm_aos.ble.BleManager
import com.example.hstm_aos.ble.DeviceType
import com.example.hstm_aos.databinding.ActivityTrainingBinding
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


class TrainingActivity : AppCompatActivity() {

    private val _blePacketFlow = MutableSharedFlow<Pair<String, ByteArray>>(replay = 0)
    val blePacketFlow = _blePacketFlow.asSharedFlow()

    private lateinit var binding: ActivityTrainingBinding
    private lateinit var bleManager: BleManager
    private var requiredTypes: Set<DeviceType> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_training)
        bleManager = (application as MainApplication).bleManager

        val contentItem = intent.getSerializableExtra("contentItem") as? ContentsItem.Content
        contentItem?.let {
            binding.trainingTypeTextView.text = it.trainingType.toString()
            requiredTypes = it.requiredDeviceTypes
        }

        lifecycleScope.launch {
            bleManager.receivedPackets.collect { (address, data) ->
                _blePacketFlow.emit(address to data)
            }
        }

        binding.startStopButton.setOnClickListener {
            val deviceToUse = bleManager.getCurrentConnectedDevices().firstOrNull { device ->
                requiredTypes.contains(device.deviceType)
            }

            deviceToUse?.let { device ->
                val packet = byteArrayOf(0x51, 0x00, 0x00)
                bleManager.sendPacketToDevice(device, packet)
            } ?: Log.w("kimtest", "no matching device")
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, TrainingFragment())
            .commit()
    }
}
