package com.qwert2603.btablo.model

import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.utils.LogUtils
import io.reactivex.Completable

class TabloRepo {

    fun sendData(message: String): Completable = Completable
        .fromAction {
            LogUtils.d("TabloRepo sendData $message")
            Thread.sleep(2000)
//            if (Random.nextBoolean()) throw Exception("stub!")
        }
        .subscribeOn(DIHolder.modelSchedulersProvider.io)
}