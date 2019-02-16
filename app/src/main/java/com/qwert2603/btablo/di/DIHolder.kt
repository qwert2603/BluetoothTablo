package com.qwert2603.btablo.di

import android.annotation.SuppressLint
import android.content.Context
import com.qwert2603.btablo.model.*
import com.qwert2603.permesso.Permesso
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers

@SuppressLint("StaticFieldLeak")
object DIHolder {
    lateinit var appContext: Context

    private val bluetoothRepo: BluetoothRepo by lazy { BluetoothRepoImpl() }
    val bluetoothRepoActivityCallbacks by lazy { bluetoothRepo.activityCallbacks }
    private val tabloInterface: TabloInterface by lazy { TabloInterfaceImpl(bluetoothRepo) }
    val settingsRepo by lazy { SettingsRepo(tabloInterface) }

    val uiScheduler: Scheduler = AndroidSchedulers.mainThread()

    val permesso: Permesso by lazy { Permesso.create(appContext) }
}