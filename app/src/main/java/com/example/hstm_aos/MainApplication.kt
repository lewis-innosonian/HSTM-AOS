package com.example.hstm_aos

import android.app.Application
import com.example.hstm_aos.ble.BleManager

class MainApplication : Application() {

    lateinit var bleManager: BleManager
        private set

    companion object {
        lateinit var instance: MainApplication
            private set

        val ble: BleManager
            get() = instance.bleManager
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        bleManager = BleManager(this)
    }
}
