package com.qwert2603.btablo.utils

class Wrapper<T>(val t: T?)

fun <T> T?.wrap() = Wrapper(this)