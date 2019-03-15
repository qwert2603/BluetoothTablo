package com.qwert2603.btablo.tablo

import androidx.annotation.StringRes
import com.qwert2603.btablo.R

enum class Game(
    @StringRes val nameRes: Int,
    val resetSeconds: Int,
    val timeForward: Boolean,
    val timeIsOverSeconds: Int,
    val signalOnTimeIsOver: Boolean = timeIsOverSeconds == 0
) {
    FOOTBALL(
        nameRes = R.string.game_football,
        resetSeconds = 10 * TabloViewState.SECONDS_PER_MINUTE,
        timeForward = false,
        timeIsOverSeconds = 0
    ),

    VOLLEYBALL(
        nameRes = R.string.game_volleyball,
        resetSeconds = 0,
        timeForward = true,
        timeIsOverSeconds = TabloViewState.MAX_MINUTES * TabloViewState.SECONDS_PER_MINUTE + TabloViewState.MAX_SECONDS
    ),

    BASKETBALL(
        nameRes = R.string.game_basketball,
        resetSeconds = 10 * TabloViewState.SECONDS_PER_MINUTE,
        timeForward = false,
        timeIsOverSeconds = 0
    );
}