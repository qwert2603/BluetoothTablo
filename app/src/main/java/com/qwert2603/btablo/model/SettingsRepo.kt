package com.qwert2603.btablo.model

import android.annotation.SuppressLint
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.tablo.TabloViewState
import com.qwert2603.btablo.utils.ObservableField
import com.qwert2603.btablo.utils.PreferenceUtils
import com.qwert2603.btablo.utils.StateHolderImpl
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class SettingsRepo {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(DIHolder.appContext)

    val vs: ObservableField<TabloViewState> = PreferenceUtils.createPrefsObjectObservable(
        prefs = prefs,
        key = "vs",
        gson = Gson(),
        defaultValue = TabloViewState.DEFAULT
    )

    private val stateHolder = object : StateHolderImpl<TabloViewState>() {
        override fun render(vs: TabloViewState) {
            super.render(vs)
            //todo
        }
    }

    private var _isStarted = BehaviorSubject.createDefault(false)
    private var _isAttackStarted = BehaviorSubject.createDefault(false)

    val isStarted: Observable<Boolean> = _isStarted.hide()
    val isAttackStarted: Observable<Boolean> = _isAttackStarted.hide()

    init {
        makeInit()
    }

    @SuppressLint("CheckResult")
    private fun makeInit() {
        _isStarted
            .switchMap {
                if (it) {
                    Observable.interval(0, 1, TimeUnit.SECONDS)
                } else {
                    Observable.never()
                }
            }
            .subscribe { i ->
                val vs = if (i > 0) {
                    vs.updateField { it.decSecond() }
                } else {
                    vs.field
                }
                if (vs.totalSeconds() == 0) {
                    setStarted(false)
                }
            }

        _isAttackStarted
            .switchMap {
                if (it) {
                    Observable.interval(0, 1, TimeUnit.SECONDS)
                } else {
                    Observable.never()
                }
            }
            .subscribe { i ->
                val vs = if (i > 0) {
                    vs.updateField { it.decAttackSecond() }
                } else {
                    vs.field
                }
                if (vs.attackSeconds == 0) {
                    setAttackStarted(false)
                }
            }

        vs.changes.subscribe { stateHolder.render(it) }
    }

    fun sendAll() {
        _isStarted.onNext(false)
        _isAttackStarted.onNext(false)
        stateHolder.renderAll()
    }

    fun sendTeams() {
        //todo
    }

    fun setStarted(started: Boolean) {
        _isStarted.onNext(started)
    }

    fun setAttackStarted(attackStarted: Boolean) {
        _isAttackStarted.onNext(attackStarted)
    }
}