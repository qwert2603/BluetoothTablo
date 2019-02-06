package com.qwert2603.btablo.model

class BluetoothDeniedException : Exception()

class TabloNotFoundException : Exception()

class BluetoothConnectionException(t: Throwable) : Exception(t)

class WrongChecksumException : Exception()