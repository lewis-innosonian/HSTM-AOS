package com.example.hstm_aos.ble

import android.bluetooth.BluetoothDevice

data class BleDevice(
    val device: BluetoothDevice,
    var rssi: Int,
    var isConnected: Boolean = false,
    var firmwareVersion: String? = null,
    var deviceType : DeviceType? = null
)

enum class DeviceType {
    BABY,
    PRO,
    AED,
    UNKNOWN
}