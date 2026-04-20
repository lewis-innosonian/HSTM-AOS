package com.example.hstm_aos.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.hstm_aos.FinishCourseDialog
import com.example.hstm_aos.LogoutInfoDialog
import com.example.hstm_aos.R
import com.example.hstm_aos.SessionManager
import com.example.hstm_aos.UserInfoManager
import java.util.WeakHashMap

open class BaseActivity : AppCompatActivity() {

    companion object {
        private val baseTextSizeMap = mutableMapOf<View, Float>()
    }


    private val processedViews = WeakHashMap<View, Boolean>()
    private var currentScale = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }


        val decor = window.decorView as ViewGroup

        decor.viewTreeObserver.addOnGlobalLayoutListener {
            applyToNewViews(decor)
        }

        window.statusBarColor = ContextCompat.getColor(this, R.color.black6)
    }

    override fun attachBaseContext(newBase: Context) {
        val metrics = newBase.resources.displayMetrics

        val newDensity = metrics.density * 0.85f

        val newMetrics = DisplayMetrics()
        newMetrics.setTo(metrics)
        newMetrics.density = newDensity
        newMetrics.scaledDensity = newDensity

        val config = Configuration(newBase.resources.configuration)
        val context = newBase.createConfigurationContext(config)
        context.resources.displayMetrics.setTo(newMetrics)

        super.attachBaseContext(context)
    }

    fun updateScale(scale: Float) {
        currentScale = scale

        val root = findViewById<ViewGroup>(android.R.id.content)
        applyScaleToAll(root)
    }

    private fun applyScaleToAll(view: View) {

        if (view is TextView) {

            val baseSize = baseTextSizeMap.getOrPut(view) {
                view.textSize / resources.displayMetrics.scaledDensity
            }

            view.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                baseSize * currentScale
            )
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyScaleToAll(view.getChildAt(i))
            }
        }
    }

    private fun applyToNewViews(view: View) {

        if (processedViews.containsKey(view)) return

        if (view is TextView) {

            val baseSize = baseTextSizeMap.getOrPut(view) {
                view.textSize / resources.displayMetrics.scaledDensity
            }

            view.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                baseSize * currentScale
            )

            processedViews[view] = true
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyToNewViews(view.getChildAt(i))
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        SessionManager.reset(
            this,
            onTimeout = { logout() },
            onWarning = { showSessionWarningDialog() }
        )
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()

        SessionManager.start(
            this,
            onTimeout = { logout() },
            onWarning = { showSessionWarningDialog() }
        )
    }

    override fun onPause() {
        super.onPause()
        SessionManager.stop()
    }

    private fun showSessionWarningDialog() {
        val dialog = LogoutInfoDialog.newInstance("").apply {
            setCallback {
                SessionManager.reset(
                    this@BaseActivity,
                    onTimeout = { logout() },
                    onWarning = { showSessionWarningDialog() }
                )
            }
        }
        dialog.show(supportFragmentManager, "logout_info")
    }

    private fun clearWebViewData() {
        val webView = WebView(this)
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()

        val cookieManager = android.webkit.CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    private fun logout() {
        clearWebViewData()
        UserInfoManager.clear(this)

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}