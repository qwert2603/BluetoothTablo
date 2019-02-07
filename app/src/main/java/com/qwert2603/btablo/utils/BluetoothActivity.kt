package com.qwert2603.btablo.utils

import android.content.Intent
import android.os.Bundle
import com.qwert2603.andrlib.base.mvi.BaseActivity
import com.qwert2603.andrlib.base.mvi.BasePresenter
import com.qwert2603.andrlib.base.mvi.BaseView
import com.qwert2603.btablo.di.DIHolder

abstract class BluetoothActivity<VS : Any, V : BaseView<VS>, P : BasePresenter<V, VS>> : BaseActivity<VS, V, P>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DIHolder.anthTabloRepo.onActivityCreate(this)
        lifecycle.addObserver(DIHolder.permesso.activityCallbacks.createActivityLifecycleObserver())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        DIHolder.anthTabloRepo.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        DIHolder.permesso.activityCallbacks.onPermissionResult(requestCode, permissions, grantResults)
    }
}