package com.example.hstm_aos.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.SweepGradient
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.example.hstm_aos.ConfigManager
import com.example.hstm_aos.GetSkillsResponseHolder
import com.example.hstm_aos.R
import com.example.hstm_aos.UserInfoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import android.graphics.PathMeasure
import android.webkit.WebSettings
import androidx.activity.compose.BackHandler
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.hstm_aos.BuildConfig
import kotlin.math.atan2
import kotlin.math.sqrt

val InterFontFamily = FontFamily(
    Font(R.font.inter_thin, weight = FontWeight.Thin),
    Font(R.font.inter_extralight, weight = FontWeight.ExtraLight),
    Font(R.font.inter_light, weight = FontWeight.Light),
    Font(R.font.inter_regular, weight = FontWeight.Normal),
    Font(R.font.inter_medium, weight = FontWeight.Medium),
    Font(R.font.inter_semibold, weight = FontWeight.SemiBold),
    Font(R.font.inter_bold, weight = FontWeight.Bold),
    Font(R.font.inter_extrabold, weight = FontWeight.ExtraBold),
    Font(R.font.inter_black, weight = FontWeight.Black)
)


class LoginActivity : ComponentActivity() {
    private val BLE_PERMISSION_REQUEST = 2001
    private val TAG = "LoginActivity"
    private var tokenHandled = false
    private lateinit var onErrorHandler: (String) -> Unit // ⭐ ADD

    private val CONFIG_URL =
        "https://api.braydenlab.com/config/hstream?type=prod"

    private val API_KEY =
        "VyBTUiIjtY4M0x1EIHCkqazPOORAyQNG48D8E5pm"

    private lateinit var pkce: PKCE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 상태바 아이콘을 검정색으로
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        requestBlePermission()
        pkce = generatePKCE()

        setContent {
            MaterialTheme {

                var showWebView by remember { mutableStateOf(false) }
                var configReady by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                var loading by remember { mutableStateOf(true)}

                onErrorHandler = { msg -> errorMessage = msg }

                LaunchedEffect(Unit) {
                    loading = true
                    fetchConfig(
                        onDone = {
                            configReady = true
                            loading = false
                        },
                        onError = {
                            errorMessage = it
                            loading = false
                        }
                    )
                }

                Box(modifier = Modifier.fillMaxSize()) {

                    if (showWebView && configReady && errorMessage == null) {
                        OAuthWebView(
                            onError = { errorMessage = it },
                            onClose = { showWebView = false }
                        )
                    } else {
                        WelcomeScreen(
                            enabled = configReady,
                            onLoginClick = { showWebView = true }
                        )
                    }

                    // ⭐ 공통 로딩
//                    if (loading) {
//                        Box(
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .background(Color.Black.copy(alpha = 0.2f)),
//                            contentAlignment = Alignment.Center
//                        ) {
////                            CircularProgressIndicator()
//                        }
//                    }

                    // ⭐ 공통 에러 팝업
                    if (errorMessage != null) {
                        AlertDialog(
                            onDismissRequest = { errorMessage = null },
                            confirmButton = {
                                Text(
                                    "확인",
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .clickable {
                                            errorMessage = null
                                            showWebView = false
                                        }
                                )
                            },
                            title = { Text("오류") },
                            text = { Text(errorMessage!!) }
                        )
                    }
                }
            }
        }
    }


    private fun hasBlePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBlePermission() {
        if (hasBlePermission()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                BLE_PERMISSION_REQUEST
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                BLE_PERMISSION_REQUEST
            )
        }
    }

    // ⭐ ADD : onError 콜백만 추가 (기존 로직 유지)
    private fun fetchConfig(
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(CONFIG_URL)
                    .addHeader("x-api-key", API_KEY)
                    .get()
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }
                val body = response.body?.string() ?: throw Exception("Empty body")
                val json = JSONObject(body)

                ConfigManager.clientId = json.optString("ClientId")
                ConfigManager.clientSecret = json.optString("ClientSecret")
                ConfigManager.cognitoRegion =
                    json.optString("CognitoIdentityUserPoolRegion")
                ConfigManager.cognitoUserPoolId =
                    json.optString("CognitoIdentityUserPoolId")
                ConfigManager.cognitoAppClientId =
                    json.optString("CognitoIdentityUserPoolAppClientId")
                ConfigManager.cognitoAppClientSecret =
                    json.optString("CognitoIdentityUserPoolAppClientSecret")
                ConfigManager.signInProviderKey =
                    json.optString("AWSCognitoUserPoolsSignInProviderKey")

                ConfigManager.getSkillsURL =
                    json.optString("GetSkillsURL")
                ConfigManager.sendResultURL =
                    json.optString("SendResultURL")
                ConfigManager.forgetPassURL =
                    json.optString("ForgetPassURL")
                ConfigManager.authorizeURL =
                    json.optString("AuthorizeURL")
                ConfigManager.accessTokenURL =
                    json.optString("AccessTokenURL")
                ConfigManager.documentURL =
                    json.optString("DocumentURL")

                ConfigManager.getSkillsURL = json.optString("GetSkillsURL").also {
                    Log.d(TAG, "getSkillsURL = $it")
                }

                val calcArray = json.optJSONArray("CalcURL")
                val list = mutableListOf<String>()
                calcArray?.let {
                    for (i in 0 until it.length()) {
                        list.add(it.optString(i))
                    }
                }
                ConfigManager.calcURL = list

                withContext(Dispatchers.Main) { onDone() }

            } catch (e: Exception) {
                Log.e(TAG, "Config error", e)
                withContext(Dispatchers.Main) {
                    onError("설정 정보를 불러오지 못했습니다.\n네트워크 또는 서버를 확인해주세요.")
                }
            }
        }
    }


    @Composable
    fun OAuthWebView(
        onError: (String) -> Unit,
        onClose: () -> Unit
    ) {
        var redirecting by remember { mutableStateOf(false) }
        val webViewState = remember { mutableStateOf<WebView?>(null) }
        var loading by remember { mutableStateOf(true) }

        BackHandler {

            webViewState.value?.let { webView ->
                onClose()
            } ?: onClose()
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            AndroidView(
                modifier = Modifier.fillMaxSize()
                    .imePadding(),

                factory = { context ->

                    WebView(context).apply {

                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        webViewState.value = this

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true

                        settings.setSupportZoom(false)
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false

                        webViewClient = object : WebViewClient() {

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                loading = true
                            }

                            override fun onPageFinished(
                                view: WebView?,
                                url: String?
                            ) {
                                loading = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                url: String?
                            ): Boolean {

                                if (url?.startsWith("braydenprotest://oauth/callback") == true) {

                                    redirecting = true
                                    loading = true

                                    handleRedirect(url)
                                    return true
                                }

                                return false
                            }
                        }

                        val authUrl = Uri.parse(ConfigManager.authorizeURL).buildUpon()
                            .appendQueryParameter("client_id", ConfigManager.clientId)
                            .appendQueryParameter(
                                "redirect_uri",
                                "braydenprotest://oauth/callback"
                            )
                            .appendQueryParameter("response_type", "id_token token")
                            .appendQueryParameter("scope", "openid profile email")
                            .appendQueryParameter("code_challenge", pkce.codeChallenge)
                            .appendQueryParameter("code_challenge_method", "S256")
                            .appendQueryParameter("nonce", generateNonce())
                            .build()
                            .toString()

                        loadUrl(authUrl)
                    }
                }
            )
            if (!redirecting) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {

                    Row(
                        modifier = Modifier
                            .height(56.dp)
                            .clickable {

                                webViewState.value?.let { webView ->
                                    onClose()
                                } ?: onClose()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Spacer(modifier = Modifier.width(20.dp))

                        Row(
                            modifier = Modifier
                                .height(56.dp)
                                .clickable {
                                    webViewState.value?.let { webView ->
                                        onClose()
                                    } ?: onClose()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Image(
                                painter = painterResource(id = R.drawable.inno_login_back),
                                contentDescription = "Back",
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "Back",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = InterFontFamily,
                                textAlign = TextAlign.Center,
                                color = colorResource(id = R.color.black1),
                            )
                        }
                    }
                }
            }

            if (loading||redirecting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    private fun handleRedirect(url: String) {
        Log.d(TAG, "handleRedirect URL: $url") // 🔹 전체 URL 출력

        if (tokenHandled) return
        tokenHandled = true

        val uri = Uri.parse(url)
        val map = mutableMapOf<String, String>()

        uri.queryParameterNames.forEach {
            uri.getQueryParameter(it)?.let { v ->
                map[it] = v
                Log.d(TAG, "Query param: $it = $v") // 🔹 쿼리 파라미터
            }
        }

        uri.fragment?.split("&")?.forEach {
            val p = it.split("=")
            if (p.size == 2) {
                map[p[0]] = p[1]
                Log.d(TAG, "Fragment param: ${p[0]} = ${p[1]}") // 🔹 fragment 파라미터
            }
        }

        onTokenReceived(
            map["access_token"],
            map["id_token"],
            map["refresh_token"]
        )
    }


    private fun onTokenReceived(
        accessToken: String?,
        idToken: String?,
        refreshToken: String?
    ) {
        Log.d(TAG, "onTokenReceived: accessToken=$accessToken")
        Log.d(TAG, "onTokenReceived: idToken=$idToken")
        Log.d(TAG, "onTokenReceived: refreshToken=$refreshToken")

        val claims = decodeJwt(idToken ?: return)
        Log.d(TAG, "JWT Claims: $claims")

        val hstreamId = claims.optString("hstreamid")
        val name = "${claims.optString("given_name")} ${claims.optString("family_name")}"
        Log.d(TAG, "User hstreamId=$hstreamId, name=$name")

        UserInfoManager.saveTokens(
            this,
            accessToken,
            idToken,
            refreshToken,
            hstreamId,
        )

        callGetSkillsApi(onErrorHandler)
    }

    private fun callGetSkillsApi(onError: (String) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val accessToken = UserInfoManager.getAccessToken(this@LoginActivity)
                val hstreamId = UserInfoManager.getHStreamId(this@LoginActivity)
                Log.d(TAG, "callGetSkillsApi: accessToken=$accessToken, hstreamId=$hstreamId")

                val request = Request.Builder()
                    .url(ConfigManager.getSkillsURL)
                    .get()
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("hStreamId", hstreamId ?: "")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("ManikinDeviceId", "unknow")
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                Log.d(TAG, "callGetSkillsApi: HTTP ${response.code}")

                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")

                val body = response.body?.string() ?: throw Exception("Empty response")
                Log.d(TAG, "callGetSkillsApi response body: $body")

                GetSkillsResponseHolder.rawResponse = body

                withContext(Dispatchers.Main) {
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.putExtra("getSkillsResponse", body)
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "GetSkills API error", e)
                withContext(Dispatchers.Main) {
                    onError(
                        "스킬 정보를 불러오지 못했습니다.\n" +
                                "네트워크 상태 또는 서버를 확인해주세요."
                    )
                }
            }
        }
    }

    private fun decodeJwt(jwt: String): JSONObject {

        val parts = jwt.split(".")
        if (parts.size < 2) return JSONObject()

        val decoded = Base64.decode(
            parts[1],
            Base64.URL_SAFE or
                    Base64.NO_PADDING or
                    Base64.NO_WRAP
        )

        return JSONObject(String(decoded))
    }


    data class PKCE(
        val codeVerifier: String,
        val codeChallenge: String
    )

    private fun generatePKCE(): PKCE {

        val verifier = ByteArray(32)
            .also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }

        val hash = MessageDigest
            .getInstance("SHA-256")
            .digest(verifier.toByteArray())

        val challenge = Base64.encodeToString(
            hash,
            Base64.URL_SAFE or
                    Base64.NO_PADDING or
                    Base64.NO_WRAP
        )

        return PKCE(verifier, challenge)
    }

    private fun generateNonce(): String =
        Base64.encodeToString(
            ByteArray(16).also { SecureRandom().nextBytes(it) },
            Base64.URL_SAFE or
                    Base64.NO_PADDING or
                    Base64.NO_WRAP
        )
}


@Composable
fun WelcomeScreen(
    enabled: Boolean,
    onLoginClick: () -> Unit
) {
    val scale = rememberScale()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
    ) {

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 심장 아이콘
            Image(
                painter = painterResource(id = R.drawable.hstm_icon),
                contentDescription = null,
                modifier = Modifier.size(scaleDp(60.dp, scale))
            )

            Spacer(Modifier.height(scaleDp(20.dp, scale)))

            Text(
                text = "Welcome to Resuscitation Skills",
                fontSize = 33.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = InterFontFamily,
                textAlign = TextAlign.Center,
                color = colorResource(id = R.color.black1),
                modifier = Modifier.padding(horizontal = scaleDp(100.dp, scale))
            )

            Spacer(Modifier.height(scaleDp(16.dp, scale)))

            Text(
                text = "Master your high quality CPR skills and earn digital certificates (BLS, ALS, PALS) with the American Red Cross Resuscitation Suite and Brayden Pro manikins.\n" +
                        "To complete your skills checks on your facility's dedicated self-directed manikin and Resuscitation.\n" +
                        "Register for your hStream ID from your learning management system before attempting to access skills checks.",
                fontSize = 24.sp,
                color = colorResource(id = R.color.black2),
                fontWeight = FontWeight.Medium,
                fontFamily = InterFontFamily,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp,
                modifier = Modifier.padding(horizontal = scaleDp(100.dp, scale))
            )

            Spacer(Modifier.height(scaleDp(30.dp, scale)))

            AnimatedLoginButton(
                enabled = enabled,
                modifier = Modifier,
                scale = scale,
                onClick = onLoginClick
            )
        }

        // 하단 영역 그대로 유지
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = scaleDp(20.dp, scale), vertical = scaleDp(16.dp, scale)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = InterFontFamily,
                color = colorResource(id = R.color.black1)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Certification Provided By",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = InterFontFamily,
                    color = colorResource(id = R.color.black1)
                )

                Spacer(modifier = Modifier.width(scaleDp(4.dp, scale)))

                Image(
                    painter = painterResource(id = R.mipmap.red_cross_logo),
                    contentDescription = null,
                )
            }
        }
    }
}


@Composable
fun AnimatedLoginButton(
    enabled: Boolean,
    modifier: Modifier,
    scale: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    var w by remember { mutableStateOf(0) }
    var h by remember { mutableStateOf(0) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {


//        if (w > 0 && h > 0) {
//            val extra = scaleDp(5.dp, scale)
//            val density = LocalDensity.current
//
//            val transition = rememberInfiniteTransition()
//            val progress by transition.animateFloat(
//                initialValue = -1f,
//                targetValue = 1f,
//                animationSpec = infiniteRepeatable(
//                    animation = tween(2000, easing = LinearEasing)
//                ),
//                label = "linearGradientMove"
//            )
//
//            Box(
//                modifier = Modifier
//                    .width(with(density) { w.toDp() } + extra * 2)
//                    .height(with(density) { h.toDp() } + extra * 2)
//                    .align(Alignment.Center)
//            ) {
//                Canvas(modifier = Modifier.matchParentSize()) {
//
//                    val strokeWidth = 8.dp.toPx()
//                    val inset = strokeWidth / 2
//                    val corner = 10.dp.toPx()
//
//                    val rectTopLeft = Offset(inset, inset)
//                    val rectSize = Size(
//                        size.width - inset * 2,
//                        size.height - inset * 2
//                    )
//
//                    drawRoundRect(
//                        color = Color(0xFF0061F2).copy(alpha = 0.4f),
//                        topLeft = rectTopLeft,
//                        size = rectSize,
//                        style = Stroke(width = strokeWidth),
//                        cornerRadius = CornerRadius(corner, corner)
//                    )
//
//                    val startX = rectTopLeft.x + rectSize.width * progress
//                    val endX = startX + rectSize.width / 3
//
//                    val brush = Brush.linearGradient(
//                        colors = listOf(
//                            Color.Transparent,
//                            Color.White.copy(alpha = 0.6f),
//                            Color.Transparent
//                        ),
//                        start = Offset(startX, rectTopLeft.y),
//                        end = Offset(endX, rectTopLeft.y)
//                    )
//
//                    drawRoundRect(
//                        brush = brush,
//                        topLeft = rectTopLeft,
//                        size = rectSize,
//                        style = Stroke(width = strokeWidth),
//                        cornerRadius = CornerRadius(corner, corner)
//                    )
//                }
//            }
//        }

        Surface(
            modifier = Modifier
                .width(400.dp)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() },
            color = Color(0xFF0061F2),
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 0.dp
        ) {
            Text(
                text = "Login with hStream",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily,
                fontSize = 20.sp,
                modifier = Modifier.padding(
                    vertical = 18.dp,
                    horizontal = 0.dp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun rememberScale(): Float {
    val w = LocalConfiguration.current.screenWidthDp
    return (w / 768f).coerceIn(0.7f, 1.2f)
}

fun scaleDp(base: Dp, scale: Float): Dp = base * scale
