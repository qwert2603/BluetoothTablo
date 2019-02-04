package com.qwert2603.btablo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.concurrent.thread
import kotlin.random.Random

class TabloRepo {

    fun sendData(message: String): LiveData<SendingState> {
        LogUtils.d("TabloRepo sendData $message")
        val states = MutableLiveData<SendingState>()
        states.value = SendingState.SENDING
        thread {
            Thread.sleep(1000)
            states.postValue(
                if (Random.nextBoolean()) {
                    SendingState.SUCCESS
                } else {
                    SendingState.ERROR
                }
            )
        }
        return states
    }

}