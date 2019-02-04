package com.qwert2603.bluetooth_tablo

import android.app.Application
import android.content.Context

class BluetoothTabloApplication : Application() {
    companion object {
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
    }
}