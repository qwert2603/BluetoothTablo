package com.qwert2603.btablo.model

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Completable

interface BluetoothRepo {
    val activityCallbacks: ActivityCallbacks
    fun sendData(command: TabloConst.Command,text: ByteArray): Completable

    interface ActivityCallbacks {
        fun onActivityCreate(activity: AppCompatActivity)
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    }
}