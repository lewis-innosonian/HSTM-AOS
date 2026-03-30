package com.example.hstm_aos

import android.app.Application
import com.example.hstm_aos.ble.BleManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableSharedFlow

class MainApplication : Application() {

    lateinit var bleManager: BleManager
        private set

    companion object {
        lateinit var instance: MainApplication
            private set

        val ble: BleManager
            get() = instance.bleManager
    }
    val completedTrainingFlow = MutableSharedFlow<ContentsItem.Content>(replay = 0)

    override fun onCreate() {
        super.onCreate()
        instance = this

        bleManager = BleManager(this)


    }
}
