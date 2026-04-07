package com.example.hstm_aos

import android.content.Context
import android.os.Handler
import android.os.Looper

object SessionManager {

    private const val TIMEOUT = 15 * 60 * 1000L

    private var handler: Handler? = null
    private var runnable: Runnable? = null

    fun start(context: Context, onTimeout: () -> Unit) {
        stop()

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            onTimeout()
        }

        handler?.postDelayed(runnable!!, TIMEOUT)
    }

    fun reset(context: Context, onTimeout: () -> Unit) {
        start(context, onTimeout)
    }

    fun stop() {
        runnable?.let { handler?.removeCallbacks(it) }
    }
}