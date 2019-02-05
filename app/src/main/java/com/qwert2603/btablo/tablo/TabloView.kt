package com.qwert2603.btablo.tablo

import com.qwert2603.andrlib.base.mvi.BaseView
import io.reactivex.Observable

interface TabloView : BaseView<TabloViewState> {
    fun sendClicks(): Observable<Any>
    fun anyFieldChanged(): Observable<Any>
}