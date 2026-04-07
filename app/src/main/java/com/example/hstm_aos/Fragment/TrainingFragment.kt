package com.example.hstm_aos.Fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.hstm_aos.CprAnalysisApi
import com.example.hstm_aos.R
import com.example.hstm_aos.RetryDialog
import com.example.hstm_aos.UserInfoManager
import com.example.hstm_aos.activity.TrainingActivity
import com.example.hstm_aos.ble.DeviceType
import com.example.hstm_aos.ble.TrainingType
import com.example.hstm_aos.customview.RoundedLinearLayout
import com.example.hstm_aos.databinding.FragmentTrainingBinding
import com.example.hstm_aos.model.HstmResponse
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.experimental.and
import kotlin.math.max

class TrainingFragment : Fragment(R.layout.fragment_training) {

    private var _binding: FragmentTrainingBinding? = null
    private val binding get() = _binding!!

    private var compCount = 0
    private var ventCount = 0
    private var cycleCount = 0

    private var isPeak = false
    private var lastValue = 0

    private var isVentPeak = false
    private var lastVentValue = 0

    private var lastSpeed = 0

    private var peakDepth = 0
    private var peakVolume = 0
    private var peakHandPoint = 0
    private lateinit var trainingTypes: Set<TrainingType>
    private var trainingType: String = ""
    private lateinit var mannequinTypes : Set<DeviceType>
    private var mannequinType: String = ""
    private var requestTrainingType: String = ""
    private var isAedActive = false
    private var animator: ValueAnimator? = null
    private var isPadsAttach = false
    private var cpr_cycle_type = "302"
    private var hexBuf: CopyOnWriteArrayList<ByteArray> = CopyOnWriteArrayList()
    private var aedHexBuf: CopyOnWriteArrayList<ByteArray> = CopyOnWriteArrayList()
    private var minDepthValue = 50
    private var maxDepthValue = 60

    private var minVolume = 400
    private var maxVolume = 700

    private var chestCountGuide = 30

    val byteArray = byteArrayOf(0xb4.toByte(), 0x00.toByte())

    private var lastActionTime = System.currentTimeMillis()
    private var handsOffCount = 0
    private var isHandsOff = false

    private val HANDS_OFF_INTERVAL = 3000L
    var hasAction = false

    var isesult = false
    private var handsOffStartTime = 0L
    private var lastHandsOffTick = 0L


    private var exoPlayer: ExoPlayer? = null
    private var currentSoundRes: Int? = null
    private var originalWidth = 0

    private var timer: CountDownTimer? = null

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
        ): TrainingFragment {
            return TrainingFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(KEY_TRAINING_TYPE, HashSet(types))
                    putSerializable(KEY_MANNEQUIN_TYPE,HashSet(mannequinType)) // 전달
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTrainingBinding.bind(view)

        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        resetHandsOffTimer()




        hexBuf = CopyOnWriteArrayList()
        aedHexBuf = CopyOnWriteArrayList()

        hexBuf.clear()
        aedHexBuf.clear()

        val activity = requireActivity() as TrainingActivity

        trainingTypes =
            (arguments?.getSerializable("KEY_TRAINING_TYPE") as? HashSet<TrainingType>)
                ?: emptySet()

        mannequinTypes =
            (arguments?.getSerializable(KEY_MANNEQUIN_TYPE) as? HashSet<DeviceType>)
                ?: emptySet()


        if (mannequinTypes.contains(DeviceType.BABY)){
            mannequinType = "infant"
            cpr_cycle_type = "152"
        } else if (mannequinTypes.contains(DeviceType.CHILD)){
            mannequinType = "child"
        } else if (mannequinTypes.contains(DeviceType.PRO)){
            mannequinType = "adult"
        }


        if (trainingTypes.contains(TrainingType.CPR)) {
            trainingType = "CPR"
            requestTrainingType = "cpr"
            binding.eduGuideText.text = "Perform 3 cycles of CPR on the manikin"
        }
        if (trainingTypes.contains(TrainingType.CCO)) {
            trainingType = "CCO"
            requestTrainingType = "compression_only"
            binding.eduGuideText.text = "Perform chest compressions on the manikin"
            binding.tvCycleCount.text = "Count"
            binding.volumeTextView.setTextColor(Color.parseColor("#EEEEEE"))
            binding.volumeTextView.compoundDrawablesRelative.forEach { drawable ->
                drawable?.mutate()?.setTint(Color.parseColor("#EEEEEE"))
            }

            binding.ventCountTextView.visibility = View.GONE
        }
        if (trainingTypes.contains(TrainingType.VO)) {
            trainingType = "vent"
            requestTrainingType = "ventilation_only"
            binding.eduGuideText.text = "Perform ventilations on the manikin"
            binding.tvCycleCount.text = "Count"
            binding.compCountLayout.visibility = View.GONE


            binding.rateTextView.setTextColor(Color.parseColor("#EEEEEE"))
            binding.rateTextView.compoundDrawablesRelative.forEach { drawable ->
                drawable?.mutate()?.setTint(Color.parseColor("#EEEEEE"))
            }

//            binding.depthTextView.setTextColor(Color.parseColor("#EEEEEE"))
//            binding.depthTextView.compoundDrawablesRelative.forEach { drawable ->
//                drawable?.mutate()?.setTint(Color.parseColor("#EEEEEE"))
//            }

            binding.positionTextView.setTextColor(Color.parseColor("#EEEEEE"))
            binding.positionTextView.compoundDrawablesRelative.forEach { drawable ->
                drawable?.mutate()?.setTint(Color.parseColor("#EEEEEE"))
            }
        }

        if (trainingTypes.contains(TrainingType.AED)) {
            trainingType = "CPR"
            requestTrainingType = "Perform 3 cycles of CPR with AED-T on the manikin"
            binding.eduGuideText.text = "Perform ventilations on the manikin"
        }





        if (mannequinType.equals("infant")){
            minDepthValue = 30
            maxDepthValue = 40

            minVolume = 200
            maxVolume = 400

            chestCountGuide = 15
            binding.ventView.setNormalRange(200..400)

            binding.ventView.setBadgeVisible(true)
        }


//        binding.depthView.setNormalRange(minDepthValue,maxDepthValue)
        updateCompressionText()
        updateVentText()

//        binding.depthView.setTrainingType(trainingType)
        binding.handPositionView.setTrainingType(trainingType)
        binding.handPositionView.setManikinType(mannequinType)
        binding.speedView.setTrainingType(trainingType)
        binding.ventView.setTrainingType(trainingType)
        viewLifecycleOwner.lifecycleScope.launch {
            activity.blePacketFlow.collect { (_, data) ->
                if (data.isNotEmpty() && String.format("%02X", data[0]) == "A8") {
                    getTrainingData(data)
                }
                lifecycleScope.launch(Dispatchers.Main) {
                    if (data.isNotEmpty() && String.format("%02X", data[0]) == "B4") { //init!
                        val timestamp = System.currentTimeMillis()
                        val timestampBytes = ByteBuffer.allocate(Long.SIZE_BYTES).putLong(timestamp).array()

                        aedHexBuf.add(data + timestampBytes)
                        Log.d("kimtest444", "${String.format("%02X", data[1])}")
//                    aedHexBuf.add(data + timestampBytes)
                        if (data.isNotEmpty() && String.format("%02X", data[1]) == "01") {
                            showAedLayout()
                            cancelAndFill(binding.turnOnAedProgress, 100)
                        }

                        if (data.isNotEmpty() && String.format("%02X", data[1]) == "03") {
                            binding.tvGuide.text = "Attach Pads"
//                    pickerNext(5)
                        }

                        if (data.isNotEmpty() && (String.format(
                                "%02X",
                                data[1]
                            ) == "08")
                        ) { // 패즈를 환자의 가슴에 붙이세요,
                            padOnAttath()
                        }

                        if (data.isNotEmpty() && String.format("%02X", data[1]) == "0A") {
                            isPadsAttach = true
                            cancelAndFill(binding.pasAedProgress, 100)
                            binding.pasAedImageView.setImageResource(R.drawable.attachpads_able)
                        }

                        if (data.isNotEmpty() && String.format("%02X", data[1]) == "0B") { //패드 떨어짐

                            if (isPadsAttach) {
                                isPadsAttach = false
                                cancelAndFill(binding.pasAedProgress, 100)
                                detachPads()
                            }
                        }




                        if (data.isNotEmpty() && String.format("%02X", data[1]) == "0D") {
                            analyzingHeartRhythm()
                        }


                        if (data.isNotEmpty() && String.format("%02X", data[1]) == "0C") { // 물러나세요

                        }

                        if (data.isNotEmpty() && String.format(
                                "%02X",
                                data[1]
                            ) == "15"
                        ) { //쇼크가 필요없음
                            cancelAndFill(binding.analyzingHeartRhythmAedprogressBar, 100)
                            noShockAble()
                        }

                        if (data.isNotEmpty() && String.format("%02X", data[1]) == "0E") { //쇼크가 필요함
                            cancelAndFill(binding.analyzingHeartRhythmAedprogressBar, 100)
                            binding.shockImageView.setImageResource(R.drawable.shock_able)
                        }

                        if (data.isNotEmpty() && String.format("%02X", data[1]) == "0F") { //버튼 눌러줘
                            shockAble()
                        }

                        if (data.isNotEmpty() && String.format("%02X", data[1]) == "10") {  // 쇼크전달
                            cancelAndFill(binding.shockProgress, 100)

                        }

                        if (data.isNotEmpty() && String.format(
                                "%02X",
                                data[1]
                            ) == "18"
                        ) {  // 패즈 떨어짐
//                    detachPads()
                        }


                        if (data.isNotEmpty() && String.format(
                                "%02X",
                                data[1]
                            ) == "12"
                        ) { //환자를 만져도 안전합니다.

                        }

                        if (data.isNotEmpty() && String.format("%02X", data[1]) == "13") { //CPR 실시
                            lifecycleScope.launch(Dispatchers.Main) {
                                binding.cprImageView.setImageResource(R.drawable.start_cpr_able)
                                endAed()
//
//                            aedFinishAble = true

//                            cycleUpdate()


//                            if (trainingCardData.cycle_limit != null && trainingCardData.cycle_limit != 0 && trainingCardData.cycle_limit < cycleCount && !trainingFinishAble) {
//                                Log.d("kimtest22", "2222")
//                                (requireActivity() as ArcMainActivity).stopTraining(
//                                    courseItem?.step!!
//                                )
//                            }


                            }
                        }
                    }
                }
            }
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
            if (trainingTypes.contains(TrainingType.AED)) {
                startAedTimer()
            }
            onFinish()
        }
    }

    private fun resetHandsOffTimer() {
        val now = System.currentTimeMillis()
        lastActionTime = now
        handsOffStartTime = 0L
        lastHandsOffTick = 0L
        handsOffCount = 0
        isHandsOff = false
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
        _binding = null
    }


    private fun playSound(res: Int, isVoice: Boolean = true) {


        if (isVoice && !UserInfoManager.getVoiceGuide(requireContext())){
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

    //타이머

     fun startTimer(seconds: Int) {

        stopTimer()

        timer = object : CountDownTimer(seconds * 1000L, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                val min = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                val sec = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                binding.timerText.text = String.format("%02d:%02d", min, sec)
            }

            override fun onFinish() {
                binding.timerText.text = "00:00"
                (activity as? TrainingActivity)?.startStopButtonPerformClick()
//                binding.startStopButton.performClick()
            }
        }.start()
    }

     fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun startAedTimer() {
        binding.waitingAed.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(10_000)
            showAedLayout()
            val timestamp = System.currentTimeMillis()
            val timestampBytes =
                ByteBuffer.allocate(Long.SIZE_BYTES).putLong(timestamp).array()
            aedHexBuf.add(byteArray + timestampBytes)
        }
    }

    private fun endAed() {
        binding.llGuide.setBackgroundColorInt(Color.parseColor("#F5F5F5"))
        binding.tvGuide.setTextColor(Color.parseColor("#333333"))
        isAedActive = false
        binding.aedContainerLayout.visibility = View.GONE
        cycleCount++
        updateCycleText()

    }

    private fun padOnAttath() {
        activity?.runOnUiThread {
            binding.pasAedImageView.setImageResource(R.drawable.attachpads_able)
            binding.tvGuide.text = "Attach pads"
            cancelAndFill(binding.turnOnAedProgress, 100)
            animateProgress(binding.pasAedProgress, 100)
        }
    }


    private fun analyzingHeartRhythm() {
//        if (binding.aedContainerLayout.visibility != View.VISIBLE) {
//            binding.cprImageView.setImageResource(R.drawable.start_cpr_able)
//            binding.shockImageView.setImageResource(R.drawable.question_mark_disable)
//            binding.analyzingHeartRhythmAedImageView.setImageResource(R.drawable.analyzing_disable)
//            aedAble()
//        }
//        if (binding.aedguideView.visibility != View.VISIBLE) {
//            showAedGuideView()
//        }
        activity?.runOnUiThread {
            binding.analyzingHeartRhythmAedImageView.setImageResource(R.drawable.analyzing_able)
            binding.tvGuide.text = "Analyzing heart rhythm"
            cancelAndFill(binding.turnOnAedProgress, 100)
            cancelAndFill(binding.pasAedProgress, 100)
            cancelAndFill(binding.analyzingHeartRhythmAedprogressBar, 0)
            cancelAndFill(binding.shockProgress, 0)
            animateProgress(binding.analyzingHeartRhythmAedprogressBar, 100)
        }
    }

    private fun shockAble() {
        activity?.runOnUiThread {
            binding.tvGuide.text = "Press shock button"
            cancelAndFill(binding.turnOnAedProgress, 100)
            cancelAndFill(binding.pasAedProgress, 100)
            cancelAndFill(binding.analyzingHeartRhythmAedprogressBar, 100)
            animateProgress(binding.shockProgress, 100)
        }
    }

    private fun noShockAble() {
        activity?.runOnUiThread {
            binding.shockImageView.setImageResource(R.drawable.noshock_able)
            binding.tvGuide.text = "No shock advised"
            cancelAndFill(binding.turnOnAedProgress, 100)
            cancelAndFill(binding.pasAedProgress, 100)
            cancelAndFill(binding.analyzingHeartRhythmAedprogressBar, 100)
            cancelAndFill(binding.shockProgress, 100)
//            animateProgress(binding.shockProgress, 100)
        }
    }

    private fun detachPads() {
        activity?.runOnUiThread {
            binding.shockImageView.setImageResource(R.drawable.question_mark_disable)
            binding.analyzingHeartRhythmAedImageView.setImageResource(R.drawable.analyzing_disable)
            binding.pasAedImageView.setImageResource(R.drawable.attachpads_able)
            binding.tvGuide.text = "Attach pads"
            cancelAndFill(binding.pasAedProgress, 0)
            cancelAndFill(binding.analyzingHeartRhythmAedprogressBar, 0)
            cancelAndFill(binding.shockProgress, 0)
            animateProgress(binding.pasAedProgress, 100)
        }
    }

    private fun showAedLayout() {
        isAedActive = true
        binding.waitingAed.visibility = View.GONE
        binding.llGuide.setBackgroundColorInt(Color.parseColor("#1AFFB881"))
        binding.tvGuide.setTextColor(Color.parseColor("#FB6B24"))
        binding.aedContainerLayout.apply {

            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .start()
        }

        binding.tvGuide.text = "Turn on the AED-T"

        binding.turnOnAedImageView.setImageResource(R.drawable.power_able)
        animateProgress(binding.turnOnAedProgress, 100)


    }

    private fun cancelAndFill(progressBar: ProgressBar, progressTo: Int) {
        animator?.cancel()
        progressBar.progress = progressTo
    }

    private fun animateProgress(progressBar: ProgressBar, progressTo: Int) {
        if (animator?.isRunning == true) return

        animator = ValueAnimator.ofInt(progressBar.progress, progressTo).apply {
            duration = 10000
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                progressBar.progress = animation.animatedValue as Int
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    progressBar.progress = progressTo
//                    animator = null
                }
            })

            start()
        }
    }

    fun getTrainingData(data: ByteArray) {

        val chestCompression =
            findMaxValue(data, 1, 10)?.toUByte()?.toInt()

        val chestCompressionSpeed = data[11].toUByte().toInt()
        lastSpeed = chestCompressionSpeed

        val ventilationPacket =
            findMaxValue(data, 14, 15)?.toUByte()?.toInt()

        var chestCompressionPoint = data[13]
        if (mannequinType.equals("infant", ignoreCase = true)) {
            chestCompressionPoint = chestCompressionPoint and 0x0F
        }

        val chestCompressionCountPack = data[12].toUByte().toInt()
        val ventilationCountPack = data[17].toUByte().toInt()

        val ventilationRate = data[16].toUByte().toInt()
        val alarmBit = (ventilationRate shr 7) and 0x01

        val timestamp = System.currentTimeMillis()
        val timestampBytes = ByteBuffer
            .allocate(Long.SIZE_BYTES)
            .putLong(timestamp)
            .array()

        hexBuf.add(data + timestampBytes)


        if (chestCompression != null && !trainingTypes.contains(TrainingType.VO)) {
            val value = chestCompression / 2
            if (chestCompression > 20) {
//                binding.depthView.addValue(value)
                detectCompressionPeak(value, chestCompressionPoint.toInt(),chestCompressionSpeed)
                hasAction = true
            } else {
//                binding.depthView.addValue(0)
            }
        }

        if (ventilationPacket != null && !trainingTypes.contains(TrainingType.CCO)) {
            val ventValue = ventilationPacket * 10
            binding.ventView.addValue(ventValue)
            detectVentPeak(ventValue, alarmBit)
            hasAction = true
        }

        binding.speedView.moveBarTo(chestCompressionSpeed)

        // ==========================
        // 3️⃣ HANDS-OFF 판단
        // ==========================

        if (!isAedActive) {
            val validAction = when (trainingType.lowercase()) {
                "cpr" -> {
                    (chestCompression != null && chestCompression > 20) ||
                            (ventilationPacket != null && ventilationPacket > 0)
                }

                "cco" -> {
                    chestCompression != null && chestCompression > 20
                }

                "vent" -> {
                    ventilationPacket != null && ventilationPacket > 0
                }

                else -> false
            }

            val currentTime = System.currentTimeMillis()

            if (validAction) {
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
                        binding.tvGuide.text = "Hands Off Time 1s"
                    } else {
                        if (currentTime - lastHandsOffTick >= 1000) {
                            handsOffCount++
                            lastHandsOffTick = currentTime
                            binding.tvGuide.text = "Hands Off Time ${handsOffCount}s"
                        }
                    }
                }
            }
        }


        hasAction = false

        // ==========================
        // 4️⃣ 종료 조건
        // ==========================
        if (trainingType.equals("CPR")) {
            if (cycleCount >= 3 && ventCount >= 2 && !isesult) {
                isesult = true
                lifecycleScope.launch {
                    (activity as? TrainingActivity)?.stopTraining()
                    val result = callApiAndGetResult()
                    if (result != null) {
                        listener?.onTrainingFinished(result)
                    }
                }
            }

        } else if (trainingType.equals("CCO")) {
            if (compCount >= 60&& !isesult) {
                isesult = true
                lifecycleScope.launch {
                    (activity as? TrainingActivity)?.stopTraining()
                    val result = callApiAndGetResult()
                    if (result != null) {
                        listener?.onTrainingFinished(result)
                    }
                }
            }

        } else if (trainingType.equals("vent")) {
            if (ventCount >= 12 && !isesult) {
                isesult = true
                lifecycleScope.launch {
                    (activity as? TrainingActivity)?.stopTraining()
                    val result = callApiAndGetResult()
                    if (result != null) {
                        listener?.onTrainingFinished(result)
                    }
                }
            }
        }
    }


    private fun createSingleFile(hexBuf: CopyOnWriteArrayList<ByteArray>, fileName: String): File {
        val combinedByteArray = hexBuf.flatMap { it.asList() }.toByteArray()

        val file = File(requireContext().filesDir, fileName)
        file.writeBytes(combinedByteArray)

        return file
    }

    suspend fun callApiAndGetResult(): HstmResponse? {
        Log.d("kimtest444","@#@?#@#???")
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
                val aedFile = createSingleFile(aedHexBuf, "aedTraining.bin")
                val rawFilePart = MultipartBody.Part.createFormData(
                    "rawHexBPfile",
                    singleFile.name,
                    RequestBody.create(mediaType, singleFile)
                )
                val aedFilePart= MultipartBody.Part.createFormData(
                    "aedHexBPfile",
                    aedFile.name,
                    RequestBody.create(mediaType, aedFile)
                )

                fun toRequestBody(value: String): RequestBody =
                    RequestBody.create("text/plain".toMediaTypeOrNull(), value)

                // API 호출
                val response: Response<HstmResponse> = api.finishTraining(
                    condition = toRequestBody(
                        """{
                    "mode": "training",
                    "target": "${mannequinType}",
                    "training_type": "${requestTrainingType}",
                    "guideline": "AHA2020",
                    "cpr_cycle_type": "${cpr_cycle_type}",
                    "is_2rescuers": false
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
                    aedHexBPfile = aedFilePart
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
    private fun detectCompressionPeak(value: Int, handPoint: Int, rate : Int = 0) {

        if (value > lastValue) {
            isPeak = true
            peakDepth = value
            peakHandPoint = handPoint
        }

        if (isPeak && value < lastValue) {

            if (cycleCount == 0 && trainingType.equals("CPR")){
                cycleCount++
                updateCycleText()
            }
            compCount++


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

            updateCompressionText()

            binding.handPositionView.updateState(peakHandPoint)
            updateGuide(peakDepth, lastSpeed, peakHandPoint)

            if (ventCount >= 1) {
                cycleCount++
                if (trainingType.contains("cpr", true)) {
                    updateCycleText()
                }
                updateVentText()
            }

            isPeak = false
        }

        lastValue = value
    }

    private fun detectVentPeak(value: Int,alarmBit : Int) {

        if (mannequinType.equals("infant", ignoreCase = true)) {

            if (alarmBit.toInt() == 0) {
//                    ventSpeedSuccess = true
            } else if (alarmBit.toInt() == 1) {
                binding.ventView.highlightBadgeRed()
            }
        }

        if (value > lastVentValue) {
            isVentPeak = true
            peakVolume = value
        }

        if (isVentPeak && value < lastVentValue) {
            ventCount++

            if (mannequinType.equals("infant", ignoreCase = true)) {

                if (alarmBit.toInt() == 0) {
                    updateVolumeGuide(peakVolume)
                } else if (alarmBit.toInt() == 1) {
                    binding.tvGuide.text = "Blow softer"
                }
            }else {updateVolumeGuide(peakVolume)}
            updateVentText()
            isVentPeak = false
        }

        lastVentValue = value
    }

    private fun updateGuide(depth: Int, speed: Int, hand: Int) {

        if (isAedActive) return

        val isHandOk = hand == 1
        val isDepthOk = depth in 50..60
        val isSpeedOk = speed in 100..120

        binding.tvGuide.text = when {
            !isHandOk -> "Check position"
            depth < minDepthValue -> "Deeper"
            depth > maxDepthValue -> "Shallower" //Shallower
            speed > 120 -> "Slower" //Faster
            speed < 100 -> "Faster"
            else -> "Good"
        }
    }

    private fun updateVolumeGuide(volume: Int) {

        if (isAedActive) return


        if (volume < minVolume){
            playSound(R.raw.under,false)
        }else if (volume > maxVolume){
            playSound(R.raw.incorrect,false)
        }else {
            playSound(R.raw.correct,false)
        }
        binding.tvGuide.text = when {
            volume < minVolume -> "Blow more"
            volume > maxVolume -> "Blow less" //Too few

            else -> "Good"
        }
    }



    private fun updateCompressionText() {
        if (trainingType.equals("CPR")) {
            binding.tvCompressionCount.text = "$compCount/${chestCountGuide}"
        } else if (trainingType.equals("CCO")) {
            binding.tvCompressionCount.text = "$compCount/60"
        }
    }

    private fun updateVentText() {
        if (trainingType.equals("CPR")) {
            binding.tvVentCount.text = "$ventCount/2"
        } else if (trainingType.equals("vent")) {
            binding.tvVentCount.text = "$ventCount/12"
        }
    }

    private fun updateCycleText() {

        if (cycleCount != 4) {
            binding.button.post {
                expandWithAnimation(
                    binding.button,
                    "Cycle" + " ${cycleCount}/3",
                    cycleCount.toString()
                )
            }
        }

        ventCount = 0
        compCount = 0
        binding.tvCycleCount.text = "Cycle $cycleCount"
        updateVentText()
        updateCompressionText()
    }

    private fun findMaxValue(data: ByteArray, start: Int, end: Int): Int? {
        if (start < 0 || end >= data.size) return null

        var maxInRange = data[start].toInt()

        for (i in start..end) {
            val v = data[i].toInt()
            if (v > maxInRange) maxInRange = v
        }
        return maxInRange
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
