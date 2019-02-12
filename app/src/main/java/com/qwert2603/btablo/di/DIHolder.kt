package com.qwert2603.btablo.di

import android.annotation.SuppressLint
import android.content.Context
import com.qwert2603.andrlib.schedulers.ModelSchedulersProvider
import com.qwert2603.andrlib.schedulers.UiSchedulerProvider
import com.qwert2603.btablo.model.*
import com.qwert2603.permesso.Permesso

@SuppressLint("StaticFieldLeak")
object DIHolder {
    lateinit var appContext: Context

    private val bluetoothRepo: BluetoothRepo by lazy { BluetoothRepoImpl() }
    val bluetoothRepoActivityCallbacks by lazy { bluetoothRepo.activityCallbacks }
    private val tabloInterface: TabloInterface by lazy { TabloInterfaceImpl(bluetoothRepo) }
    val settingsRepo by lazy { SettingsRepo(tabloInterface) }

    private val schedulersProviderImpl = SchedulersProviderImpl()
    val modelSchedulersProvider: ModelSchedulersProvider = schedulersProviderImpl
    val uiSchedulerProvider: UiSchedulerProvider = schedulersProviderImpl

    val permesso: Permesso by lazy { Permesso.create(appContext) }
}