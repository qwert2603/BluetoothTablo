package com.qwert2603.btablo.utils

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.qwert2603.btablo.di.DIHolder

abstract class BluetoothActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DIHolder.bluetoothRepo.activityCallbacks.onActivityCreate(this)
        lifecycle.addObserver(DIHolder.permesso.activityCallbacks.createActivityLifecycleObserver())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        DIHolder.bluetoothRepo.activityCallbacks.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        DIHolder.permesso.activityCallbacks.onPermissionResult(requestCode, permissions, grantResults)
    }
}