package com.qwert2603.btablo.model

import com.qwert2603.andrlib.util.LogUtils
import com.qwert2603.btablo.utils.convertToByte
import com.qwert2603.btablo.utils.convertToBytes
import com.qwert2603.btablo.utils.digitAt
import io.reactivex.Completable
import io.reactivex.Single

class TabloInterfaceImpl(private val bluetoothRepo: BluetoothRepo) : TabloInterface {

    override fun setTime(minutes: Int, seconds: Int): Completable =
        sendMessage(
            TabloConst.Address.ADDR0,
            TabloConst.Command.CMD_TIME,
            byteArrayOf(
                minutes.digitAt(1).convertToByte(),
                minutes.digitAt(0).convertToByte(),
                seconds.digitAt(1).convertToByte(),
                seconds.digitAt(0).convertToByte(),
                if (seconds % 2 == 0) TabloConst.TIME_DOTS_ON else TabloConst.TIME_DOTS_OFF
            )
        )

    override fun setTimeAttack(seconds: Int, signal: Boolean): Completable =
        sendMessage(
            TabloConst.Address.ADDR_24SEC,
            TabloConst.Command.CMD_24SEC,
            byteArrayOf(
                seconds.digitAt(1).convertToByte(),
                seconds.digitAt(0).convertToByte(),
                if (signal) TabloConst.SIGNAL_ON else TabloConst.SIGNAL_OFF
            )
        ).also { LogUtils.d("TabloInterfaceImpl setTimeAttack $seconds $signal") }

    override fun setScore(points1: Int, points2: Int): Completable =
        sendMessage(
            TabloConst.Address.ADDR_DIG,
            TabloConst.Command.CMD_SCORE,
            byteArrayOf(
                points1.digitAt(2).convertToByte(),
                points1.digitAt(1).convertToByte(),
                points1.digitAt(0).convertToByte(),
                points2.digitAt(2).convertToByte(),
                points2.digitAt(1).convertToByte(),
                points2.digitAt(0).convertToByte()
            )
        )

    override fun setPeriod(period: Int): Completable =
        sendMessage(TabloConst.Address.ADDR_DIG, TabloConst.Command.CMD_PEROID, byteArrayOf(period.convertToByte()))

    override fun setFouls(fouls1: Int, fouls2: Int): Completable =
        sendMessage(
            TabloConst.Address.ADDR_DIG,
            TabloConst.Command.CMD_FOUL,
            byteArrayOf(fouls1.convertToByte(), fouls2.convertToByte())
        )

    override fun setTimeouts(timeouts1: Int, timeouts2: Int): Completable =
        sendMessage(
            TabloConst.Address.ADDR_DIG,
            TabloConst.Command.CMD_TIMEOUT,
            byteArrayOf(timeouts1.toByte(), timeouts2.toByte())
        )

    override fun setHolding(isTeam2: Boolean): Completable =
        sendMessage(
            TabloConst.Address.ADDR_DIG,
            TabloConst.Command.CMD_HANDLING,
            byteArrayOf(if (isTeam2) TabloConst.HOLDING_TEAM2 else TabloConst.HOLDING_TEAM1)
        )

    override fun setSignal1(isOn: Boolean): Completable =
        sendMessage(TabloConst.Address.ADDR_DIG, TabloConst.Command.CMD_SIREN1, byteArrayOf(TabloConst.SIGNAL_ON))

    override fun setSignal2(isOn: Boolean): Completable =
        sendMessage(TabloConst.Address.ADDR_DIG, TabloConst.Command.CMD_SIREN2, byteArrayOf(TabloConst.SIGNAL_ON))

    override fun setTeam1Name(name: String): Completable =
        sendMessage(TabloConst.Address.ADDR_MATRIX, TabloConst.Command.CMD_MES1, name.padEnd(10).convertToBytes())

    override fun setTeam2Name(name: String): Completable =
        sendMessage(TabloConst.Address.ADDR_MATRIX, TabloConst.Command.CMD_MES2, name.padEnd(10).convertToBytes())

    private fun sendMessage(address: TabloConst.Address, command: TabloConst.Command, text: ByteArray): Completable =
        Single
            .fromCallable {
                if (TabloConst.TEST_MODE) {
                    "_${address}_${command}_${String(text)}_".toByteArray()
                } else {
                    byteArrayOf(TabloConst.START_BYTE, address.value, command.value)
                        .plus(text)
                        .plus(TabloConst.STOP_BYTE)
                }
            }
            .flatMapCompletable {
                if (TabloConst.TEST_MODE) {
                    Completable.fromAction { LogUtils.d("sendMessage ${String(it)}") }
                } else {
                    bluetoothRepo.sendData(command, it)
                }
            }
}