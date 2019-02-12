package com.qwert2603.btablo.model

import com.qwert2603.btablo.utils.convertToByte

object TabloConst {

    const val TEST_MODE = true

    enum class Address(val value: Byte) {
        ADDR0(0.convertToByte()), //общий адрес
        ADDR_24SEC(1.convertToByte()),  //адрес табло 24 сек (у обоих один адрес)
        ADDR_DIG(2.convertToByte()), //адрес большого цифрового табло
        ADDR_MATRIX(3.convertToByte());   //адрес названия команд
    }

    enum class Command(val value: Byte) {
        CMD_TIME(0.convertToByte()), //Время + точки = 5 Б
        CMD_24SEC(1.convertToByte()),//24 секунды + круг (дюралайт) = 3 Б
        CMD_SCORE(2.convertToByte()), //Счет = 6 Б
        CMD_PEROID(3.convertToByte()),   //Период = 1 Б
        CMD_FOUL(4.convertToByte()), //Фолы = 2 Б
        CMD_TIMEOUT(5.convertToByte()),  //Тайм-аут = 2 Б
        CMD_HANDLING(6.convertToByte()),   //Стрелка владения = 1 Б
        CMD_SIREN1(7.convertToByte()),//Сирена 1 = 1 Б
        CMD_SIREN2(8.convertToByte()),//Сирена 2 = 1 Б
        CMD_MES1(9.convertToByte()),//Название команды 1 = 10 Б
        CMD_MES2(10.convertToByte());//Название команды 2 = 10 Б
    }

    val START_BYTE = 0x3A.convertToByte()
    val STOP_BYTE = 0x0d.convertToByte()

    val SIGNAL_ON = 0xFF.convertToByte()
    val SIGNAL_OFF = 0x00.convertToByte()
    val TIME_DOTS_ON = 0xFF.convertToByte()
    val TIME_DOTS_OFF = 0x00.convertToByte()
    val HOLDING_TEAM1 = 0x00.convertToByte()
    val HOLDING_TEAM2 = 0x01.convertToByte()
}