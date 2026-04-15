package com.example.hstm_aos

import android.content.Context
import android.os.Handler
import android.os.Looper

object SessionManager {

    private const val TIMEOUT = 15 * 60 * 1000L
    private const val WARNING_TIME = TIMEOUT - (60 * 1000L)

    private var handler: Handler? = null
    private var timeoutRunnable: Runnable? = null
    private var warningRunnable: Runnable? = null

    fun start(
        context: Context,
        onTimeout: () -> Unit,
        onWarning: () -> Unit
    ) {
        stop()

        handler = Handler(Looper.getMainLooper())

        warningRunnable = Runnable {
            android.util.Log.d("SESSION", "1분 남음")
            onWarning()
        }

        timeoutRunnable = Runnable {
            onTimeout()
        }

        handler?.postDelayed(warningRunnable!!, WARNING_TIME)
        handler?.postDelayed(timeoutRunnable!!, TIMEOUT)
    }

    fun reset(
        context: Context,
        onTimeout: () -> Unit,
        onWarning: () -> Unit
    ) {
        start(context, onTimeout, onWarning)
    }

    fun stop() {
        warningRunnable?.let { handler?.removeCallbacks(it) }
        timeoutRunnable?.let { handler?.removeCallbacks(it) }
    }
}