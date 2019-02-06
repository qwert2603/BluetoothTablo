package com.qwert2603.btablo

import android.app.Application
import com.qwert2603.andrlib.util.LogUtils
import com.qwert2603.btablo.di.DIHolder

class BluetoothTabloApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DIHolder.appContext = this

        LogUtils.APP_TAG = "bluetooth_tablo"
    }
}