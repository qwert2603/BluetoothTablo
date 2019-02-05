package com.qwert2603.btablo.di

import android.content.Context
import com.qwert2603.andrlib.schedulers.ModelSchedulersProvider
import com.qwert2603.andrlib.schedulers.UiSchedulerProvider
import com.qwert2603.btablo.model.SchedulersProviderImpl
import com.qwert2603.btablo.model.SettingsRepo
import com.qwert2603.btablo.model.TabloRepo

object DIHolder {
    lateinit var appContext: Context

    val settingsRepo by lazy { SettingsRepo() }
    val tabloRepo by lazy { TabloRepo() }


    private val schedulersProviderImpl = SchedulersProviderImpl()

    val modelSchedulersProvider: ModelSchedulersProvider = schedulersProviderImpl
    val uiSchedulerProvider: UiSchedulerProvider = schedulersProviderImpl
}