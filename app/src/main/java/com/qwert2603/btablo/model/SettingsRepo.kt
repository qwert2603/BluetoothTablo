package com.qwert2603.btablo.model

import android.annotation.SuppressLint
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.mac_settings.MacSettings
import com.qwert2603.btablo.tablo.SendingState
import com.qwert2603.btablo.tablo.TabloViewState
import com.qwert2603.btablo.utils.ObservableField
import com.qwert2603.btablo.utils.PreferenceUtils
import com.qwert2603.btablo.utils.StateHolderImpl
import com.qwert2603.btablo.utils.renderIfChangedEqual
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class SettingsRepo(private val tabloInterface: TabloInterface) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(DIHolder.appContext)

    var macSettings: MacSettings by PreferenceUtils.createPrefsObject(
        prefs = prefs,
        key = "macSettings",
        gson = Gson(),
        defaultValue = MacSettings(TabloConst.DEFAULT_MAC, emptyList()),
        commitPrefs = true
    )

    val vs: ObservableField<TabloViewState> = PreferenceUtils.createPrefsObjectObservable(
        prefs = prefs,
        key = "vs",
        gson = Gson(),
        defaultValue = TabloViewState.DEFAULT
    )

    private val stateHolder = object : StateHolderImpl<TabloViewState>(vs.field) {
        override fun render(vs: TabloViewState) {
            super.render(vs)

//            renderIfChangedEqual({ team1 }) { tabloInterface.setTeam1Name(it).makeSend() }
//            renderIfChangedEqual({ team2 }) { tabloInterface.setTeam2Name(it).makeSend() }

            if (isFirstRendering()) {
                tabloInterface.setTeam1Name(vs.team1).makeSend(TabloConst.Command.CMD_MES1)
                tabloInterface.setTeam2Name(vs.team2).makeSend(TabloConst.Command.CMD_MES2)
            }

            renderIfChangedEqual({ minutes to seconds }) { (minutes, seconds) ->
                tabloInterface.setTime(minutes, seconds).makeSend(TabloConst.Command.CMD_TIME)
            }

            renderIfChangedEqual({ points1 to points2 }) { (points1, points2) ->
                tabloInterface.setScore(points1, points2).makeSend(TabloConst.Command.CMD_SCORE)
            }

            renderIfChangedEqual({ period }) { tabloInterface.setPeriod(it).makeSend(TabloConst.Command.CMD_PEROID) }

            renderIfChangedEqual({ fouls1 to fouls2 }) { (fouls1, fouls2) ->
                tabloInterface.setFouls(fouls1, fouls2).makeSend(TabloConst.Command.CMD_FOUL)
            }

            renderIfChangedEqual({ timeouts1 to timeouts2 }) { (timeouts1, timeouts2) ->
                tabloInterface.setTimeouts(timeouts1, timeouts2).makeSend(TabloConst.Command.CMD_TIMEOUT)
            }

            renderIfChangedEqual({ holdIsTeam2 }) {
                tabloInterface.setHolding(it).makeSend(TabloConst.Command.CMD_HANDLING)
            }

            renderIfChangedEqual({ attackSeconds }) {
                tabloInterface.setTimeAttack(it, it == 0).makeSend(TabloConst.Command.CMD_24SEC)
            }
        }
    }

    private val _isStarted = BehaviorSubject.createDefault(false)
    private val _isAttackStarted = BehaviorSubject.createDefault(false)

    val isStarted: Observable<Boolean> = _isStarted.hide()
    val isAttackStarted: Observable<Boolean> = _isAttackStarted.hide()

    private val messagesToSend = PublishSubject.create<Pair<Completable, TabloConst.Command>>()

    private val _sendingState = BehaviorSubject.createDefault<SendingState>(SendingState.NotSent)

    val sendingState: Observable<SendingState> = _sendingState.hide()

    init {
        makeInit()
    }

    @SuppressLint("CheckResult")
    private fun makeInit() {

        Observable
            .interval(0, 5, TimeUnit.SECONDS)
            .subscribe {
                tabloInterface
                    .setHolding(vs.field.holdIsTeam2)
                    .makeSend(TabloConst.Command.CMD_HANDLING)
            }

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
                // i == 0 -- when initial (0 seconds passed).
                val vs = if (i > 0) {
                    vs.updateField { it.decAttackSecond() }
                } else {
                    vs.field
                }
                if (vs.attackSeconds == 0) {
                    setAttackStarted(false)
                    if (i > 0) {
                        tabloInterface.setSignal1(true).makeSend(TabloConst.Command.CMD_SIREN1)
                    }
                }
            }

        vs.changes
            .skip(1)
            .subscribe { stateHolder.render(it) }

        val resetSendingMessages = BehaviorSubject.createDefault(Unit)

        resetSendingMessages
            .switchMap {
                messagesToSend
                    .concatMap { (completable, command) ->
                        completable
                            .toSingleDefault<SendingState>(SendingState.Success)
                            .onErrorReturn { SendingState.Error(it) }
                            .toObservable()
                            .startWith(SendingState.Sending)
                            .takeUntil(messagesToSend.filter { it.second == command })
                    }
                    .takeUntil { it is SendingState.Error }
                    .doFinally { resetSendingMessages.onNext(Unit) }
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
        tabloInterface.setTeam1Name(tabloViewState.team1).makeSend(TabloConst.Command.CMD_MES1)
        tabloInterface.setTeam2Name(tabloViewState.team2).makeSend(TabloConst.Command.CMD_MES2)
    }

    fun setStarted(started: Boolean) {
        _isStarted.onNext(started)
    }

    fun setAttackStarted(attackStarted: Boolean) {
        _isAttackStarted.onNext(attackStarted)
    }

    fun setSignal1(enable: Boolean) {
        tabloInterface.setSignal1(enable).makeSend(TabloConst.Command.CMD_SIREN1)
    }

    fun setSignal2(enable: Boolean) {
        tabloInterface.setSignal2(enable).makeSend(TabloConst.Command.CMD_SIREN2)
    }

    private fun Completable.makeSend(command: TabloConst.Command) {
        messagesToSend.onNext(this to command)
    }
}