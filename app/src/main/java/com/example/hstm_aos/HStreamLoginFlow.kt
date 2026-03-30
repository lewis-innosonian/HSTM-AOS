package com.example.hstm_aos.activity

import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HStreamLoginFlow(
    private val clientId: String,
    private val authorizeURL: String,
    private val tokenURL: String,
    private val redirectURI: String
) {

    private var oauthContinuation: Continuation<OAuthResult>? = null
    private lateinit var pkce: PKCE
    private lateinit var state: String

    suspend fun run(webView: WebView): OAuthResult =
        suspendCancellableCoroutine { cont ->
            oauthContinuation = cont

            pkce = generatePKCE()
            state = generateState()

            val authUrl = Uri.parse(authorizeURL).buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", redirectURI)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", "openid profile email")
                .appendQueryParameter("state", state)
                .appendQueryParameter("code_challenge", pkce.codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .build()
                .toString()

            Log.d("HStreamLoginFlow", "Loading OAuth URL: $authUrl")

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    url?.let {
                        if (it.startsWith(redirectURI)) {
                            handleCallback(it)
                            return true
                        }
                    }
                    return false
                }
            }

            webView.loadUrl(authUrl)

            cont.invokeOnCancellation {
                oauthContinuation = null
            }
        }

    private fun handleCallback(url: String) {
        val uri = Uri.parse(url)

        val idToken = uri.getQueryParameter("id_token")
        val accessToken = uri.getQueryParameter("access_token")
        val refreshToken = uri.getQueryParameter("refresh_token")

        if (idToken != null || accessToken != null) {
            oauthContinuation?.resume(HStreamLoginFlow.OAuthResult(idToken, accessToken, refreshToken))
            oauthContinuation = null
        } else {
            oauthContinuation?.resumeWithException(Exception("Tokens not found in redirect!"))
            oauthContinuation = null
        }
    }




    private fun exchangeCodeForToken(code: String) {
        try {
            val client = OkHttpClient()
            val formBody = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", clientId)
                .add("code", code)
                .add("redirect_uri", redirectURI)
                .add("code_verifier", pkce.codeVerifier)
                .build()

            val request = Request.Builder()
                .url(tokenURL)
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    val accessToken = json.optString("access_token")
                    val idToken = json.optString("id_token")
                    val refreshToken = json.optString("refresh_token")
                    oauthContinuation?.resume(OAuthResult(idToken, accessToken, refreshToken))
                } else {
                    oauthContinuation?.resumeWithException(Exception("Token request failed: ${response.code}"))
                }
                oauthContinuation = null
            }
        } catch (e: Exception) {
            oauthContinuation?.resumeWithException(e)
            oauthContinuation = null
        }
    }


    private fun generatePKCE(): PKCE {
        val secureRandom = SecureRandom()
        val codeVerifier = ByteArray(32).also { secureRandom.nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(codeVerifier.toByteArray(Charsets.US_ASCII))
        val codeChallenge = Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        return PKCE(codeVerifier, codeChallenge)
    }

    private fun generateState(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    data class PKCE(val codeVerifier: String, val codeChallenge: String)
    data class OAuthResult(val idToken: String?, val accessToken: String?, val refreshToken: String?)
}
