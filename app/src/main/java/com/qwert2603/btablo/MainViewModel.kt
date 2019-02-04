package com.qwert2603.btablo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val messages = MutableLiveData<String>()

    val sendingState: LiveData<SendingState> = messages
        .switchMap { DIHolder.tabloRepo.sendData(it) }

    fun send(message: String) {
        messages.value = message
    }
}