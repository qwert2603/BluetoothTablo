package com.qwert2603.btablo.di

import android.annotation.SuppressLint
import android.content.Context
import com.qwert2603.andrlib.schedulers.ModelSchedulersProvider
import com.qwert2603.andrlib.schedulers.UiSchedulerProvider
import com.qwert2603.btablo.model.TabloRepo
import com.qwert2603.btablo.model.SchedulersProviderImpl
import com.qwert2603.btablo.model.SettingsRepo
import com.qwert2603.permesso.Permesso

@SuppressLint("StaticFieldLeak")
object DIHolder {
    lateinit var appContext: Context

    val settingsRepo by lazy { SettingsRepo() }
    val tabloRepo by lazy { TabloRepo() }


    private val schedulersProviderImpl = SchedulersProviderImpl()

    val modelSchedulersProvider: ModelSchedulersProvider = schedulersProviderImpl
    val uiSchedulerProvider: UiSchedulerProvider = schedulersProviderImpl

    val permesso: Permesso by lazy { Permesso.create(appContext) }
}