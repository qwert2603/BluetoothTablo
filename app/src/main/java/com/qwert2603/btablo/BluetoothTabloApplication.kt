package com.qwert2603.btablo

import android.app.Application
import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.utils.LogUtils
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

/* todo:
 * в main.c сирена не выключается (отправляется только 0xFF).
 * как передавать период -- число или символ?
 * проверить есть ли MAC адрес у HC-06.
 */