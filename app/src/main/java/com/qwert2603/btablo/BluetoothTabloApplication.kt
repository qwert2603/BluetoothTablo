package com.qwert2603.btablo

import android.app.Application
import com.qwert2603.andrlib.util.LogUtils
import com.qwert2603.btablo.di.DIHolder
import io.reactivex.plugins.RxJavaPlugins

class BluetoothTabloApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DIHolder.appContext = this

        LogUtils.APP_TAG = "bluetooth_tablo"
        LogUtils.logType =
            if (BuildConfig.DEBUG) {
                LogUtils.LogType.ANDROID
            } else {
                LogUtils.LogType.NONE
            }

        RxJavaPlugins.setErrorHandler {
            LogUtils.e("RxJavaPlugins.setErrorHandler", it)
        }
    }
}