package com.qwert2603.btablo.tablo

data class TabloViewState(
    val sendingState: SendingState?,
    val hasUnsentChanges: Boolean,
    val changedAfterSendingStarted: Boolean
)