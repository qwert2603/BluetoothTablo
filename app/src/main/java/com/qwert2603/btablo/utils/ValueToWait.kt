package com.qwert2603.btablo.utils

class ValueToWait<T>(
    @Volatile private var value: T
) {
    @Volatile
    private var version: Long = 1

    fun makeUpdate(nextValue: T) {
        synchronized(this) {
            value = nextValue
            ++version
        }
    }

    fun waitNext(initUpdateAction: () -> Unit): T {
        val prevVersion = version

        initUpdateAction()

        while (prevVersion == version) Thread.yield()

        return value
    }
}