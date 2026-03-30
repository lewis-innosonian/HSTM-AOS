package com.example.hstm_aos.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Parcelable
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.hstm_aos.ContentsItem
import com.example.hstm_aos.DisConnectDialog
import com.example.hstm_aos.Fragment.TrainingFragment
import com.example.hstm_aos.MainApplication
import com.example.hstm_aos.ManikinTypeChooseDialog
import com.example.hstm_aos.R
import com.example.hstm_aos.TrainingStatus
import com.example.hstm_aos.UserInfoManager
import com.example.hstm_aos.UserTrainingState
import com.example.hstm_aos.ble.BleManager
import com.example.hstm_aos.ble.DeviceType
import com.example.hstm_aos.ble.TrainingType
import com.example.hstm_aos.databinding.ActivityTrainingBinding
import com.example.hstm_aos.model.HstmResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.Serializable
import java.util.concurrent.TimeUnit

class TrainingActivity : BaseActivity(), TrainingFragment.OnTrainingFinishedListener {

    private val _blePacketFlow = MutableSharedFlow<Pair<String, ByteArray>>(replay = 0)
    val blePacketFlow = _blePacketFlow.asSharedFlow()

    private lateinit var binding: ActivityTrainingBinding
    private lateinit var bleManager: BleManager
    private var requiredTypes: Set<DeviceType> = emptySet()
    private var contentItem: ContentsItem.Content? = null

    private var timer: CountDownTimer? = null
    private var totalSeconds = 120 //120초 하드코딩
    private lateinit var trainingTypes: Set<TrainingType>
    private lateinit var mannequinType: Set<DeviceType>
    private lateinit var mannequin: String

    private lateinit var disConnectDialog: DisConnectDialog

    override fun onTrainingFinished(apiResult: HstmResponse) {
        Log.d("kimtest", "API result from fragment: $apiResult")
        contentItem?.status = TrainingStatus.COMPLETED
        resetUI(apiResult)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_training)
        bleManager = (application as MainApplication).bleManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 상태바 아이콘을 검정색으로
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        contentItem = intent.getSerializableExtra("contentItem") as? ContentsItem.Content
        contentItem?.let {
            trainingTypes = it.trainingType
            mannequinType = it.requiredDeviceTypes

            if (mannequinType.contains(DeviceType.BABY)){
                mannequin = "Infant"
            } else if (mannequinType.contains(DeviceType.CHILD)){
                mannequin = "Child"
            } else if (mannequinType.contains(DeviceType.PRO)){
                mannequin = "Adult"
            }


            if (it.trainingType.toString().contains("cpr",true)){
                binding.trainingTypeTextView.text = "${mannequin} ${contentItem?.text}"
            }else if (it.trainingType.toString().contains("cco",true)){
                binding.trainingTypeTextView.text = "${mannequin} ${contentItem?.text}"
            }else if (it.trainingType.toString().contains("vo",true)){
                binding.trainingTypeTextView.text = "${mannequin} ${contentItem?.text}"
            }

            requiredTypes = it.requiredDeviceTypes
        }

        lifecycleScope.launch {
            bleManager.receivedPackets.collect { (address, data) ->
                _blePacketFlow.emit(address to data)
            }
        }


        binding.userIDTextView.text = "${UserInfoManager.getFirstName(this)} ${UserInfoManager.getLastName(this)}, ${UserInfoManager.getOrganizations(this)}"

        lifecycleScope.launch {
            bleManager.connectedDevice.collect { (device, connected, type) ->
                if (connected) {
                    stopTraining()

                    stopTimer()
                    binding.trainingOverLayout.visibility = View.GONE

                    if (type == DeviceType.UNKNOWN) {
                        showManikinChooseDialog(device.device.address)
                        return@collect
                    }


                    binding.startStopButton.setBgColor(android.graphics.Color.parseColor("#0061F2"))

                    binding.startStopButton.text = "Practice Start"

                    binding.startStopButton.setPaddingRelative(
                        dp(46),
                        binding.startStopButton.paddingTop,
                        dp(46),
                        binding.startStopButton.paddingBottom
                    )

                    binding.timerLayout.visibility = View.GONE
                    binding.BackLayout.visibility = View.VISIBLE
                    binding.timerText.text = "00:00"

                    loadTrainingFragment()
                } else {
                    //팝업

                    stopTimer()
                    binding.trainingOverLayout.visibility = View.GONE
                    binding.startStopButton.setBgColor(android.graphics.Color.parseColor("#0061F2"))

                    binding.startStopButton.text = "Practice Start"

                    binding.startStopButton.setPaddingRelative(
                        dp(46),
                        binding.startStopButton.paddingTop,
                        dp(46),
                        binding.startStopButton.paddingBottom
                    )

                    binding.timerLayout.visibility = View.GONE
                    binding.BackLayout.visibility = View.VISIBLE
                    binding.timerText.text = "00:00"

                    loadTrainingFragment()


                    disConnectDialog = DisConnectDialog.newInstance()
                    disConnectDialog.show(supportFragmentManager, "deviceInfoDialog")

                    disConnectDialog.setCallback {
                        bleManager.toggleConnection(device)
                    }

                    disConnectDialog.setCancelCallback {
                        finish()
                    }
//                    deviceTypeMap.remove(address)
//                            CustomToast(requireActivity()).show(
//                                message = "Device disconnected",
//                                iconRes = R.drawable.inno_disconnect_icon,
//                                bgColor = Color.parseColor("#FD1708")
//                            )
                }

//                updateTrainingStatus()
            }
        }


        binding.BackLayout.setOnClickListener {
            finish()
        }


        bleManager.getCurrentConnectedDevices().forEach {
            Log.d("kimtest",
                "name=${it.device.name}, " +
                        "type=${it.deviceType}, " +
                        "rawType=${it.deviceType?.name}"
            )
        }
        Log.d("kimtest", "Required types: $requiredTypes")
        binding.startStopButton.setOnClickListener {

            val deviceToUse = bleManager.getCurrentConnectedDevices()
                .firstOrNull { device ->
                    requiredTypes.contains(device.deviceType) &&
                            device.deviceType != DeviceType.AED
                }


            if (binding.startStopButton.text == "Practice Start") {

                Log.d("kimtest", "@#@# ${deviceToUse?.deviceType?.name}")


                binding.startStopButton.text = "End Skills Check"

                binding.startStopButton.setPaddingRelative(
                    dp(37),
                    binding.startStopButton.paddingTop,
                    dp(37),
                    binding.startStopButton.paddingBottom
                )

                binding.startStopButton.setBgColor(android.graphics.Color.parseColor("#333333"))
                binding.timerLayout.visibility = View.VISIBLE
                binding.BackLayout.visibility = View.GONE

                val fragment =
                    supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                            as? TrainingFragment

                fragment?.startCountDown {
                    startTimer(totalSeconds)
                    deviceToUse?.let { device ->
                        val packet = byteArrayOf(0x54, 0x01)
                        bleManager.sendPacketToDevice(device, packet)
                    } ?: Log.w("kimtest", "no matching device")
                }

            } else {




                // Practice Stop 클릭 시 BLE 패킷 전송
                stopTraining()

                // Fragment(API) 통신 후 resetUI 호출
                val fragment =
                    supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                            as? TrainingFragment

                fragment?.let { frag ->
                    lifecycleScope.launch {
                        // API 호출
                        val apiResult = frag.callApiAndGetResult()
                        val gson = Gson()
                        Log.d("kimtest", gson.toJson(apiResult))
                        Log.d("kimtest", "API result: $apiResult")

                        // UI 3초 딜레이 후 resetUI
//                        kotlinx.coroutines.delay(3000)
                        if (apiResult != null) {
                            resetUI(apiResult)
                        }
                    }
                } ?: run {

                    lifecycleScope.launch {
                        //실패
//                        kotlinx.coroutines.delay(3000)
//                        resetUI()
                    }
                }


            }
        }


        loadTrainingFragment()
    }

    fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun showManikinChooseDialog(address: String) {
        val dialog = ManikinTypeChooseDialog(this)

        dialog.listener = object : ManikinTypeChooseDialog.Listener {
            override fun onSelected(type: DeviceType) {
                bleManager.setManualDeviceType(address, type)

                binding.startStopButton.setBgColor(android.graphics.Color.parseColor("#0061F2"))

                binding.startStopButton.setPaddingRelative(
                    dp(46),
                    binding.startStopButton.paddingTop,
                    dp(46),
                    binding.startStopButton.paddingBottom
                )

                binding.startStopButton.text = "Practice Start"
                binding.timerLayout.visibility = View.GONE
                binding.BackLayout.visibility = View.VISIBLE
                binding.timerText.text = "00:00"

                loadTrainingFragment()

            }

            override fun onCancel() {
                // 선택 안 하면 연결 끊어도 되고 그냥 둬도 됨
                Log.d("ManikinDialog", "User cancelled manikin type selection")
            }
        }

        dialog.start()
    }


    fun stopTraining(){

        val deviceToUse = bleManager.getCurrentConnectedDevices()
            .firstOrNull { device ->
                requiredTypes.contains(device.deviceType) &&
                        device.deviceType != DeviceType.AED
            }

        binding.trainingOverLayout.visibility = View.VISIBLE

        deviceToUse?.let { device ->
            val packet = byteArrayOf(0x58)
            bleManager.sendPacketToDevice(device, packet)
        } ?: Log.w("kimtest", "no matching device")
    }
    fun onclickStop() {
        binding?.startStopButton?.performClick()
    }

    override fun onResume() {
        super.onResume()
        bleManager.attachActivity(this)
    }

    private fun loadTrainingFragment() {
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                TrainingFragment.newInstance(trainingTypes,mannequinType)
            )
            .commitAllowingStateLoss()

//        supportFragmentManager.beginTransaction()
//            .replace(
//                R.id.fragmentContainer,
//                TwoRescuerTrainingFragment.newInstance(trainingTypes)
//            )
//            .commit()

    }

    private fun resetUI(apiResult: HstmResponse) {

        bleManager.getCurrentConnectedDevices()
            .firstOrNull { device ->
                device.deviceType == DeviceType.AED
            }.let { device ->
                val dataToSend = byteArrayOf(0x63, 13, 0)
                if (device != null) {
                    bleManager.sendPacketToDevice(device, dataToSend)
                }
            }

        binding.trainingOverLayout.visibility = View.GONE
        stopTimer()

        binding.startStopButton.setBgColor(android.graphics.Color.parseColor("#0061F2"))

        binding.startStopButton.text = "Practice Start"

        binding.startStopButton.setPaddingRelative(
            dp(46),
            binding.startStopButton.paddingTop,
            dp(46),
            binding.startStopButton.paddingBottom
        )

        binding.timerLayout.visibility = View.GONE
        binding.BackLayout.visibility = View.VISIBLE
        binding.timerText.text = "00:00"

        contentItem?.status = TrainingStatus.COMPLETED

        contentItem?.let {
            it.status = TrainingStatus.COMPLETED
            UserTrainingState.markCompleted(it) // 고유 키로 완료 처리
        }
        // Fragment 리셋
        loadTrainingFragment()

        val intent = android.content.Intent(this, ResultActivity::class.java).apply {
            putExtra("deviceTypes", ArrayList(contentItem?.requiredDeviceTypes))
            putExtra("passing_score", contentItem?.passing_Score)
            putExtra("trainingTypes", ArrayList(trainingTypes))
            putExtra("hstmResponse", apiResult)
            putExtra("contentItem", contentItem as Serializable)
        }
        startActivity(intent)
    }

    private fun startTimer(seconds: Int) {

        stopTimer()

        timer = object : CountDownTimer(seconds * 1000L, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val min = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                val sec = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                binding.timerText.text = String.format("%02d:%02d", min, sec)
            }

            override fun onFinish() {
                binding.timerText.text = "00:00"
                binding.startStopButton.performClick()
            }
        }.start()
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }
}
