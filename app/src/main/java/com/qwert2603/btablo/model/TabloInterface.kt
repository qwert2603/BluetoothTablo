package com.qwert2603.btablo.model

import androidx.annotation.IntRange
import androidx.annotation.Size
import io.reactivex.Completable

interface TabloInterface {

    fun setTime(
        @IntRange(from = 0, to = 99) minutes: Int,
        @IntRange(from = 0, to = 59) seconds: Int
    ): Completable

    fun setTimeAttack(
        @IntRange(from = 0, to = 99) seconds: Int,
        signal: Boolean
    ): Completable

    fun setScore(
        @IntRange(from = 0, to = 999) points1: Int,
        @IntRange(from = 0, to = 999) points2: Int
    ): Completable

    fun setPeriod(@IntRange(from = 0, to = 9) period: Int): Completable

    fun setFouls(
        @IntRange(from = 0, to = 9) fouls1: Int,
        @IntRange(from = 0, to = 9) fouls2: Int
    ): Completable

    fun setTimeouts(
        @IntRange(from = 0, to = 3) timeouts1: Int,
        @IntRange(from = 0, to = 3) timeouts2: Int
    ): Completable

    fun setHolding(isTeam2: Boolean): Completable

    fun setSignal1(isOn: Boolean): Completable

    fun setSignal2(isOn: Boolean): Completable

    fun setTeam1Name(@Size(max = 10) name: String): Completable

    fun setTeam2Name(@Size(max = 10) name: String): Completable
}