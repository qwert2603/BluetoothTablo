package com.qwert2603.btablo.model

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.qwert2603.andrlib.util.LogUtils
import com.qwert2603.btablo.utils.noThrow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.Executors

class MessagesSender(private val socket: BluetoothSocket) {

    data class Message(
        val uuid: String,
        val text: String,
        val onSend: (t: Throwable?) -> Unit
    )

    private val messagesLock = Any()

    private var messages = ArrayDeque<Message>()

    private val executorService = Executors.newSingleThreadExecutor()

    init {
        executorService.submit {
            while (true) {
                Thread.sleep(10)
                Thread.yield()
                Log.v("bluetooth_tablo", "MessagesSender executorService while (true)")
                val message: Message = synchronized(messagesLock) { messages.pollFirst() } ?: continue
                doSendMessage(message)
            }
        }
    }

    fun sendMessage(message: Message) {
        synchronized(messagesLock) {
            LogUtils.d("MessagesSender sendMessage $message")
            messages.add(message)
        }
    }

    fun cancelMessage(message: Message) {
        synchronized(messagesLock) {
            val removed = messages.remove(message)
            LogUtils.d("MessagesSender cancelMessage $removed $message")
        }
    }

    fun stop() {
        LogUtils.d("MessagesSender stop executorService.isShutdown=${executorService.isShutdown}")
        executorService.shutdownNow()
    }

    private fun doSendMessage(message: Message) {
        try {
            LogUtils.d("MessagesSender doSendMessage $message")
            LogUtils.d("MessagesSender doSendMessage socket.isConnected = ${socket.isConnected} $socket")
            if (!socket.isConnected) {
                socket.connect()
            }

            val expectedChecksum = message.text.hashCode().toString()

            LogUtils.d("MessagesSender doSendMessage write ${message.text}")
            socket.outputStream.write("${message.text}\n".toByteArray())
            socket.outputStream.flush()
            LogUtils.d("MessagesSender doSendMessage flush")

//                Thread.sleep(1000)

            val bufferedReader = BufferedReader(InputStreamReader(socket.inputStream))
            val actualChecksum = bufferedReader.readLine()
            LogUtils.d("MessagesSender doSendMessage readLine $actualChecksum")

            if (actualChecksum != expectedChecksum) {
                throw WrongChecksumException()
            }

            message.onSend(null)
        } catch (t: Throwable) {
            noThrow { socket.close() }
            message.onSend(
                when (t) {
                    is WrongChecksumException -> t
                    else -> BluetoothConnectionException(t)
                }
            )
            stop()
        }
    }
}