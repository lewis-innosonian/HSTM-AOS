package com.example.hstm_aos.ble

import android.bluetooth.BluetoothDevice
import java.io.Serializable

data class BleDevice(
    val device: BluetoothDevice,
    var rssi: Int,
    var isConnected: Boolean = false,
    var firmwareVersion: String? = null,
    var deviceType : DeviceType? = null
)

enum class DeviceType : Serializable {
    BABY,
    PRO,
    CHILD,
    AED,
    UNKNOWN
}


enum class TrainingType : Serializable {
    CPR,
    CCO,
    VO,
    TWORESCUER,
    AED
}