package com.qwert2603.btablo.tablo

sealed class SendingState {
    object NotSent : SendingState()
    object Sending : SendingState()
    data class Error(val t: Throwable) : SendingState()
    object Success : SendingState()
}