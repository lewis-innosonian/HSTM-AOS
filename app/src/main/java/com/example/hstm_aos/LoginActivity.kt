package com.example.hstm_aos

import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var showWebView by remember { mutableStateOf(false) }

                if (showWebView) {
                    LoginWebView(
                        url = "https://www.naver.com"
                    )
                } else {
                    WelcomeScreen(
                        onLoginClick = { showWebView = true }
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit
) {
    val scale = rememberScale()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
    ) {

        AnimatedLoginButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = scaleDp(20.dp, scale)),
            scale = scale,
            onClick = onLoginClick
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(
                    start = scaleDp(40.dp, scale),
                    end = scaleDp(40.dp, scale),
                    top = scaleDp(30.dp, scale),
                    bottom = scaleDp(20.dp, scale)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(id = R.drawable.hstm_icon),
                contentDescription = null
            )

            Spacer(modifier = Modifier.height(scaleDp(30.dp, scale)))

            Text(
                text = "Welcome to Resuscitation Skills",
                fontSize = 33.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = colorResource(id = R.color.black1)
            )

            Spacer(modifier = Modifier.height(scaleDp(20.dp, scale)))

            Text(
                text = """
                    Master your high quality CPR skills and earn digital certificates (BLS, ALS, PALS) with the
                    American Red Cross Resuscitation Suite and Brayden Pro manikins.
                """.trimIndent(),
                fontSize = 24.sp,
                lineHeight = 34.sp,
                color = colorResource(id = R.color.black2),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AnimatedLoginButton(
    modifier: Modifier = Modifier,
    scale: Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val density = LocalDensity.current

    val backScale by animateFloatAsState(
        targetValue = if (pressed) 1f else 1.05f,
        animationSpec = tween(120),
        label = ""
    )

    val backOffset by animateDpAsState(
        targetValue = if (pressed) 0.dp else scaleDp(2.dp, scale),
        animationSpec = tween(120),
        label = ""
    )

    val buttonHPadding = scaleDp(50.dp, scale)
    val buttonVPadding = scaleDp(10.dp, scale)
    val backExtra = scaleDp(2.dp, scale)

    var buttonWidthPx by remember { mutableStateOf(0) }
    var buttonHeightPx by remember { mutableStateOf(0) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = backScale
                    scaleY = backScale
                }
                .offset(y = backOffset)
                .width(with(density) { buttonWidthPx.toDp() + backExtra * 2 })
                .height(with(density) { buttonHeightPx.toDp() + backExtra * 2 })
                .background(
                    color = Color(0xFF0061F2).copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp)
                )
        )

        Surface(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .onSizeChanged {
                    buttonWidthPx = it.width
                    buttonHeightPx = it.height
                },
            color = Color(0xFF0061F2),
            shape = RoundedCornerShape(10.dp),
            shadowElevation = 6.dp
        ) {
            Text(
                text = "Login with hStream",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.padding(
                    horizontal = buttonHPadding,
                    vertical = buttonVPadding
                )
            )
        }
    }
}

@Composable
fun LoginWebView(
    url: String
) {
    val context = LocalContext.current

    //임시
    LaunchedEffect(Unit) {
        delay(3000)
        context.startActivity(
            Intent(context, MainActivity::class.java)
        )
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            WebView(it).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        }
    )
}

@Composable
fun rememberScale(): Float {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    return (screenWidth / 768f).coerceIn(0.7f, 1.2f)
}

fun scaleDp(base: Dp, scale: Float): Dp {
    return base * scale
}
