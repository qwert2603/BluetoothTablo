package com.qwert2603.bluetooth_tablo

import android.util.Log

object LogUtils {
    private const val TAG = "bluetooth_tablo"

    fun d(msg: String) {
        d(TAG, msg)
    }

    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    fun e(tag: String = TAG, msg: String = "error!", t: Throwable? = null) {
        Log.e(tag, msg, t)
    }
}