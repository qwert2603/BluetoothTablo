package com.qwert2603.btablo.model

import android.annotation.SuppressLint
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.qwert2603.andrlib.util.renderIfChangedEqual
import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.tablo.SendingState
import com.qwert2603.btablo.tablo.TabloViewState
import com.qwert2603.btablo.utils.ObservableField
import com.qwert2603.btablo.utils.PreferenceUtils
import com.qwert2603.btablo.utils.StateHolderImpl
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class SettingsRepo(private val tabloInterface: TabloInterface) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(DIHolder.appContext)

    val vs: ObservableField<TabloViewState> = PreferenceUtils.createPrefsObjectObservable(
        prefs = prefs,
        key = "vs",
        gson = Gson(),
        defaultValue = TabloViewState.DEFAULT
    )

    private val stateHolder = object : StateHolderImpl<TabloViewState>(vs.field) {
        override fun render(vs: TabloViewState) {
            super.render(vs)

            renderIfChangedEqual({ team1 }) { tabloInterface.setTeam1Name(it).makeSend() }
            renderIfChangedEqual({ team2 }) { tabloInterface.setTeam2Name(it).makeSend() }

            renderIfChangedEqual({ minutes to seconds }) { (minutes, seconds) ->
                tabloInterface.setTime(minutes, seconds).makeSend()
            }

            renderIfChangedEqual({ points1 to points2 }) { (points1, points2) ->
                tabloInterface.setScore(points1, points2).makeSend()
            }

            renderIfChangedEqual({ period }) { tabloInterface.setPeriod(it).makeSend() }

            renderIfChangedEqual({ fouls1 to fouls2 }) { (fouls1, fouls2) ->
                tabloInterface.setFouls(fouls1, fouls2).makeSend()
            }

            renderIfChangedEqual({ timeouts1 to timeouts2 }) { (timeouts1, timeouts2) ->
                tabloInterface.setTimeouts(timeouts1, timeouts2).makeSend()
            }

            renderIfChangedEqual({ holdIsTeam2 }) { tabloInterface.setHolding(it).makeSend() }

            renderIfChangedEqual({ attackSeconds }) { tabloInterface.setTimeAttack(it, false).makeSend() }
        }
    }

    private val _isStarted = BehaviorSubject.createDefault(false)
    private val _isAttackStarted = BehaviorSubject.createDefault(false)

    val isStarted: Observable<Boolean> = _isStarted.hide()
    val isAttackStarted: Observable<Boolean> = _isAttackStarted.hide()

    private val messagesToSend = PublishSubject.create<Completable>()

    private val _sendingState = BehaviorSubject.createDefault<SendingState>(SendingState.NotSent)

    val sendingState: Observable<SendingState> = _sendingState.hide()

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

        vs.changes
            .skip(1)
            .subscribe { stateHolder.render(it) }

        messagesToSend
            .concatMap { completable ->
                completable
                    .toSingleDefault<SendingState>(SendingState.Success)
                    .onErrorReturn { SendingState.Error(it) }
                    .toObservable()
                    .startWith(SendingState.Sending)
            }
            .subscribe { _sendingState.onNext(it) }
    }

    fun sendAll() {
        stateHolder.renderAll()
    }

    fun prepareForSendAll() {
        stateHolder.resetPrevViewState()
    }

    fun sendTeams() {
        val tabloViewState = vs.field
        tabloInterface.setTeam1Name(tabloViewState.team1).makeSend()
        tabloInterface.setTeam2Name(tabloViewState.team2).makeSend()
    }

    fun setStarted(started: Boolean) {
        _isStarted.onNext(started)
    }

    fun setAttackStarted(attackStarted: Boolean) {
        _isAttackStarted.onNext(attackStarted)
    }

    private fun Completable.makeSend() {
        messagesToSend.onNext(this)
    }
}