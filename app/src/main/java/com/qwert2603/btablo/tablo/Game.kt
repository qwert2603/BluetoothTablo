package com.qwert2603.btablo.tablo

import androidx.annotation.StringRes
import com.qwert2603.btablo.R

enum class Game(
    @StringRes val nameRes: Int,
    val resetSeconds: Int,
    val timeForward: Boolean,
    val timeIsOverSeconds: Int = if (timeForward) TabloViewState.MAX_TOTAL_SECONDS else 0,
    val signalOnTimeIsOver: Boolean = timeIsOverSeconds == 0
) {
    FOOTBALL(
        nameRes = R.string.game_football,
        resetSeconds = 10 * TabloViewState.SECONDS_PER_MINUTE,
        timeForward = false
    ),

    VOLLEYBALL(
        nameRes = R.string.game_volleyball,
        resetSeconds = 0,
        timeForward = true
    ),

    BASKETBALL(
        nameRes = R.string.game_basketball,
        resetSeconds = 10 * TabloViewState.SECONDS_PER_MINUTE,
        timeForward = false
    );

    val stepsEndTotalSeconds = if (timeForward) TabloViewState.MAX_TOTAL_SECONDS else 0
}