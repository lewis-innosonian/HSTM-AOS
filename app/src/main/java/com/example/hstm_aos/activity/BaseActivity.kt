package com.example.hstm_aos.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hstm_aos.FinishCourseDialog
import com.example.hstm_aos.LogoutInfoDialog
import com.example.hstm_aos.R
import com.example.hstm_aos.SessionManager
import com.example.hstm_aos.UserInfoManager

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        window.statusBarColor = ContextCompat.getColor(this, R.color.black6)
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        SessionManager.reset(
            this,
            onTimeout = { logout() },
            onWarning = { showSessionWarningDialog() }
        )
        return super.dispatchTouchEvent(ev)
    }

    private fun getTouchedView(view: View, x: Int, y: Int): View? {

        val location = IntArray(2)
        view.getLocationOnScreen(location)

        val left = location[0]
        val top = location[1]
        val right = left + view.width
        val bottom = top + view.height

        if (x < left || x > right || y < top || y > bottom) return null

        if (view is android.view.ViewGroup) {
            for (i in view.childCount - 1 downTo 0) {
                val child = view.getChildAt(i)
                val target = getTouchedView(child, x, y)
                if (target != null) return target
            }
        }

        return view
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
        UserInfoManager.clear(this@BaseActivity)

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}