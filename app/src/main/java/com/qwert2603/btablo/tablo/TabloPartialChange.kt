package com.qwert2603.btablo.tablo

import com.qwert2603.andrlib.base.mvi.PartialChange

sealed class TabloPartialChange : PartialChange {
    object SendStarted : TabloPartialChange()
    object SendError : TabloPartialChange()
    object SendSuccess : TabloPartialChange()
    object AnyFieldChanged : TabloPartialChange()
}