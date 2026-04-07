package com.example.hstm_aos.activity

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.hstm_aos.ContentsItem
import com.example.hstm_aos.DisConnectDialog
import com.example.hstm_aos.Fragment.TrainingFragment
import com.example.hstm_aos.Fragment.TwoRescuerTrainingFragment
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
import com.google.android.exoplayer2.MediaItem
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TwoRescuerTrainingActivity : BaseActivity() ,
    TwoRescuerTrainingFragment.OnTrainingFinishedListener {

    private val _blePacketFlow = MutableSharedFlow<Pair<String, ByteArray>>(replay = 0)
    val blePacketFlow = _blePacketFlow.asSharedFlow()

    private lateinit var binding: ActivityTrainingBinding
    private lateinit var bleManager: BleManager
    private var requiredTypes: Set<DeviceType> = emptySet()

    private var timer: CountDownTimer? = null
    private var totalSeconds = 360
    private lateinit var trainingTypes: Set<TrainingType>
    private var contentItem: ContentsItem.Content? = null
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
        binding.userIDTextView.text = "${UserInfoManager.getFirstName(this)} ${UserInfoManager.getLastName(this)}, ${UserInfoManager.getOrganizations(this)}"
        contentItem?.let {
//            trainingTypes = it.trainingType
//            binding.trainingTypeTextView.text = it.trainingType.toString()
//            requiredTypes = it.requiredDeviceTypes
        }

        lifecycleScope.launch {
            bleManager.receivedPackets.collect { (address, data) ->
                _blePacketFlow.emit(address to data)
            }
        }

        lifecycleScope.launch {
            val fragment =
                supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                        as? TrainingFragment

            bleManager.connectedDevice.collect { (device, connected, type) ->
                if (connected) {
                    stopTraining()

                    fragment?.stopTimer()
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

//                    binding.timerLayout.visibility = View.GONE
                    binding.BackLayout.visibility = View.VISIBLE
//                    binding.timerText.text = "00:00"

                    loadTrainingFragment()
                } else {
                    //팝업
                    val fragment =
                        supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                                as? TrainingFragment
                    fragment?.stopTimer()
                    binding.trainingOverLayout.visibility = View.GONE
                    binding.startStopButton.setBgColor(android.graphics.Color.parseColor("#0061F2"))

                    binding.startStopButton.text = "Practice Start"

                    binding.startStopButton.setPaddingRelative(
                        dp(46),
                        binding.startStopButton.paddingTop,
                        dp(46),
                        binding.startStopButton.paddingBottom
                    )

//                    binding.timerLayout.visibility = View.GONE
                    binding.BackLayout.visibility = View.VISIBLE
//                    binding.timerText.text = "00:00"

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

        binding.startStopButton.setOnClickListener {

            val deviceToUse = bleManager.getCurrentConnectedDevices()
                .firstOrNull { device ->
                    requiredTypes.contains(device.deviceType) &&
                            device.deviceType != DeviceType.AED
                }

            if (deviceToUse == null){
                Log.d("kimtest","?")
                return@setOnClickListener
            }
            if (binding.startStopButton.text == "Practice Start") {

                Log.d("kimtest","@#@# ${deviceToUse?.deviceType?.name}")


//                Handler().postDelayed({
//                    val audioUri =
//                        Uri.parse("android.resource://${packageName}/${R.raw.estart}")
//                    val mediaItem = MediaItem.fromUri(audioUri)
//                    exoPlayer?.apply {
//                        setMediaItem(mediaItem)
//                        prepare()
//                        play()
//                    }
//                }, 2000)


                binding.startStopButton.text = "End Skills Check"

                binding.startStopButton.setPaddingRelative(
                    dp(37),
                    binding.startStopButton.paddingTop,
                    dp(37),
                    binding.startStopButton.paddingBottom
                )

                binding.startStopButton.setBgColor(android.graphics.Color.parseColor("#333333"))
//                binding.timerLayout.visibility = View.VISIBLE
                binding.BackLayout.visibility = View.GONE

                val fragment =
                    supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                            as? TwoRescuerTrainingFragment

                fragment?.startCountDown {
                    fragment.startTimer(totalSeconds)
                    deviceToUse?.let { device ->
                        val packet = byteArrayOf(0x54, 0x01)
                        bleManager.sendPacketToDevice(device, packet)
                    } ?: Log.w("kimtest", "no matching device")
                }

            } else {
                binding.trainingOverLayout.visibility = View.VISIBLE

                val fragment =
                    supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                            as? TwoRescuerTrainingFragment

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

                deviceToUse?.let { device ->
                    val packet = byteArrayOf(0x58)
                    bleManager.sendPacketToDevice(device, packet)
                } ?: Log.w("kimtest", "no matching device")
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
//                binding.timerLayout.visibility = View.GONE
                binding.BackLayout.visibility = View.VISIBLE
//                binding.timerText.text = "00:00"

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


    override fun onResume() {
        super.onResume()
        bleManager.attachActivity(this)
    }

    private fun loadTrainingFragment() {
//        supportFragmentManager.beginTransaction()
//            .replace(
//                R.id.fragmentContainer,
//                TrainingFragment.newInstance(trainingTypes)
//            )
//            .commit()

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                TwoRescuerTrainingFragment.newInstance(trainingTypes,mannequinType)
            )
            .commit()

    }

    private fun resetUI(apiResult: HstmResponse) {
        val fragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                    as? TrainingFragment
        binding.trainingOverLayout.visibility = View.GONE
        fragment?.stopTimer()

        binding.startStopButton.setBgColor(android.graphics.Color.parseColor("#0061F2"))

        binding.startStopButton.text = "Practice Start"

        binding.startStopButton.setPaddingRelative(
            dp(46),
            binding.startStopButton.paddingTop,
            dp(46),
            binding.startStopButton.paddingBottom
        )


//        binding.timerLayout.visibility = View.GONE
        binding.BackLayout.visibility = View.VISIBLE
//        binding.timerText.text = "00:00"

        contentItem?.status = TrainingStatus.COMPLETED

        contentItem?.let {
            it.status = TrainingStatus.COMPLETED
            UserTrainingState.markCompleted(it) // 고유 키로 완료 처리
        }
        // Fragment 리셋
        loadTrainingFragment()

        val intent = android.content.Intent(this, ResultActivity::class.java).apply {
            putExtra("deviceTypes", ArrayList(contentItem?.requiredDeviceTypes))
            putExtra("trainingTypes", ArrayList(trainingTypes))
            putExtra("passing_score", contentItem?.passing_Score)
            putExtra("hstmResponse", apiResult)
        }
        startActivity(intent)
    }

    fun startStopButtonPerformClick(){
        binding.startStopButton.performClick()
    }

}
