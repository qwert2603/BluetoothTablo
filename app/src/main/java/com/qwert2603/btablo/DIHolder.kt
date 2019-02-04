package com.qwert2603.btablo

object DIHolder {
    val settingsRepo by lazy { SettingsRepo() }
    val tabloRepo by lazy { TabloRepo() }
}