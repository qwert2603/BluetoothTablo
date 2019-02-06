package com.qwert2603.btablo.model

import android.os.Looper
import com.qwert2603.andrlib.schedulers.ModelSchedulersProvider
import com.qwert2603.andrlib.schedulers.UiSchedulerProvider
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class SchedulersProviderImpl : ModelSchedulersProvider, UiSchedulerProvider {
    override val ui: Scheduler = AndroidSchedulers.mainThread()
    override fun isOnUi() = Looper.myLooper() == Looper.getMainLooper()
    override val io: Scheduler = Schedulers.newThread()
    override val computation: Scheduler = Schedulers.computation()
}