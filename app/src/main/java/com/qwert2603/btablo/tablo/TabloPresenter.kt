package com.qwert2603.btablo.tablo

import com.qwert2603.andrlib.base.mvi.BasePresenter
import com.qwert2603.andrlib.base.mvi.PartialChange
import com.qwert2603.andrlib.util.LogUtils
import com.qwert2603.btablo.di.DIHolder
import io.reactivex.Observable

class TabloPresenter : BasePresenter<TabloView, TabloViewState>(DIHolder.uiSchedulerProvider) {

    override val initialState = TabloViewState(
        sendingState = null,
        hasUnsentChanges = false,
        changedAfterSendingStarted = false
    )

    override val partialChanges: Observable<PartialChange> = Observable.merge(
        intent { it.anyFieldChanged() }
            .map { TabloPartialChange.AnyFieldChanged },
        intent { it.sendClicks() }
            .switchMap {
                DIHolder.tabloRepo.sendData(DIHolder.settingsRepo.getMessage())
                    .toSingleDefault<TabloPartialChange>(TabloPartialChange.SendSuccess)
                    .onErrorReturn {
                        LogUtils.e(msg = "TabloPresenter sendData", t = it)
                        TabloPartialChange.SendError(it)
                    }
                    .toObservable()
                    .startWith(TabloPartialChange.SendStarted)
            }
    )

    override fun stateReducer(vs: TabloViewState, change: PartialChange): TabloViewState {
        if (change !is TabloPartialChange) null!!
        return when (change) {
            TabloPartialChange.SendStarted -> vs.copy(
                sendingState = SendingState.Sending,
                changedAfterSendingStarted = false
            )
            is TabloPartialChange.SendError -> vs.copy(sendingState = SendingState.Error(change.t))
            TabloPartialChange.SendSuccess -> vs.copy(
                sendingState = SendingState.Success,
                hasUnsentChanges = vs.changedAfterSendingStarted
            )
            TabloPartialChange.AnyFieldChanged -> vs.copy(hasUnsentChanges = true, changedAfterSendingStarted = true)
        }
    }
}