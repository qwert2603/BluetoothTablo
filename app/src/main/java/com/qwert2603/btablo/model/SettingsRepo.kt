package com.qwert2603.btablo.model

import android.preference.PreferenceManager
import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.utils.PrefsInt
import com.qwert2603.btablo.utils.PrefsString

class SettingsRepo {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(DIHolder.appContext)

    var team1 by PrefsString(prefs, "team1", "Хозяева")
    var team2 by PrefsString(prefs, "team2", "Гости")
    var points1 by PrefsInt(prefs, "points1")
    var points2 by PrefsInt(prefs, "points2")

    fun getMessage() = listOf(team1, team2, points1, points2)
        .map { it.toString() }
        .reduce { acc, s -> "$acc $s" }
}