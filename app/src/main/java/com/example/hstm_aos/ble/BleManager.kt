package com.example.hstm_aos.ble

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BleManager(private val context: Context) {

    private val FE59_UUID =
        UUID.fromString("0000fe59-0000-1000-8000-00805f9b34fb")

    private val AED_SERVICE_UUID =
        UUID.fromString("ef680100-9b35-4933-9b10-52ffa9740042")

    /* ==================================================== */

    private val UART_SERVICE_UUID =
        UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")

    private val UART_TX_UUID =
        UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")

    private val UART_RX_UUID =
        UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

    private val CCCD_UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /* =================================================== */

    private val adapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val scanner = adapter.bluetoothLeScanner
    private val scope = CoroutineScope(Dispatchers.Main)

    private val deviceMap = ConcurrentHashMap<String, BleDevice>()
    private val gattMap = ConcurrentHashMap<String, BluetoothGatt>()


    private val _scanResults = MutableStateFlow<List<BleDevice>>(emptyList())
    val scanResults: StateFlow<List<BleDevice>> = _scanResults.asStateFlow()

    private val _connectionState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val connectionState: StateFlow<Map<String, Boolean>> = _connectionState.asStateFlow()

    private val _connectedDevice =
        MutableSharedFlow<Triple<String, Boolean, DeviceType>>(extraBufferCapacity = 5)

    val connectedDevice: SharedFlow<Triple<String, Boolean, DeviceType>> =
        _connectedDevice.asSharedFlow()

    private val _receivedPackets =
        MutableSharedFlow<Pair<String, ByteArray>>(extraBufferCapacity = 10)
    val receivedPackets: SharedFlow<Pair<String, ByteArray>> =
        _receivedPackets.asSharedFlow()

    /* ====================================== */

    private val filters = listOf(
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString("0000180a-0000-1000-8000-00805f9b34fb"))
            .build(),
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString("00000001-0000-1000-8000-00805f9b34fb"))
            .build(),
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString("0000fe59-0000-1000-8000-00805f9b34fb"))
            .build()
    )


    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(type: Int, result: ScanResult) {
            val device = result.device ?: return
            val address = device.address

            val item = deviceMap[address]
            if (item == null) {
                deviceMap[address] = BleDevice(
                    device = device,
                    rssi = result.rssi,
                    isConnected = gattMap.containsKey(address)
                )
            } else {
                item.rssi = result.rssi
            }
            emitScanResults()
        }
    }

    private fun emitScanResults() {
        _scanResults.value = deviceMap.values.toList()
    }

    private fun emitConnectionState(address: String, connected: Boolean) {
        _connectionState.value = _connectionState.value.toMutableMap().apply {
            this[address] = connected
        }

        val device = deviceMap[address]
        device?.isConnected = connected

        if (connected && device?.deviceType != null) {
            _connectedDevice.tryEmit(
                Triple(address, true, device.deviceType!!)
            )
        }

        emitScanResults()
    }


    fun toggleConnection(device: BleDevice) {
        val address = device.device.address

        if (gattMap.containsKey(address)) {
            gattMap[address]?.disconnect()
            return
        }

        device.device.connectGatt(
            context,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
    }


    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            val address = gatt.device.address

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gattMap[address] = gatt

                    gatt.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    gattMap.remove(address)
                    gatt.close()
                    emitConnectionState(address, false)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val deviceName = gatt.device.name ?: ""

            val type = detectDeviceType(gatt)
            val address = gatt.device.address

            deviceMap[address]?.deviceType = type

            Log.d("BLE_TYPE", "${gatt.device.name} → $type")

            val service = gatt.getService(UART_SERVICE_UUID) ?: return

            val rxChar = service.getCharacteristic(UART_RX_UUID)
            gatt.setCharacteristicNotification(rxChar, true)

            val cccd = rxChar.getDescriptor(CCCD_UUID)
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(cccd)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.uuid != CCCD_UUID) return
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val address = gatt.device.address
            val deviceType = deviceMap[address]?.deviceType ?: DeviceType.UNKNOWN

            val dataToSend = when (deviceType) {
                DeviceType.AED -> byteArrayOf(0x61, 0x00)
                DeviceType.BABY,
                DeviceType.PRO -> byteArrayOf(0x51, 0x00, 0x00)

                else -> return
            }

            sendUart(gatt, dataToSend)

            Log.d("BLE_SEND", "Init packet → $deviceType")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid != UART_RX_UUID) return

            val address = gatt.device.address
            val data = characteristic.value

            val headerPacket = data.sliceArray(0 until 1)
            val header = headerPacket.joinToString(", ") { "%02X".format(it) }

            if (header == "A2" || header == "B1") {
                emitConnectionState(address, true)
            }

            _receivedPackets.tryEmit(address to data)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {

        }
    }


    fun sendUart(gatt: BluetoothGatt, data: ByteArray) {
        val service = gatt.getService(UART_SERVICE_UUID) ?: return
        val txChar = service.getCharacteristic(UART_TX_UUID) ?: return

        txChar.value = data
        txChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        gatt.writeCharacteristic(txChar)
    }


    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        ]
    )
    fun startScan(clear: Boolean = false) {
        if (clear) {
            deviceMap.clear()
            emitScanResults()
        }
        scanner.startScan(
            filters,
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build(),
            scanCallback
        )
    }

    fun stopScan() {
        scanner.stopScan(scanCallback)
    }

    fun disconnectAll() {
        gattMap.values.forEach { it.disconnect() }
    }

    fun detectDeviceType(gatt: BluetoothGatt): DeviceType {
        val hasFe59 = gatt.getService(FE59_UUID) != null
        val hasAedService = gatt.getService(AED_SERVICE_UUID) != null

        // 여기서 child 필요할때 이름으로 분기해야될듯..?
        return when {
            hasAedService -> DeviceType.AED
            hasFe59 -> DeviceType.BABY
            else -> DeviceType.PRO
        }
    }

}
