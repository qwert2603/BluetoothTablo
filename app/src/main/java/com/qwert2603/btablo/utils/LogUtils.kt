package com.qwert2603.btablo.utils

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

object LogUtils {

    var APP_TAG = "AASSDD"
    private const val ERROR_MSG = "ERROR!!!"

    enum class LogType {
        NONE,
        ANDROID,
        ANDROID_ERRORS,
        SOUT,
        SOUT_ERRORS
    }

    var logType = LogType.ANDROID
        @Synchronized set

    var errorsFilter: (Throwable) -> Boolean = { true }

    var onErrorLogged: ((tag: String, msg: String, t: Throwable?) -> Unit)? = null

    fun d(msg: () -> String) {
        d(APP_TAG, msg)
    }

    fun d(tag: String, msg: () -> String) {
        when (logType) {
            LogUtils.LogType.NONE -> Unit
            LogUtils.LogType.ANDROID -> Log.d(tag, msg())
            LogUtils.LogType.ANDROID_ERRORS -> Unit
            LogUtils.LogType.SOUT -> println("$tag ${msg()}")
            LogUtils.LogType.SOUT_ERRORS -> Unit
        }
    }

    fun d(msg: String) {
        d(APP_TAG, msg)
    }

    fun d(tag: String, msg: String) {
        when (logType) {
            LogUtils.LogType.NONE -> Unit
            LogUtils.LogType.ANDROID -> Log.d(tag, msg)
            LogUtils.LogType.ANDROID_ERRORS -> Unit
            LogUtils.LogType.SOUT -> println("$tag $msg")
            LogUtils.LogType.SOUT_ERRORS -> Unit
        }
    }

    fun e(msg: String = ERROR_MSG, t: Throwable? = null) {
        e(APP_TAG, msg, t)
    }

    fun e(tag: String, msg: String, t: Throwable?) {
        if (t != null && !errorsFilter(t)) {
            val stringWriter = StringWriter()
            t.printStackTrace(PrintWriter(stringWriter))
            d(tag, "$msg ${t.message}\n" + stringWriter.toString())
            return
        }
        when (logType) {
            LogUtils.LogType.NONE -> Unit
            LogUtils.LogType.ANDROID -> Log.e(tag, "$msg $t", t)
            LogUtils.LogType.ANDROID_ERRORS -> Log.e(tag, "$msg $t", t)
            LogUtils.LogType.SOUT -> System.err.println("$tag $msg $t").also { t?.printStackTrace() }
            LogUtils.LogType.SOUT_ERRORS -> System.err.println("$tag $msg $t").also { t?.printStackTrace() }
        }
        onErrorLogged?.invoke(tag, msg, t)
    }

    fun printCurrentStack() {
        when (logType) {
            LogUtils.LogType.NONE -> Unit
            LogUtils.LogType.ANDROID -> Log.v(APP_TAG, "", Exception())
            LogUtils.LogType.ANDROID_ERRORS -> Unit
            LogUtils.LogType.SOUT -> println("$APP_TAG printCurrentStack").also { Exception().printStackTrace() }
            LogUtils.LogType.SOUT_ERRORS -> Unit
        }
    }

    @Synchronized
    fun <T> withErrorLoggingOnly(action: () -> T): T {
        val prev = logType
        logType = when (prev) {
            LogUtils.LogType.NONE -> LogUtils.LogType.NONE
            LogUtils.LogType.ANDROID -> LogUtils.LogType.ANDROID_ERRORS
            LogUtils.LogType.ANDROID_ERRORS -> LogUtils.LogType.ANDROID_ERRORS
            LogUtils.LogType.SOUT -> LogUtils.LogType.SOUT_ERRORS
            LogUtils.LogType.SOUT_ERRORS -> LogUtils.LogType.SOUT_ERRORS
        }
        try {
            return action()
        } finally {
            logType = prev
        }
    }

    @Synchronized
    fun <T> allowDebugLogging(action: () -> T): T {
        val prev = logType
        logType = when (prev) {
            LogUtils.LogType.NONE -> LogUtils.LogType.NONE
            LogUtils.LogType.ANDROID -> LogUtils.LogType.ANDROID
            LogUtils.LogType.ANDROID_ERRORS -> LogUtils.LogType.ANDROID
            LogUtils.LogType.SOUT -> LogUtils.LogType.SOUT
            LogUtils.LogType.SOUT_ERRORS -> LogUtils.LogType.SOUT
        }
        try {
            return action()
        } finally {
            logType = prev
        }
    }
}
