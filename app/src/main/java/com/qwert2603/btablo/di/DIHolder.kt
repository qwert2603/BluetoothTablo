package com.qwert2603.btablo.di

import android.annotation.SuppressLint
import android.content.Context
import com.qwert2603.andrlib.schedulers.ModelSchedulersProvider
import com.qwert2603.andrlib.schedulers.UiSchedulerProvider
import com.qwert2603.btablo.model.AnthTabloRepo
import com.qwert2603.btablo.model.SchedulersProviderImpl
import com.qwert2603.btablo.model.SettingsRepo
import com.qwert2603.permesso.Permesso

@SuppressLint("StaticFieldLeak")
object DIHolder {
    lateinit var appContext: Context

    val settingsRepo by lazy { SettingsRepo() }
    val anthTabloRepo by lazy { AnthTabloRepo() }


    private val schedulersProviderImpl = SchedulersProviderImpl()

    val modelSchedulersProvider: ModelSchedulersProvider = schedulersProviderImpl
    val uiSchedulerProvider: UiSchedulerProvider = schedulersProviderImpl

    val permesso: Permesso by lazy { Permesso.create(appContext) }
}