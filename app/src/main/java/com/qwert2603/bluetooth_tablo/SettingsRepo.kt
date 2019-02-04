package com.qwert2603.bluetooth_tablo

import android.preference.PreferenceManager

class SettingsRepo {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(BluetoothTabloApplication.appContext)

    var team1 by PrefsString(prefs, "team1")
    var team2 by PrefsString(prefs, "team2")
    var points1 by PrefsInt(prefs, "points1")
    var points2 by PrefsInt(prefs, "points2")

    fun getMessage() = listOf(team1, team2, points1, points2)
        .map { it.toString() }
        .reduce { acc, s -> "$acc\n$s" }
}