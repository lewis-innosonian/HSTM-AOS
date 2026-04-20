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
import android.graphics.Color
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.example.hstm_aos.customview.CustomToast
import com.example.hstm_aos.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class BleManager(private var context: Context) {

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

    private val OTA_SERVICE_UUID =
        UUID.fromString("00001234-0000-1000-8000-00805f9b34fb")

    private val OTA_CONTROL_UUID =
        UUID.fromString("00009a14-0000-1000-8000-00805f9b34fb")

    private val OTA_DATA_UUID =
        UUID.fromString("00005678-0000-1000-8000-00805f9b34fb")

    private val OTA_STATUS_UUID =
        UUID.fromString("00009a15-0000-1000-8000-00805f9b34fb")

    private val PSK_KEY = byteArrayOf(
        0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,
        0x08,0x09,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,
        0x10,0x11,0x12,0x13,0x14,0x15,0x16,0x17,
        0x18,0x19,0x1A,0x1B,0x1C,0x1D,0x1E,0x1F
    )
    @Volatile
    private var otaStatus: Int = -1
    private var writtenBytes = 0
    private var totalBytes = 0
    private var otaChunks: List<ByteArray> = emptyList()
    private var otaIndex = 0

    /* =================================================== */



    private val writeQueue: ArrayDeque<Pair<BluetoothGatt, ByteArray>> = ArrayDeque()
//    val item = writeQueue.removeFirstOrNull()

    private var isWriting = false

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
        MutableSharedFlow<Triple<BleDevice, Boolean, DeviceType>>(extraBufferCapacity = 5)

    val connectedDevice: SharedFlow<Triple<BleDevice, Boolean, DeviceType>> =
        _connectedDevice.asSharedFlow()

    private val _receivedPackets =
        MutableSharedFlow<Pair<String, ByteArray>>(extraBufferCapacity = 10)
    val receivedPackets: SharedFlow<Pair<String, ByteArray>> =
        _receivedPackets.asSharedFlow()

    private val userDisconnectMap = mutableMapOf<String, Boolean>()


    private val toastQueue: ArrayDeque<() -> Unit> = ArrayDeque()
    private var isShowingToast = false



    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(type: Int, result: ScanResult) {
            val device = result.device ?: return
            val address = device.address
            val name = device.name ?: result.scanRecord?.deviceName ?: ""

            val isMatched = name.contains("Brayden", true)


            val hasService = result.scanRecord?.serviceUuids?.any {
                it.uuid == FE59_UUID || it.uuid == AED_SERVICE_UUID
            } == true

            if (!isMatched && !hasService) return

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

    private fun emitConnectionState(device: BleDevice, connected: Boolean) {
        _connectionState.value = _connectionState.value.toMutableMap().apply {
            this[device.device.address] = connected
        }

        val device = deviceMap[device.device.address]
        device?.isConnected = connected

        if (connected && device?.deviceType != null) {
            _connectedDevice.tryEmit(
                Triple(device, true, device.deviceType!!)
            )
        }

        emitScanResults()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun toggleConnection(device: BleDevice) {
        val address = device.device.address
        if (gattMap.containsKey(address)) {
            writeQueue.clear()
            isWriting = false

            userDisconnectMap[address] = true
            gattMap[address]?.disconnect()
            return
        }
        device.device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun initUart(gatt: BluetoothGatt, deviceType: DeviceType) {
        val service = gatt.getService(UART_SERVICE_UUID) ?: return
        val rxChar = service.getCharacteristic(UART_RX_UUID)

        gatt.setCharacteristicNotification(rxChar, true)
        val cccd = rxChar.getDescriptor(CCCD_UUID)
        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(cccd)
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun setManualDeviceType(address: String, type: DeviceType) {
        deviceMap[address]?.deviceType = type

        gattMap[address]?.let { gatt ->
            initUart(gatt, type)
        }
    }


    private val gattCallback = object : BluetoothGattCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address
            val device = deviceMap[gatt.device.address]
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    gattMap[address] = gatt

                    gatt.requestMtu(512)

//                    gatt.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    val userDisconnected = userDisconnectMap[address] ?: false
                    userDisconnectMap.remove(address)

                    gattMap.remove(address)
                    gatt?.disconnect()
                    gatt?.close()
                    emitConnectionState(device!!, false)
                    if (!userDisconnected) {
                        showToast(
                            "${gatt.device.name} disconnected",
                            R.drawable.inno_disconnect_icon,
                            "#FD1708"
                        )
                    }
                    _connectedDevice.tryEmit(Triple(device!!, false, deviceMap[address]?.deviceType ?: DeviceType.UNKNOWN))
                }
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "MTU 변경 성공: $mtu")
            } else {
                Log.d("BLE", "MTU 변경 실패")
            }

            gatt.discoverServices()
        }
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val address = gatt.device.address
            val type = detectDeviceType(gatt)

            deviceMap[address]?.deviceType = type

            Log.d("BLE_TYPE", "${gatt.device.name} → $type")

            initUart(gatt, type)
        }


        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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
//                DeviceType.PRO -> byteArrayOf(0x51, 0x00, 0x00)
                DeviceType.UNKNOWN -> byteArrayOf(0x51, 0x00, 0x00)

                else -> return
            }

            sendUart(gatt, dataToSend)

            Log.d("BLE_SEND", "Init packet → $deviceType")
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid == OTA_STATUS_UUID) {
                val value = characteristic.value[0].toInt()
                otaStatus = value
                Log.d("OTA", "STATUS = $value")
            }
        }


        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid != UART_RX_UUID) return

            val address = gatt.device.address
            val data = characteristic.value

            val device = deviceMap[address]


            val headerPacket = data.sliceArray(0 until 1)
            val header = headerPacket.joinToString(", ") { "%02X".format(it) }

            if (header == "A2" || header == "B1") {
                val versionBytes = data.copyOfRange(1, 9)
                val versionString = versionBytes.toString(Charsets.UTF_8).trim()
                device?.firmwareVersion = versionString

                showToast(
                    "Device connected",
                    R.drawable.inno_toast_connect_icon,
                    "#1AAF0D"
                )

                if (device != null) {
                    emitConnectionState(device, true)
                }
            }

            _receivedPackets.tryEmit(address to data)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (characteristic.uuid != OTA_DATA_UUID) return

            if (status == BluetoothGatt.GATT_SUCCESS) {
                val size = characteristic.value?.size ?: 0
                writtenBytes += size

                val progress = (writtenBytes * 100) / totalBytes
                Log.d("OTA", "REAL Progress: $progress%")

                scope.launch {
                    delay(10)
                    writeNextChunk(gatt)
                }

            } else {
                Log.e("OTA", "Write faail: $status")
            }
        }
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun enqueueWrite(gatt: BluetoothGatt, data: ByteArray) {
        writeQueue.add(gatt to data)
        if (!isWriting) {
            writeNext()
        }
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun writeNext() {
        val item = writeQueue.removeFirstOrNull() ?: run {
            isWriting = false
            return
        }

        val (gatt, data) = item

        val service = gatt.getService(OTA_SERVICE_UUID) ?: return
        val char = service.getCharacteristic(OTA_DATA_UUID) ?: return

        isWriting = true

        char.value = data
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

        val success = gatt.writeCharacteristic(char)

        if (!success) {
            isWriting = false
            writeNext()
        }
    }


    fun getCurrentConnectedDevices(): List<BleDevice> {
        return deviceMap.values.filter { it.isConnected && it.deviceType != null }
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendPacketToDevice(device: BleDevice, data: ByteArray) {
        device.device.address.let { address ->
            gattMap[address]?.let { gatt ->
                sendUart(gatt, data)
            } ?: Log.d("kimtest", "Device $address not connected")
        }
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun sendUart(gatt: BluetoothGatt, data: ByteArray) {
        val service = gatt.getService(UART_SERVICE_UUID) ?: return
        val txChar = service.getCharacteristic(UART_TX_UUID) ?: return

        txChar.value = data
        txChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
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
            null,
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build(),
            scanCallback
        )
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun detectDeviceType(gatt: BluetoothGatt): DeviceType {
        val hasFe59 = gatt.getService(FE59_UUID) != null
        val hasAedService = gatt.getService(AED_SERVICE_UUID) != null

        // 여기서 child 필요할때 이름으로 분기해야될듯..?
        return when {
            hasAedService -> DeviceType.AED
            hasFe59 && gatt.device.name.contains("pro", ignoreCase = true) ->
                DeviceType.UNKNOWN
            hasFe59 -> DeviceType.BABY
            else -> DeviceType.UNKNOWN
        }
    }


    fun getCurrentConnectionMap(): Map<String, Boolean> {
        return _connectionState.value
    }

    fun getCurrentDeviceTypeMap(): Map<String, DeviceType> {
        return deviceMap
            .filter { it.value.isConnected && it.value.deviceType != null }
            .mapValues { it.value.deviceType!! }
    }

    private fun showToast(message: String, icon: Int, color: String) {
        val activity = context as? AppCompatActivity ?: return

        activity.runOnUiThread {
            toastQueue.add {
                CustomToast(activity).show(
                    message = message,
                    iconRes = icon,
                    bgColor = Color.parseColor(color)
                )
            }

            if (!isShowingToast) {
                showNextToast()
            }
        }
    }

    private fun showNextToast() {
        val activity = context as? AppCompatActivity ?: return

        val next = toastQueue.removeFirstOrNull() ?: run {
            isShowingToast = false
            return
        }

        isShowingToast = true
        next()

        activity.window.decorView.postDelayed({
            showNextToast()
        }, 2000)
    }

    fun attachActivity(activity: AppCompatActivity) {
        this.context = activity
    }

    //m161 firmwareUpdate

    private fun encryptChunk(plain: ByteArray): ByteArray {
        val iv = ByteArray(12)
        java.security.SecureRandom().nextBytes(iv)

        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = javax.crypto.spec.SecretKeySpec(PSK_KEY, "AES")
        val spec = javax.crypto.spec.GCMParameterSpec(128, iv)

        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, spec)
        val encrypted = cipher.doFinal(plain)

        return iv + encrypted
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendDfuTrigger(gatt: BluetoothGatt) {
        val service = gatt.getService(UART_SERVICE_UUID) ?: return
        val char = service.getCharacteristic(UART_TX_UUID) ?: return

        val data = byteArrayOf(0x51, 0x01, 0x01)

        char.value = data
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        gatt.writeCharacteristic(char)

        Log.d("OTA", "DFU Trigger sent")
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendStart(gatt: BluetoothGatt, total: Int) {
        val service = gatt.getService(OTA_SERVICE_UUID) ?: return
        val char = service.getCharacteristic(OTA_CONTROL_UUID) ?: return

        val buffer = java.nio.ByteBuffer.allocate(5)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)

        buffer.put(0x01)
        buffer.putInt(total)

        char.value = buffer.array()
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        gatt.writeCharacteristic(char)

        Log.d("OTA", "START sent total=$total")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendEnd(gatt: BluetoothGatt) {
        val service = gatt.getService(OTA_SERVICE_UUID) ?: return
        val char = service.getCharacteristic(OTA_CONTROL_UUID) ?: return

        char.value = byteArrayOf(0x02)
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        gatt.writeCharacteristic(char)

        Log.d("OTA", "END sent")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun waitForReady(gatt: BluetoothGatt): Boolean {

        repeat(20) {
            readOtaStatus(gatt)
            delay(300)

            if (otaStatus == 0x01) { // READY
                Log.d("OTA", "Device READY")
                return true
            }
        }

        Log.e("OTA", "Device NOT READY")
        return false
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun startOta(address: String, firmware: ByteArray) {
        val gatt = gattMap[address] ?: return
        otaStatus = -1

        scope.launch {
            try {
                val chunkSize = 460

                val numChunks = (firmware.size + chunkSize - 1) / chunkSize
                val encryptedTotal = firmware.size + numChunks * (12 + 16)

                writtenBytes = 0
                totalBytes = encryptedTotal

                Log.d("OTA", "START : total=$encryptedTotal")

                sendDfuTrigger(gatt)
                delay(500)

                sendStart(gatt, encryptedTotal)
                delay(500)

                val ready = waitForReady(gatt)
                if (!ready) {
                    Log.e("OTA", "Abort: device not ready")
                    return@launch
                }

                otaChunks = mutableListOf<ByteArray>().apply {
                    for (offset in 0 until firmware.size step chunkSize) {
                        val chunk = firmware.copyOfRange(
                            offset,
                            minOf(offset + chunkSize, firmware.size)
                        )
                        add(encryptChunk(chunk))
                    }
                }

                otaIndex = 0

                Log.d("OTA", "Chunk count = ${otaChunks.size}")

                writeNextChunk(gatt)

            } catch (e: Exception) {
                Log.e("OTA", "Error: ${e.message}")
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun writeNextChunk(gatt: BluetoothGatt) {

        if (otaIndex >= otaChunks.size) {
            Log.d("OTA", "All chunks sent : END!!!!!!")

            sendEnd(gatt)

            scope.launch {
                delay(1000)

                repeat(30) {
                    try {
                        readOtaStatus(gatt)
                        delay(1000)

                        if (otaStatus == 0x03) {
                            Log.d("OTA", "OTA SUCCESS")
                            return@launch
                        }

                        if (otaStatus == 0x04) {
                            Log.e("OTA", "OTA ERROR")
                            return@launch
                        }

                    } catch (e: Exception) {
                        Log.d("OTA", "Device disconnected")
                        return@launch
                    }
                }

                Log.d("OTA", "OTA finish")
            }

            return
        }

        val data = otaChunks[otaIndex]
        otaIndex++

        val service = gatt.getService(OTA_SERVICE_UUID) ?: return
        val char = service.getCharacteristic(OTA_DATA_UUID) ?: return

        char.value = data
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

        val success = gatt.writeCharacteristic(char)

        if (!success) {
            otaIndex--
            writeNextChunk(gatt)
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun readOtaStatus(gatt: BluetoothGatt) {
        val service = gatt.getService(OTA_SERVICE_UUID) ?: return
        val char = service.getCharacteristic(OTA_STATUS_UUID) ?: return

        gatt.readCharacteristic(char)
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        scanner.stopScan(scanCallback)
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnectAll() {
        gattMap.values.forEach { it.disconnect() }
    }


    fun getDeviceByAddress(address: String): BleDevice? {
        return deviceMap[address]
    }

}
