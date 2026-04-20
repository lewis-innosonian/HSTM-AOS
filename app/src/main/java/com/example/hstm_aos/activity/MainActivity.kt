package com.example.hstm_aos.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.hstm_aos.BuildConfig
import com.example.hstm_aos.ContentsItem
import com.example.hstm_aos.Fragment.GuideAndHelpFragment
import com.example.hstm_aos.Fragment.HomeFragment
import com.example.hstm_aos.GetSkillsResponseHolder
import com.example.hstm_aos.MainApplication
import com.example.hstm_aos.R
import com.example.hstm_aos.UserInfoManager
import com.example.hstm_aos.ble.BleManager
import com.example.hstm_aos.databinding.ActivityMainBinding
import com.example.hstm_aos.model.GetSkillsResponse
import com.example.hstm_aos.model.OpenSkill
import com.example.hstm_aos.model.Organization
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.FontScaleManager
import com.example.hstm_aos.adapter.OrganizationAdapter
import com.example.hstm_aos.ble.DfuService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.LinkedList
import java.util.Queue

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tabContainer: LinearLayout
    private val tabs = mutableListOf<View>()
    private val fragments = mutableListOf<Fragment>()
    private val docList = mutableListOf<GuideAndHelpFragment.GuideMenuItem>()
    private val bleManager: BleManager
        get() = MainApplication.ble

    companion object {
        private const val TAG = "kimtest"
    }

    //firmware Update Listener

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val dfuInProgressDevices = mutableSetOf<String>()
    private val dfuQueue: Queue<Pair<String, String>> = LinkedList()
    private var isDfuRunning = false

    private val dfuListener = object : DfuProgressListenerAdapter() {

        override fun onDeviceConnecting(deviceAddress: String) {
            Log.d("DFU", "onDeviceConnecting")
        }

        override fun onDfuProcessStarting(deviceAddress: String) {
            Log.d("DFU", "onDfuProcessStarting")
        }

        override fun onProgressChanged(
            deviceAddress: String,
            percent: Int,
            speed: Float,
            avgSpeed: Float,
            currentPart: Int,
            partsTotal: Int
        ) {
            Log.d("DFU", "onProgressChanged: $percent%")
        }

        override fun onDfuCompleted(deviceAddress: String) {
            Log.d("DFU", "onDfuCompleted")
            dfuInProgressDevices.remove(deviceAddress)
            startNextDfu()
        }

        override fun onError(deviceAddress: String, error: Int, errorType: Int, message: String) {
            Log.e("DFU", "onError: $message")
            dfuInProgressDevices.remove(deviceAddress)
            startNextDfu()
        }
    }


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
//        bleManager.startScan(clear = true)
        val step = getSharedPreferences("settings", MODE_PRIVATE)
            .getInt("font_step", 0)

        val scale = FontScaleManager.getScale(step)

        updateScale(scale)




//        requestBlePermission()
        bleManager.startScan(clear = true)

        val body = GetSkillsResponseHolder.rawResponse
        if (body.isNullOrBlank()) {
            Log.e(TAG, "getSkillsResponse is null")
            return
        }

        handleGetSkillsResponse(body)
    }

    override fun onResume() {
        super.onResume()
        bleManager.attachActivity(this)
//        bleManager.startScan(clear = true)
        DfuServiceListenerHelper.registerProgressListener(this, dfuListener)
    }

    override fun onPause() {
        super.onPause()
//        bleManager.stopScan()
        DfuServiceListenerHelper.unregisterProgressListener(this, dfuListener)
    }


    @SuppressLint("MissingPermission")
    private fun startDfuService(deviceAddress: String, url: String) {

        Log.d("DFU", "start DFU → $deviceAddress")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DfuServiceInitiator.createDfuNotificationChannel(this)
        }

        scope.launch {
            try {
                disconnectAndWait(deviceAddress)
                val file = downloadFirmware(url)
                startDfu(deviceAddress, file)

            } catch (e: Exception) {
                Log.e("DFU", "DFU ERROR", e)
            }
        }
    }

    //url 넣고 테스트
    fun addDfu(deviceAddress: String, url: String) {
        if (dfuInProgressDevices.contains(deviceAddress)) return

        dfuQueue.add(deviceAddress to url)

        if (!isDfuRunning) {
            startNextDfu()
        }
    }

    private fun startNextDfu() {
        val next = dfuQueue.poll() ?: run {
            isDfuRunning = false
            return
        }

        isDfuRunning = true
        startDfuService(next.first, next.second)
    }

    @SuppressLint("MissingPermission")
    private suspend fun disconnectAndWait(address: String) {
        val device = bleManager.getDeviceByAddress(address) ?: return

        if (device.isConnected) {
            bleManager.toggleConnection(device)
        }

        repeat(20) {
            delay(200)
            val current = bleManager.getDeviceByAddress(address)
            if (current?.isConnected == false) return
        }

        Log.d("DFU", "disconnect timeout")
    }


    private suspend fun downloadFirmware(url: String): File =
        withContext(Dispatchers.IO) {

            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->

                if (!response.isSuccessful) {
                    throw IOException("Download failed: $response")
                }

                val tempFile = File.createTempFile("dfu_temp", ".zip", cacheDir)
                tempFile.deleteOnExit()

                response.body?.byteStream()?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IOException("Empty response")

                return@withContext tempFile
            }
        }

    private fun startDfu(deviceAddress: String, file: File) {

        Log.d("kimtest4","StartDFU")
        val uri = Uri.fromFile(file)

        val initiator = DfuServiceInitiator(deviceAddress)
            .setKeepBond(true)
            .setRestoreBond(true)
            .setForceDfu(true)
            .setForeground(true)
            .setDisableNotification(false)
            .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
            .setZip(uri)

        initiator.start(
            this,
            DfuService::class.java
        )

        dfuInProgressDevices.add(deviceAddress)
    }


    private fun startDfuFromUrl(deviceAddress: String, fileUrl: String) {

        Thread {
            try {
                val url = URL(fileUrl)
                val connection = url.openConnection()
                connection.connect()

                val input = BufferedInputStream(url.openStream())
                val file = File(cacheDir, "dfu.zip")

                val output = FileOutputStream(file)
                val data = ByteArray(1024)
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                runOnUiThread {
                    Log.d("kimtest4","dfu Start")
                    startDfu(deviceAddress, file)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }


    //


    private fun handleGetSkillsResponse(body: String) {
        try {
            val raw = body.trim()

            if (!raw.startsWith("{") && !raw.startsWith("\"")) {
                Log.e(TAG, "Invalid JSON response: $raw")
                return
            }

            val json = if (raw.startsWith("\"")) {
                Gson().fromJson(raw, String::class.java)
            } else {
                raw
            }

            val result = Gson().fromJson(json, GetSkillsResponse::class.java)

            val organizations = result.hStream_user?.Organizations

            if (organizations.isNullOrEmpty()) {
                Log.e(TAG, "No organizations")
                return
            }

            if (organizations.size == 1) {
                processOrganization(result, organizations.first())
            } else {
                showOrganizationSelectDialog(result, organizations)
            }

        } catch (e: Exception) {
            Log.e(TAG, "getSkillsResponse parsing failed", e)
        }
    }

    private fun showOrganizationSelectDialog(
        result: GetSkillsResponse,
        orgs: List<Organization>
    ) {

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_select_organization)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0f)
        dialog.setCancelable(false)

        val recycler = dialog.findViewById<RecyclerView>(R.id.orgRecycler)
        val btnContinue = dialog.findViewById<Button>(R.id.btnContinue)

        val adapter = OrganizationAdapter(orgs)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        btnContinue.setOnClickListener {

            val selectedOrg = adapter.getSelectedOrg()

            dialog.dismiss()

            processOrganization(result, selectedOrg)
        }

        dialog.show()
    }


    private fun processOrganization(
        result: GetSkillsResponse,
        selectedOrg: Organization
    ) {
        binding.rootLayout.visibility = View.VISIBLE
        val firstInstitution = selectedOrg.Institutions?.firstOrNull()

        val skills = buildList {
            firstInstitution?.Open_Skill?.let { addAll(it) }
        }

        result.hStream_user?.Main_Video?.let {
            docList.add(
                GuideAndHelpFragment.GuideMenuItem(
                    title = "Main Video",
                    type = GuideAndHelpFragment.MenuType.VIDEO,
                    url = it
                )
            )
        }

        firstInstitution?.help_docs?.video?.takeIf { it.isNotEmpty() }?.let {
            docList.add(
                GuideAndHelpFragment.GuideMenuItem(
                    title = "Guideline Video",
                    type = GuideAndHelpFragment.MenuType.VIDEO,
                    url = it
                )
            )
        }

        firstInstitution?.help_docs?.documents?.forEach { doc ->
            val name = doc.document_name ?: return@forEach
            val fileStoreId = doc.file_store_id ?: ""
            val fileStoreOrgNodeId = doc.file_store_org_node_id ?: ""
            val localFile = File(filesDir, name)

            if (!localFile.exists()) {
                lifecycleScope.launch {
                    downloadDocument(
                        name,
                        fileStoreId,
                        fileStoreOrgNodeId,
                        selectedOrg.org_id ?: ""
                    )
                }
            }

            docList.add(
                GuideAndHelpFragment.GuideMenuItem(
                    title = name,
                    type = GuideAndHelpFragment.MenuType.TEXT,
                    localFileName = name,
                    fileStoreId = fileStoreId,
                    fileStoreOrgNodeId = fileStoreOrgNodeId
                )
            )
        }

        firstInstitution?.help_docs?.online_docs?.forEach {
            docList.add(
                GuideAndHelpFragment.GuideMenuItem(
                    title = it.display_name ?: "Document",
                    type = GuideAndHelpFragment.MenuType.WEB,
                    url = it.url ?: ""
                )
            )
        }

        UserInfoManager.setOrganizations(this, selectedOrg.org_name ?: "-")
        UserInfoManager.setFirstName(this, selectedOrg.First_name ?: "-")
        UserInfoManager.setLastName(this, selectedOrg.Last_name ?: "-")

        binding.userIDTextView.text =
            "${UserInfoManager.getFirstName(this)} ${UserInfoManager.getLastName(this)}, ${
                UserInfoManager.getOrganizations(
                    this
                )
            }"

        initUI()
        setupFragments(skills)

        val intent = Intent(this, GuideHelpActivity::class.java)
        intent.putExtra("documents", ArrayList(docList))
        startActivity(intent)

    }


    private fun setupFragments(skills: List<Any>?) {
        val homeFragment = HomeFragment().apply {
            arguments = Bundle().apply {
                putSerializable("skills", ArrayList(skills ?: emptyList()))
            }
        }

        val guideFragment = GuideAndHelpFragment().apply {
            arguments = Bundle().apply {
                putSerializable("documents", ArrayList(docList))
            }
        }

        fragments.clear()
        fragments.add(homeFragment)
        fragments.add(guideFragment)

        supportFragmentManager.beginTransaction().apply {
            fragments.forEach { add(R.id.fragmentContainer, it).hide(it) }
            show(homeFragment)
        }.commit()

        addTab("Home", R.drawable.inno_home_icon, homeFragment)
        addTab("Guide & Help", R.drawable.inno_question_mark_icon, guideFragment)
        selectTab(0,true)
    }


    private suspend fun downloadDocument(
        name: String,
        fileStoreId: String,
        fileStoreOrgNodeId: String,
        orgId: String
    ) = withContext(Dispatchers.IO) {
        try {
            val token = UserInfoManager.getAccessToken(this@MainActivity)
            val hStreamId = UserInfoManager.getHStreamId(this@MainActivity)

            val request = Request.Builder()
                .url("https://hlc-api.hstream.net/prod/api/hs/Resuscitation/V1/GetDocumentDetails")
                .get()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .addHeader("hStreamId", hStreamId ?: "")
                .addHeader("OrganizationId", orgId)
                .addHeader("FileStoreId", fileStoreId)
                .addHeader("FileStoreInstitutionId", fileStoreOrgNodeId)
                .build()

            val response = OkHttpClient().newCall(request).execute()
            val body = response.body?.string() ?: return@withContext

            val base64 = com.google.gson.JsonParser.parseString(body)
                .asJsonObject["FileStore"].asString

            File(filesDir, name).writeBytes(Base64.decode(base64, Base64.DEFAULT))

        } catch (e: Exception) {
            Log.e(TAG, " document download error", e)
        }
    }

    private fun openGuideAndHelpFragment(
        docList: List<GuideAndHelpFragment.GuideMenuItem>,
        fragment: Fragment
    ) {
        val index = fragments.indexOf(fragment)
        if (index >= 0) {
            selectTab(index)
            showFragment(fragment)
        }
    }


    fun setupLogoutButton() {

        clearWebViewData()

        UserInfoManager.clear(this@MainActivity)

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

    }

    private fun clearWebViewData() {
        val webView = WebView(this)
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()

        // 쿠키 삭제
        val cookieManager = android.webkit.CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    private fun initUI() {
        binding.logoutRoundedButton.setOnClickListener {
            setupLogoutButton()

//            val device = bleManager.getCurrentConnectedDevices().firstOrNull()
//                ?: return@setOnClickListener
//
//            startDfuFromUrl(device.device.address,"https://blog.kakaocdn.net/dna/bL4D8B/dJMcaipkYif/AAAAAAAAAAAAAAAAAAAAADz_lvawIHm_Wh0P9bH8yLBrDpyIUYbVCc9TgSYuV87e/app_dfu_package_3018D.zip?credential=yqXZFxpELC7KVnFOS48ylbz2pIh7yKj8&expires=1777561199&allow_ip=&allow_referer=&signature=GMbrvMN7LzsmJM%2B%2FERkCZRT%2Bi7I%3D&attach=1&knm=tfile.zip")


//
//            val firmware = assets.open("firmware2.bin").readBytes()
//
//            bleManager.startOta(device.device.address, firmware)

        }

//        binding.logoutRoundedButton1.setOnClickListener {
//            val device = bleManager.getCurrentConnectedDevices().firstOrNull()
//                ?: return@setOnClickListener
//
//            val firmware = assets.open("firmware1.bin").readBytes()
//
//            bleManager.startOta(device.device.address, firmware)
//        }

        binding.closeImageView.setOnClickListener {
            binding.guideHelpLayout.visibility = View.GONE
        }

        binding.guideHelpLayout.setOnClickListener {
            val guideFragment = fragments.getOrNull(1)
            guideFragment?.let { openGuideAndHelpFragment(docList, it) }
            binding.guideHelpLayout.visibility = View.GONE
        }

        binding.versionTextView.text =
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        tabContainer = findViewById(R.id.tabContainer)

        tabselectTab()

    }

    fun tabselectTab(){
        tabContainer.post {
            selectTab(0,true)
        }
    }
    private fun requestBlePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ), 1001
            )
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1002
            )
        }
    }

    fun showLottie(){
        binding.dimLayout.visibility = View.VISIBLE
        binding.passLottieView.visibility = View.VISIBLE
    }

    fun hideLottie(){
        binding.dimLayout.visibility = View.GONE
        binding.passLottieView.visibility = View.GONE
    }

    private fun addTab(title: String, iconRes: Int, fragment: Fragment) {
        val tab = layoutInflater.inflate(R.layout.view_custom_tab, tabContainer, false)
        val icon = tab.findViewById<ImageView>(R.id.tabIcon)
        val text = tab.findViewById<TextView>(R.id.tabText)

        icon.setImageResource(iconRes)
        text.text = title

        val index = tabs.size
        tabs.add(tab)

        tab.setOnClickListener {
            if (index == 1) {
                binding.guideHelpLayout.visibility = View.GONE
            }

            selectTab(index)
            showFragment(fragment)
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.marginEnd = (10 * resources.displayMetrics.density).toInt()
        tabContainer.addView(tab, params)
    }


    private fun selectTab(selectedIndex: Int,isScaling:Boolean= false) {
        tabs.forEachIndexed { index, view ->
            val icon = view.findViewById<ImageView>(R.id.tabIcon)

            view.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT

            if (index == selectedIndex) {
                view.setBackgroundResource(R.drawable.bg_tab_selected)
                icon.setColorFilter(Color.WHITE)
                view.setPadding(dp(20), dp(0), dp(20), dp(0))
                animateTabWidth(view, true,isScaling)
            } else {
                view.setBackgroundResource(R.drawable.bg_tab_unselected)
                icon.setColorFilter(Color.WHITE)
                view.setPadding(dp(10), dp(10), dp(10), dp(10))
                animateTabWidth(view, false,isScaling)
            }
        }
    }


    private fun animateTabWidth(view: View, expand: Boolean ,isScaling:Boolean = false) {
        val text = view.findViewById<TextView>(R.id.tabText)
        val icon = view.findViewById<ImageView>(R.id.tabIcon)

        val fixedHeight = view.height.takeIf { it > 0 } ?: dp(40)

        if (expand) {
            text.visibility = View.VISIBLE
            text.alpha = 0f
            text.translationX = -dp(4).toFloat()
        }

        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(fixedHeight, View.MeasureSpec.EXACTLY)
        )

        val startWidth = view.width
        val endWidth = if (expand) {
            view.measuredWidth
        } else {
            icon.measuredWidth + dp(20)
        }

//        if (isScaling) {
//            view.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
//            text.visibility = if (expand) View.VISIBLE else View.GONE
//            view.requestLayout()
//            return
//        }

        ValueAnimator.ofInt(startWidth, endWidth).apply {
            if (isScaling) {
                duration = 0
            }else {
                duration = 220
            }
            addUpdateListener { anim ->
                val w = anim.animatedValue as Int
                view.layoutParams = view.layoutParams.apply {
                    width = w
                    height = fixedHeight
                }
                view.requestLayout()

                if (expand && anim.animatedFraction > 0.1f && text.alpha == 0f) {
                    text.animate()
                        .alpha(1f)
                        .translationX(0f)
                        .setDuration(50)
                        .start()
                }
            }

            start()
        }

        if (!expand) {
            text.visibility = View.GONE
        }
    }


    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            fragments.forEach { hide(it) }
            show(fragment)
        }.commit()
    }

    private fun dp(dp: Int) = (dp * resources.displayMetrics.density).toInt()
}
