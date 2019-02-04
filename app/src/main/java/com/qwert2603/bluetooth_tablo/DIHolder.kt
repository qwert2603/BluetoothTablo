package com.qwert2603.bluetooth_tablo

object DIHolder {
    val settingsRepo by lazy { SettingsRepo() }
    val tabloRepo by lazy { TabloRepo() }
}