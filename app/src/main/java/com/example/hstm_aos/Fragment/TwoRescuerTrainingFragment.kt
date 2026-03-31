package com.example.hstm_aos.Fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.hstm_aos.CprAnalysisApi

import com.example.hstm_aos.R
import com.example.hstm_aos.RetryDialog
import com.example.hstm_aos.UserInfoManager
import com.example.hstm_aos.activity.TrainingActivity
import com.example.hstm_aos.activity.TwoRescuerTrainingActivity
import com.example.hstm_aos.ble.DeviceType
import com.example.hstm_aos.ble.TrainingType
import com.example.hstm_aos.customview.RoundedLinearLayout
import com.example.hstm_aos.databinding.FragmentTwoRescuerTrainingBinding
import com.example.hstm_aos.model.HstmResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ExoPlayer

class TwoRescuerTrainingFragment : Fragment(R.layout.fragment_two_rescuer_training) {

    private var _binding: FragmentTwoRescuerTrainingBinding? = null
    private val binding get() = _binding!!

    /* =========================
     * Count / State
     * ========================= */
    private var compCount = 0
    private var ventCount = 0
    private var cycleCount = 0

    private var peakVolume = 0

    private var isPeak = false
    private var lastValue = 0
    private var lastSpeed = 0
    private var peakDepth = 0
    private var peakHandPoint = 0

    private var isVentPeak = false
    private var lastVentValue = 0

    private var lastCompressionTime = 0L
    private var lastVentTime = 0L

    private val COMPRESSION_TIMEOUT = 1000L

    private var compressionTimeoutJob: Job? = null
    private var virtualJob: Job? = null

    private lateinit var trainingTypes: Set<TrainingType>
    private var trainingType: String = ""

    private var isAedActive = false
    private var isVirtualTime = false

    private var originalWidth = 0

    private var sender: FileRecordSender? = null

    private var lastIsMyTurn: Boolean? = null
    val eventList = mutableListOf<Map<String, Any>>()

    private var hexBuf: CopyOnWriteArrayList<ByteArray> = CopyOnWriteArrayList()


    private var lastActionTime = System.currentTimeMillis()
    private var handsOffCount = 0
    private var isHandsOff = false

    private val HANDS_OFF_INTERVAL = 3000L
    var hasAction = false

    private var handsOffStartTime = 0L
    private var lastHandsOffTick = 0L

    private var minDepthValue = 50
    private var maxDepthValue = 60


    private var minVolume = 400
    private var maxVolume = 700
    private var chestCountGuide = 30

    private var isFinish = false
    private var mannequinType: String = ""
    private lateinit var mannequinTypes : Set<DeviceType>
    private var cpr_cycle_type = "302"

    private var exoPlayer: ExoPlayer? = null
    private var currentSoundRes: Int? = null

    private var finishCycle = 8

    /* =========================
     * Phase
     * ========================= */
    private enum class Phase { COMPRESSION, VENTILATION }

    interface OnTrainingFinishedListener {
        fun onTrainingFinished(apiResult: HstmResponse)
    }


    private var listener: OnTrainingFinishedListener? = null


    companion object {
        private const val KEY_TRAINING_TYPE = "KEY_TRAINING_TYPE"
        private const val KEY_MANNEQUIN_TYPE = "KEY_MANNEQUIN_TYPE"

        fun newInstance(
            types: Set<TrainingType>,
            mannequinType: Set<DeviceType>
        ): TwoRescuerTrainingFragment {
            return TwoRescuerTrainingFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_TRAINING_TYPE, HashSet(types))
                    putSerializable(KEY_MANNEQUIN_TYPE,HashSet(mannequinType)) // 전달
                }
            }
        }
    }

    /* =========================
     * Lifecycle
     * ========================= */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTwoRescuerTrainingBinding.bind(view)

        exoPlayer = ExoPlayer.Builder(requireContext()).build()

        val activity = requireActivity() as TwoRescuerTrainingActivity

        resetHandsOffTimer()
        trainingTypes =
            arguments?.getSerializable(KEY_TRAINING_TYPE) as? HashSet<TrainingType> ?: emptySet()

        mannequinTypes =
            (arguments?.getSerializable(KEY_MANNEQUIN_TYPE) as? HashSet<DeviceType>)
                ?: emptySet()

        trainingType = when {
            trainingTypes.contains(TrainingType.CPR) -> "CPR"
            trainingTypes.contains(TrainingType.CCO) -> "CCO"
            trainingTypes.contains(TrainingType.VO) -> "vent"
            else -> ""
        }

        if (mannequinTypes.contains(DeviceType.BABY)){
            mannequinType = "infant"
            cpr_cycle_type = "152"
        } else if (mannequinTypes.contains(DeviceType.CHILD)){
            mannequinType = "child"
        } else if (mannequinTypes.contains(DeviceType.PRO)){
            mannequinType = "adult"
        }

        hexBuf = CopyOnWriteArrayList()
        hexBuf.clear()

        Log.d("kimtest44","${mannequinType}")
        if (mannequinType.equals("infant")){
            minDepthValue = 30
            maxDepthValue = 40

            minVolume = 200
            maxVolume = 400

            chestCountGuide = 15
            binding.ventView.setNormalRange(200..400)

            binding.ventView.setBadgeVisible(true)
        }
        binding.depthView.setNormalRange(minDepthValue,maxDepthValue)

        binding.depthView.setTrainingType(trainingType)
        binding.handPositionView.setTrainingType(trainingType)
        binding.handPositionView.setManikinType(mannequinType)
        binding.speedView.setTrainingType(trainingType)

        binding.tvCompressionCount.text = "$compCount/${chestCountGuide}"

        /* BLE 수신 */
        viewLifecycleOwner.lifecycleScope.launch {
            activity.blePacketFlow.collect { (_, data) ->
                if (!isVirtualTime &&
                    data.isNotEmpty() &&
                    String.format("%02X", data[0]) == "A8"
                ) {
                    if (!isVirtualTime) {
                        getTrainingData1(data)
                    }
                }
            }
        }
    }

    private fun showErrorDialog(message : String, isInternetError : Boolean = false){
        val dlg = RetryDialog(requireContext(), message,isInternetError)
        dlg.listener = object : RetryDialog.ReTryDialogClickedListener {
            override fun noButton() {
                val activity = requireActivity() as TrainingActivity
                activity.finish()
            }

            override fun tryAgainButton() {
                (activity as? TrainingActivity)?.stopTraining()
                lifecycleScope.launch {
                    val result = callApiAndGetResult()
                    if (result != null) {
                        listener?.onTrainingFinished(result)
                    }
                }
            }

        }
        dlg.start()
    }


    //음성
    private fun playSound(res: Int) {

        if (!UserInfoManager.getVoiceGuide(requireContext())){
            return
        }
        if (exoPlayer?.isPlaying == true && currentSoundRes == res) {
            return
        }

//            if (!exoPlayer!!.isPlaying) {
        val audioUri =
            Uri.parse("android.resource://${requireContext().packageName}/${res}")
        val mediaItem = MediaItem.fromUri(audioUri)
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        currentSoundRes = res

//        }
    }

    /*0 = 가슴압박 시작
    1 = 가슴압박 끝
    10 = 호흡 시작
    11 = 호흡 끝*/


    private fun resetHandsOffTimer() {
        val now = System.currentTimeMillis()
        lastActionTime = now
        handsOffStartTime = 0L
        lastHandsOffTick = 0L
        handsOffCount = 0
        isHandsOff = false
    }

    fun addEvent(event: Int, timestamp: Long) {
        Log.d("kimtest888","event ${event}")
        val newEvent = mapOf(
            "event" to event,
            "timestamp" to timestamp
        )
        eventList.add(newEvent)
    }

    /* =========================
     * Cycle Rule
     * ========================= */
    private fun isMyCompressionCycle(cycle: Int): Boolean {
        val block = (cycle - 1) / 2
        return block % 2 == 0
    }

    private fun traineePhase(): Phase =
        if (isMyCompressionCycle(cycleCount))
            Phase.COMPRESSION
        else
            Phase.VENTILATION

    private fun virtualPhase(): Phase =
        if (isMyCompressionCycle(cycleCount))
            Phase.VENTILATION
        else
            Phase.COMPRESSION

    /* =========================
     * Data Engine
     * ========================= */
    private fun getTrainingData(data: ByteArray, isVirtual : Boolean = true) {

        var chestCompression = 0
        if (isVirtual && mannequinType.contains("infant",true)){
            chestCompression = findMaxValue(data, 1, 10)?.toUByte()?.toInt()?:0
            data[1] = (data[1].toUByte().toFloat() / 1.5).toInt().toByte()
            data[2] = (data[2].toUByte().toFloat() / 1.5).toInt().toByte()
            data[3] = (data[3].toUByte().toFloat() / 1.5).toInt().toByte()
            data[4] = (data[4].toUByte().toFloat() / 1.5).toInt().toByte()
            data[5] = (data[5].toUByte().toFloat() / 1.5).toInt().toByte()
            data[6] = (data[6].toUByte().toFloat() / 1.5).toInt().toByte()
            data[7] = (data[7].toUByte().toFloat() / 1.5).toInt().toByte()
            data[8] = (data[8].toUByte().toFloat() / 1.5).toInt().toByte()
            data[9] = (data[9].toUByte().toFloat() / 1.5).toInt().toByte()
            data[10] = (data[10].toUByte().toFloat() / 1.5).toInt().toByte()
        }else {
            chestCompression = findMaxValue(data, 1, 10)?.toUByte()?.toInt()?:0
        }
        var chestCompressionSpeed = data[11].toUByte().toInt()
        if (isVirtual && virtualPhase().equals(Phase.VENTILATION)){
            chestCompressionSpeed = 0
        }
        val ventilationRate = data[16].toUByte().toInt()
        val alarmBit = (ventilationRate shr 7) and 0x01
        lastSpeed = chestCompressionSpeed


        Log.d("kimtest777","${isVirtual} ${mannequinType}")
        var ventilationPacket = 0
        if (isVirtual && mannequinType.contains("infant",true)){
            ventilationPacket = findMaxValue(data, 14, 15)?.toUByte()?.toInt()?:0 / 2
            data[14] = (data[14].toUByte().toInt() / 2).toByte()
            data[15] = (data[15].toUByte().toInt() / 2).toByte()
        }else {
            ventilationPacket = findMaxValue(data, 14, 15)?.toUByte()?.toInt()?:0
        }

//        val ventilationPacket = findMaxValue(data, 14, 15)?.toUByte()?.toInt()
        val chestCompressionPoint = data[13].toUByte().toInt()


        val timestamp = System.currentTimeMillis()
        val timestampBytes = ByteBuffer
            .allocate(Long.SIZE_BYTES)
            .putLong(timestamp)
            .array()

        hexBuf.add(data + timestampBytes)

        if (chestCompression != null) {
            val value = chestCompression / 2
            if (chestCompression > 20 && (virtualPhase().equals(Phase.COMPRESSION))) {
                if (mannequinType.contains("infant") && isVirtual){
                    binding.depthView.addValue((value/1.5).toInt())
                } else {
                    binding.depthView.addValue(value)
                }
                detectCompressionPeak(value, chestCompressionPoint)
                hasAction = true
            } else {
                binding.depthView.addValue(0)
            }
        }

        binding.speedView.moveBarTo(chestCompressionSpeed)

        if (ventilationPacket != null && virtualPhase().equals(Phase.VENTILATION)) {
            val ventValue = ventilationPacket * 10
            if (mannequinType.contains("infant") && isVirtual) {
                binding.ventView.addValue(ventValue/2)
            }else{
                binding.ventView.addValue(ventValue)
            }
            detectVentPeak(ventValue, alarmBit)
            hasAction = true
        } else {
            binding.ventView.addValue(0)
        }


        // ==========================
        // 3️⃣ HANDS-OFF 판단
        // ==========================

        if (!isAedActive) {
            val validAction = (chestCompression != null && chestCompression > 20) || (ventilationPacket != null && ventilationPacket > 0)


            val currentTime = System.currentTimeMillis()

            if (validAction || isVirtualTime) {
                // 행동이 있으면 리셋
                lastActionTime = currentTime
                handsOffStartTime = 0L
                lastHandsOffTick = 0L
                handsOffCount = 0
                isHandsOff = false
            } else {
                val idleTime = currentTime - lastActionTime

                // 🔹 3초 지나면 hands-off 시작
                if (idleTime >= HANDS_OFF_INTERVAL) {

                    if (!isHandsOff) {
                        isHandsOff = true
                        handsOffStartTime = currentTime
                        lastHandsOffTick = currentTime
                        handsOffCount = 1
                        if (!isVirtualTime) binding.tvGuide.text = "Hands Off Time 1s"
                    } else {
                        if (currentTime - lastHandsOffTick >= 1000) {
                            handsOffCount++
                            lastHandsOffTick = currentTime
                            if (!isVirtualTime) binding.tvGuide.text = "Hands Off Time ${handsOffCount}s"
                        }
                    }
                }
            }
        }


        hasAction = false
    }


    private fun getTrainingData1(data: ByteArray, isVirtual : Boolean = false) {

        var chestCompression = 0
        if (isVirtual && mannequinType.contains("infant",true)){
            chestCompression = findMaxValue(data, 1, 10)?.toUByte()?.toInt()?:0
            data[1] = (data[1].toUByte().toFloat() / 1.5).toInt().toByte()
            data[2] = (data[2].toUByte().toFloat() / 1.5).toInt().toByte()
            data[3] = (data[3].toUByte().toFloat() / 1.5).toInt().toByte()
            data[4] = (data[4].toUByte().toFloat() / 1.5).toInt().toByte()
            data[5] = (data[5].toUByte().toFloat() / 1.5).toInt().toByte()
            data[6] = (data[6].toUByte().toFloat() / 1.5).toInt().toByte()
            data[7] = (data[7].toUByte().toFloat() / 1.5).toInt().toByte()
            data[8] = (data[8].toUByte().toFloat() / 1.5).toInt().toByte()
            data[9] = (data[9].toUByte().toFloat() / 1.5).toInt().toByte()
            data[10] = (data[10].toUByte().toFloat() / 1.5).toInt().toByte()
        }else {
            chestCompression = findMaxValue(data, 1, 10)?.toUByte()?.toInt()?:0
        }
        var chestCompressionSpeed = data[11].toUByte().toInt()

        val ventilationRate = data[16].toUByte().toInt()
        val alarmBit = (ventilationRate shr 7) and 0x01
        lastSpeed = chestCompressionSpeed


        Log.d("kimtest777","${isVirtual} ${mannequinType}")
        var ventilationPacket = 0
        if (isVirtual && mannequinType.contains("infant",true)){
            ventilationPacket = findMaxValue(data, 14, 15)?.toUByte()?.toInt()?:0 / 2
            data[14] = (data[14].toUByte().toInt() / 2).toByte()
            data[15] = (data[15].toUByte().toInt() / 2).toByte()
        }else {
            ventilationPacket = findMaxValue(data, 14, 15)?.toUByte()?.toInt()?:0
        }

//        val ventilationPacket = findMaxValue(data, 14, 15)?.toUByte()?.toInt()
        val chestCompressionPoint = data[13].toUByte().toInt()


        val timestamp = System.currentTimeMillis()
        val timestampBytes = ByteBuffer
            .allocate(Long.SIZE_BYTES)
            .putLong(timestamp)
            .array()

        hexBuf.add(data + timestampBytes)

        if (chestCompression != null) {
            val value = chestCompression / 2
            if (chestCompression > 20 && (traineePhase().equals(Phase.COMPRESSION))) {
                if (mannequinType.contains("infant") && isVirtual){
                    binding.depthView.addValue((value/1.5).toInt())
                } else {
                    binding.depthView.addValue(value)
                }
                detectCompressionPeak(value, chestCompressionPoint,chestCompressionSpeed)
                hasAction = true
            } else {
                binding.depthView.addValue(0)
            }
        }

        binding.speedView.moveBarTo(chestCompressionSpeed)

        if (ventilationPacket != null && traineePhase().equals(Phase.VENTILATION)) {
            val ventValue = ventilationPacket * 10
            if (mannequinType.contains("infant") && isVirtual) {
                binding.ventView.addValue(ventValue/2)
            }else{
                binding.ventView.addValue(ventValue)
            }
            detectVentPeak(ventValue, alarmBit)
            hasAction = true
        } else {
            binding.ventView.addValue(0)
        }


        // ==========================
        // 3️⃣ HANDS-OFF 판단
        // ==========================

        if (!isAedActive) {
            val validAction = (chestCompression != null && chestCompression > 20) || (ventilationPacket != null && ventilationPacket > 0)


            val currentTime = System.currentTimeMillis()

            if (validAction || isVirtualTime) {
                // 행동이 있으면 리셋
                lastActionTime = currentTime
                handsOffStartTime = 0L
                lastHandsOffTick = 0L
                handsOffCount = 0
                isHandsOff = false
            } else {
                val idleTime = currentTime - lastActionTime

                // 🔹 3초 지나면 hands-off 시작
                if (idleTime >= HANDS_OFF_INTERVAL) {

                    if (!isHandsOff) {
                        isHandsOff = true
                        handsOffStartTime = currentTime
                        lastHandsOffTick = currentTime
                        handsOffCount = 1
                        if (!isVirtualTime) binding.tvGuide.text = "Hands Off Time 1s"
                    } else {
                        if (currentTime - lastHandsOffTick >= 1000) {
                            handsOffCount++
                            lastHandsOffTick = currentTime
                            if (!isVirtualTime) binding.tvGuide.text = "Hands Off Time ${handsOffCount}s"
                        }
                    }
                }
            }
        }


        hasAction = false
    }

    private fun createSingleFile(hexBuf: CopyOnWriteArrayList<ByteArray>, fileName: String): File {
        val combinedByteArray = hexBuf.flatMap { it.asList() }.toByteArray()

        val file = File(requireContext().filesDir, fileName)
        file.writeBytes(combinedByteArray)

        return file
    }

    suspend fun callApiAndGetResult(): HstmResponse? {
        return withContext(Dispatchers.IO) {
            try {
                // Retrofit 초기화
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS) // 연결 타임아웃
                    .writeTimeout(120, TimeUnit.SECONDS)  // 업로드 타임아웃
                    .readTimeout(120, TimeUnit.SECONDS)   // 다운로드 타임아웃
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://hstmprod.braydenlab.com/") // 운영 URL
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(CprAnalysisApi::class.java)

                // RequestBody 생성
                val mediaType = "application/octet-stream".toMediaTypeOrNull()
                val singleFile = createSingleFile(hexBuf, "cprTraining.bin")
                val aedFilePart: MultipartBody.Part? = null
                val rawFilePart = MultipartBody.Part.createFormData(
                    "rawHexBPfile",
                    singleFile.name,
                    RequestBody.create(mediaType, singleFile)
                )


                val jsonEventList = Gson().toJson(eventList)
                val eventListRequestBody = jsonEventList.toRequestBody("application/json".toMediaType())


                fun toRequestBody(value: String): RequestBody =
                    RequestBody.create("text/plain".toMediaTypeOrNull(), value)

                // API 호출
                val response: Response<HstmResponse> = api.finishTraining(
                    condition = toRequestBody(
                        """{
                    "mode": "assessment",
                    "target": "${mannequinType}",
                    "training_type": "cpr",
                    "guideline": "AHA2020",
                    "cpr_cycle_type": "${cpr_cycle_type}",
                    "is_2rescuers": true
                }"""
                    ),
                    accessToken = toRequestBody("YOUR_ACCESS_TOKEN_HERE"),
                    deviceInfo = toRequestBody("""{"DeviceID":"A10D5560-B6E5-54A2-A30E-05FD658251B2","IpAddress":"192.168.0.162","DeviceName":"iPad","DeviceOS":"18.6","SSID":"","DeviceModel":"iPad8,6"}"""),
                    organization = toRequestBody("""{"show_welcome_screen":true,"Last_name":"Russo","First_name":"Bode","org_name":"Quality Care Health System","org_id":"285b5dfe-8a16-e911-b189-005056b10657"}"""),
                    dummy = toRequestBody("""{"DeviceID":"emulator","Model":"Brayden pro","Hardware":"1.0","Firmware":"101.2534"}"""),
                    openSkill = toRequestBody("""{"Reference_ID":"68197258-50dc-ec11-80f0-005056b137a2","TrainMode":"Skills","Skill_Type_id":4,"Passing_Score":"84","CompVentRatio":"30:2","Skill_Type":"Adult 1-Provider CPR","Due_Date":"2022-06-01 23:59:00","Assignment_ID":"68197258-50dc-ec11-80f0-005056b137a2","Cert_Type":1}"""),
                    usage = toRequestBody("""{"Timezone":"Asia/Seoul","hstreamId":"bfb1d35a-6a3d-47e5-af0c-3e724c951c5c","Time":"19:21:44","uName":"Bode Russo","Timestamp":1769077377,"Coaching":"Self training","Date":"2026.01.22","validNum":2103059167,"Application version":"2.11 (2)","Type":"CPR Training","Regional_Option":"usa","Application Name":"Resus Skills","Level":"Lay","CreateEpoch":1769077377,"Guideline":"ARC 2020","Comment":"This is comment field","Email":"aquaall05252022122955@hstm.testinator.com"}"""),
                    institution = toRequestBody("""{"feedback":"True","inst_name":"Quality Care Medical Center","is_bridge_user":false,"inst_id":"49c18030-8c16-e911-b189-005056b10657"}"""),
                    calculationService = toRequestBody("""{"Version":"1.1.301"}"""),
                    guidePrompts = toRequestBody("""["• Compression fraction is very good","• Provide a ventilation volume of between 400ml and 700ml for each ventilation"]"""),
                    custom = toRequestBody("\"\""),
                    certification = toRequestBody("""{"Target":"N/A"}"""),
                    rawHexBPfile = rawFilePart,
                    aedHexBPfile = aedFilePart,
                    vp_event_list = eventListRequestBody
                )

                if (response.isSuccessful) {
                    response.body()

                } else {
                    // 서버 오류
                    withContext(Dispatchers.Main) {
                        showErrorDialog("We apologize for the inconvenience.\nPlease contact your manager for assistance.")
                    }
                    null
                }
            } catch (e: IOException) {
                // 인터넷 연결 실패
                withContext(Dispatchers.Main) {
                    showErrorDialog("Please check your internet connection and try again.\nIf the issue persists, restart the training.",true)
                }
                null
            } catch (e: HttpException) {
                // HTTP 오류
                withContext(Dispatchers.Main) {
                    showErrorDialog("We apologize for the inconvenience.\nPlease contact your manager for assistance.")
                }
                null
            } catch (e: Exception) {
                // 기타 오류
                withContext(Dispatchers.Main) {
                    showErrorDialog("We apologize for the inconvenience.\nPlease contact your manager for assistance.")
                }
                null
            }
        }
    }


    /* =========================
     * Compression
     * ========================= */
    private fun detectCompressionPeak(value: Int, handPoint: Int, rate : Int = 0) {


        if (value > 20 && !isVirtualTime){
            binding.switchGuideTextView.visibility = View.INVISIBLE
        }

        if (value > lastValue) {
            isPeak = true
            peakDepth = value
            peakHandPoint = handPoint
        }

        if (isPeak && value < lastValue) {
            compCount++

            if (cycleCount ==0){
                cycleCount++
                updateCycleText()
            }

            if (!isVirtualTime) {
                if (peakHandPoint != 1) {
                    playSound(R.raw.e_ocation)
                } else if (maxDepthValue < lastValue) {
                    playSound(R.raw.e_shollower)
                }
                else if (minDepthValue > lastValue){
                    Log.d("kimtest333","???? =${value}")
                    playSound(R.raw.e_deeper)//e_shollower
                }else if (100 > rate){
                    playSound(R.raw.e_faster) //e_slower
                }else if (120 < rate){
                    playSound(R.raw.e_slower)
                }else {
                    playSound(R.raw.e_good)
                }
            }
            lastCompressionTime = System.currentTimeMillis()

            updateCompressionText()
            binding.handPositionView.updateState(peakHandPoint)
            updateFeedback(peakDepth, lastSpeed, peakHandPoint)


            if (isVirtualTime && cycleCount == 4 && compCount == 28) {
//                binding.switchGuideTextView.visibility = View.VISIBLE
            }

            startTimeoutWatcher()
            isPeak = false
        }
        lastValue = value
    }

    /* =========================
     * Ventilation
     * ========================= */
    private fun detectVentPeak(value: Int,alarmBit : Int) {

        if (mannequinType.equals("infant", ignoreCase = true)) {

            if (alarmBit.toInt() == 0) {
//                    ventSpeedSuccess = true
            } else if (alarmBit.toInt() == 1) {
                binding.ventView.highlightBadgeRed()
            }
        }

        if (value > 20 && !isVirtualTime){
            binding.switchGuideTextView.visibility = View.INVISIBLE
        }

        if (value > lastVentValue) {
            isVentPeak = true
            peakVolume = value
        }

        if (isVentPeak && value < lastVentValue) {
            ventCount++
            lastVentTime = System.currentTimeMillis()
            if (mannequinType.equals("infant", ignoreCase = true)) {

                if (alarmBit.toInt() == 0) {
                    updateVolumeGuide(peakVolume)
                } else if (alarmBit.toInt() == 1) {
                    binding.tvGuide.text = "Blow softer"
                }
            }else {updateVolumeGuide(peakVolume)}

            updateVentText()

            startTimeoutWatcher()



            if (ventCount >= 2) {
                onCycleCompleted()
            }
            isVentPeak = false
        }
        lastVentValue = value
    }

    /* =========================
     * Timeout Logic (분리 핵심)
     * ========================= */
    private fun startTimeoutWatcher() {
        compressionTimeoutJob?.cancel()
        compressionTimeoutJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(COMPRESSION_TIMEOUT)
            checkTimeoutAndStartVirtual()
        }
    }

    private fun checkTimeoutAndStartVirtual() {
        if (isVirtualTime) return

        when (traineePhase()) {
            Phase.COMPRESSION -> checkCompressionTimeout()
            Phase.VENTILATION -> checkVentTimeout()
        }
    }

    private fun checkCompressionTimeout() {
        if (compCount < chestCountGuide ) return
        if (System.currentTimeMillis() - lastCompressionTime >= COMPRESSION_TIMEOUT) {
            startVirtualTime()
        }
    }

    private fun checkVentTimeout() {
        if (ventCount < 2 ) return
        if (System.currentTimeMillis() - lastVentTime >= COMPRESSION_TIMEOUT) {
            Log.d("kimtest555","${ventCount} == ${traineePhase()}")
            startVirtualTime()
        }
    }

    fun startCountDown(onFinish: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {

            binding.tvGuide.text = "3"
            kotlinx.coroutines.delay(1000)

            binding.tvGuide.text = "2"
            kotlinx.coroutines.delay(1000)

            binding.tvGuide.text = "1"
            kotlinx.coroutines.delay(1000)

            binding.tvGuide.text = "START"
            resetHandsOffTimer()
            kotlinx.coroutines.delay(500)

            updateGuideText()
            onFinish()
        }
    }

    /* =========================
     * Cycle Complete
     * ========================= */
    private fun onCycleCompleted() {
        cycleCount++

        if (cycleCount >= finishCycle+1 && ventCount >= 2) {

            isFinish = true
            lifecycleScope.launch {
                (activity as? TwoRescuerTrainingActivity)!!.stopTraining()
                val result = callApiAndGetResult()
                if (result != null) {
                    listener?.onTrainingFinished(result)
                }
            }
        }

        compCount = 0
        ventCount = 0

        isVentPeak = false
        lastVentValue = 0

        isPeak = false
        lastValue = 0

        stopVirtualTime()
        updateCycleText()
        updateGuideText()

        // 3–4 사이클이면 즉시 버추얼 시작
        if (traineePhase() == Phase.VENTILATION) {
            startVirtualTime()
        }



    }

    /* =========================
     * Virtual
     * ========================= */
    private fun startVirtualTime() {
        if (isVirtualTime || isFinish) return

        isVirtualTime = true
        updateGuideText()

        if (virtualPhase().equals(Phase.COMPRESSION)){
            addEvent(0,System.currentTimeMillis())
        } else if (virtualPhase().equals(Phase.VENTILATION)){
            addEvent(10,System.currentTimeMillis())
        }
        val assetFile = when (virtualPhase()) {
            Phase.COMPRESSION -> if (mannequinType.contains("infant")){"test13.bin"} else {"rawHexBPfile_chest1.bin"}
            Phase.VENTILATION -> "rawHexBPfile_vent1.bin"
        }

        sender?.stop()
        sender = FileRecordSender(requireContext(), assetFile)

        virtualJob?.cancel()
        virtualJob = viewLifecycleOwner.lifecycleScope.launch {
            sender?.start { record, index, total ->
                if (!isVirtualTime) return@start
                val value15 = record[12].toUByte().toInt()

                Log.d("RECORD_HEX", record.joinToString(" ") {
                    "%02X".format(it)
                })
                if (isVirtualTime) {
                    getTrainingData(record, true)
                }
                if (index == total) stopVirtualTime()
            }
        }
    }

    private fun stopVirtualTime() {
        if (!isVirtualTime) return

        if (virtualPhase().equals(Phase.COMPRESSION)){
            addEvent(1,System.currentTimeMillis())
        } else if (virtualPhase().equals(Phase.VENTILATION)){
            addEvent(11,System.currentTimeMillis())
        }
        isVirtualTime = false

        virtualJob?.cancel()
        compressionTimeoutJob?.cancel()
        sender?.stop()
        updateGuideText()
    }

    /* =========================
     * UI
     * ========================= */
    private fun updateGuideText() {


        Log.d("kimtest5555","update")



        if (isVirtualTime) {
            binding.tvGuide.text = "Waiting for your turn"
            Log.d("kimtest5555","return")
            return
        }


        val isMyTurn = isMyCompressionCycle(cycleCount)

        val isFirst = lastIsMyTurn == null
        val turnChanged = !isFirst && lastIsMyTurn != isMyTurn





        if (turnChanged) {
//            binding.switchGuideTextView.visibility = View.INVISIBLE
        }
        lastIsMyTurn = isMyTurn

        Log.d("kimtest5555","gogo ${isMyTurn} ## ${traineePhase()}")
        if (traineePhase().equals(Phase.COMPRESSION)){
            binding.tvGuide.text = "Start compressions"
        } else if (traineePhase().equals(Phase.VENTILATION)){
            binding.tvGuide.text = "Start ventilations"
        }





        if (isFirst) {
            if (isMyTurn) {
                binding.depthContainer.setMyTurn("${UserInfoManager.getFirstName(requireContext())} ${UserInfoManager.getLastName(requireContext())}")
                binding.ventContainer.setVirtualTurn()
                binding.depthView.setVirtualData(false)
                binding.handPositionView.setVirtualData(false)
                binding.speedView.setVirtualData(false)
                binding.ventView.setVirtualData(true)
            } else {
                binding.depthContainer.setVirtualTurn()
                binding.ventContainer.setMyTurn("${UserInfoManager.getFirstName(requireContext())} ${UserInfoManager.getLastName(requireContext())}")
                binding.depthView.setVirtualData(true)
                binding.handPositionView.setVirtualData(true)
                binding.speedView.setVirtualData(true)
                binding.ventView.setVirtualData(false)
            }
            return
        }




        if (!turnChanged) return





        if (isMyTurn) {
            // vent → chest

            binding.ventContainer.setVirtualTurn()
            binding.depthContainer.setMyTurn("${UserInfoManager.getFirstName(requireContext())} ${UserInfoManager.getLastName(requireContext())}")
            binding.depthView.setVirtualData(false)
            binding.handPositionView.setVirtualData(false)
            binding.speedView.setVirtualData(false)
            binding.ventView.setVirtualData(true)
        } else {
            // chest → vent
            binding.depthContainer.setVirtualTurn()
            binding.ventContainer.setMyTurn("${UserInfoManager.getFirstName(requireContext())} ${UserInfoManager.getLastName(requireContext())}")
            binding.depthView.setVirtualData(true)
            binding.handPositionView.setVirtualData(true)
            binding.speedView.setVirtualData(true)
            binding.ventView.setVirtualData(false)
        }
    }






    private fun updateFeedback(depth: Int, speed: Int, hand: Int) {
        if (isAedActive) return
        if (isVirtualTime) return

        binding.tvGuide.text = when {
            hand != 1 -> "Check position"
            depth < minDepthValue -> "Deeper"
            depth > maxDepthValue -> "Shallower" //Shallower
            speed > 120 -> "Slower"
            speed < 100 -> "Faster" //Faster
            else -> "Good"
        }
    }

    private fun updateVolumeGuide(volume: Int) {

        if (isAedActive) return
        if (isVirtualTime) return

        binding.tvGuide.text = when {
            volume < minVolume -> "Blow more"
            volume > maxVolume -> "Blow less"

            else -> "Good"
        }
    }


    private fun updateCompressionText() {
        if (compCount>chestCountGuide){
            binding.tvCompressionCount.text = "${chestCountGuide}/${chestCountGuide}"
        }else {
            binding.tvCompressionCount.text = "$compCount/${chestCountGuide}"
        }
    }

    private fun updateVentText() {
        if (ventCount >2){
            binding.tvVentCount.text = "2/2"
        }else {
            binding.tvVentCount.text = "$ventCount/2"
        }
    }

    private fun updateCycleText() {

        if (cycleCount != finishCycle + 1) {
            binding.button.post {
                expandWithAnimation(
                    binding.button,
                    "Cycle" + " ${cycleCount}/${finishCycle}",
                    cycleCount.toString()
                )
            }
        }
        binding.tvCycleCount.text = "Cycle $cycleCount"
        updateCompressionText()
        updateVentText()
    }

    private fun findMaxValue(data: ByteArray, start: Int, end: Int): Int? {
        if (start < 0 || end >= data.size) return null
        var max = data[start].toInt()
        for (i in start..end) if (data[i].toInt() > max) max = data[i].toInt()
        return max
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnTrainingFinishedListener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopVirtualTime()
        _binding = null
    }

    private fun expandWithAnimation(button: RoundedLinearLayout, text: String, cycleCount: String) {
        button.visibility = View.VISIBLE

        val startY = button.y + button.height
        val endY = button.y
        val startSize = 0
        val endSize = button.width

        val animator = ValueAnimator.ofFloat(startY, endY)
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            button.y = animatedValue

            val sizeProgress = (animatedValue - startY) / (startY - endY)
            val newSize = (startSize + (endSize - startSize) * sizeProgress).toInt()
            val params = button.layoutParams
            params.width = button.width
            button.layoutParams = params
        }

        animator.duration = 100
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                expand(button, text, cycleCount)
            }
        })
        animator.start()
    }

    private fun collapseWithAnimation(button: RoundedLinearLayout, text: String) {
        val targetY = button.y + button.height

        val animator = ValueAnimator.ofFloat(button.y, targetY)
        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Float
            button.y = animatedValue
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                button.visibility = View.INVISIBLE
                binding.cycleTextview.text = ""
                button.y = 0f
                val params = button.layoutParams
                params.width = originalWidth
                button.layoutParams = params
            }
        })
        animator.duration = 100
        animator.start()
    }


    private fun expand(button: RoundedLinearLayout, text: String, cycleCount: String) {

        if (!isAdded){
            return
        }
        originalWidth = binding.button.width
        val targetWidth = 400

        val animator = ValueAnimator.ofInt(button.width, targetWidth)
        animator.addUpdateListener { animation ->
            val params = button.layoutParams
            params.width = animation.animatedValue as Int
            button.layoutParams = params
        }
        animator.duration = 300
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                Handler().postDelayed({
                    if (isAdded) {
                        binding.cycleInfoImageView.visibility = View.VISIBLE
                        binding.cycleTextview.text = text
                    }
                }, 300)
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                Handler().postDelayed({
                    collapse(binding.button, cycleCount)
                }, 1500)
            }
        })

        animator.start()
    }

    private fun collapse(button: RoundedLinearLayout, text: String) {
        binding.cycleInfoImageView.visibility = View.GONE
        val animator = ValueAnimator.ofInt(button.width, originalWidth)
        animator.addUpdateListener { animation ->
            val params = button.layoutParams
            params.width = animation.animatedValue as Int
            button.layoutParams = params
        }
        binding.cycleTextview.text = ""
        animator.duration = 200

        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                collapseWithAnimation(binding.button, text)
            }
        })

        animator.start()
    }

}

/* =========================
 * FileRecordSender
 * ========================= */
class FileRecordSender(
    private val context: Context,
    private val assetFileName: String,
    private val recordSize: Int = 20,
    private val delayMillis: Long = 50
) {
    private var handler: Handler? = null
    private var index = 0
    private var isStopped = false

    fun start(callback: (record: ByteArray, index: Int, total: Int) -> Unit) {
        try {
            val data = context.assets.open(assetFileName).readBytes()
            val numRecords = data.size / recordSize
            val remainder = data.size % recordSize
            val totalRecords = if (remainder > 0) numRecords + 1 else numRecords

            handler = Handler(Looper.getMainLooper())
            index = 0
            isStopped = false

            fun sendNext() {
                if (isStopped) return
                if (index < numRecords) {
                    val start = index * recordSize
                    callback(data.copyOfRange(start, start + recordSize), index + 1, totalRecords)
                    index++
                    handler?.postDelayed({ sendNext() }, delayMillis)
                } else if (remainder > 0 && index == numRecords) {
                    callback(data.copyOfRange(numRecords * recordSize, data.size), totalRecords, totalRecords)
                }
            }
            sendNext()
        } catch (e: Exception) {
            Log.e("FileSender", "파일 읽기 실패", e)
        }
    }

    fun stop() {
        isStopped = true
        handler?.removeCallbacksAndMessages(null)
    }
}