package com.qwert2603.btablo.model

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.qwert2603.andrlib.util.LogUtils
import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.utils.CachingSingle
import com.qwert2603.btablo.utils.Wrapper
import com.qwert2603.btablo.utils.cacheIfSuccess
import com.qwert2603.btablo.utils.wrap
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class AnthTabloRepo {

    private data class ValueToWait<T>(
        val version: Long,
        val value: T
    ) {
        fun createNext(t: T) = copy(version = version + 1, value = t)

        companion object {
            fun makeUpdate(action: () -> Unit) {
                synchronized(ValueToWait::class.java) {
                    action()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 2
        private const val ALCATEL_MAC = "3C:CB:7C:39:DA:95"
        private const val REDMI_MAC = "E0:62:67:66:E7:D6"
        val BT_UUID: UUID = UUID.fromString("a3768bc3-601a-4f7b-ab72-798c5c2e44a8")

        private fun noThrow(action: () -> Unit) {
            try {
                action()
            } catch (t: Throwable) {
            }
        }
    }

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val currentActivity = BehaviorSubject.createDefault<Wrapper<AppCompatActivity>>(Wrapper(null))

    @Volatile
    private var isBtEnabled = ValueToWait(1, false)

    @Volatile
    private var currentSocket = ValueToWait(1, Wrapper<BluetoothSocket>(null))

    private val sendScheduler = Schedulers.from(Executors.newFixedThreadPool(8))

    private val btFoundReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothDevice.ACTION_FOUND) return

            val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            val deviceName = device.name
            val deviceMacAddress = device.address
            LogUtils.d("BT device found $deviceName $deviceMacAddress")
            if (deviceMacAddress in listOf(ALCATEL_MAC, REDMI_MAC)) {
                LogUtils.d("TabloRepo btFoundReceiver bluetoothAdapter.cancelDiscovery() ${bluetoothAdapter.cancelDiscovery()}")

                val socket = device.createInsecureRfcommSocketToServiceRecord(BT_UUID)
                ValueToWait.makeUpdate { currentSocket = currentSocket.createNext(socket.wrap()) }
            }
        }
    }

    private val skt: CachingSingle<MessagesSender> = requestGeoPermission()
        .flatMap { enableBt() }
        .flatMap {
            getCurrentSocket()
                .timeout(15, TimeUnit.SECONDS, sendScheduler, Single.error(TabloNotFoundException()))
        }
        .map { MessagesSender(it) }
        .cacheIfSuccess("skt")

    init {
        DIHolder.appContext.registerReceiver(btFoundReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    fun sendData(message: String): Completable = skt.get()
        .flatMapCompletable { messagesSender ->
            Completable
                .create { emitter ->
                    val uuid = UUID.randomUUID().toString()
                    val msg = MessagesSender.Message(uuid, message) { t: Throwable? ->
                        if (!emitter.isDisposed) {
                            if (t == null) {
                                emitter.onComplete()
                            } else {
                                emitter.onError(t)
                            }
                        }
                    }
                    emitter.setCancellable {
                        LogUtils.d("TabloRepo sendData cancellable called $msg")
                        messagesSender.cancelMessage(msg)
                    }
                    messagesSender.sendMessage(msg)
                }
                .timeout(
                    10,
                    TimeUnit.SECONDS,
                    sendScheduler,
                    Completable.error(BluetoothConnectionException(TimeoutException()))
                )
                .doOnError {
                    skt.reset()
                    messagesSender.stop()
                }
        }

    private fun requestGeoPermission(): Single<Unit> = DIHolder.permesso.permissionRequester
        .requestPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        .map { Unit }
        .doOnSubscribe { LogUtils.d("TabloRepo requestGeoPermission doOnSubscribe") }
        .subscribeOn(sendScheduler)

    private fun enableBt(): Single<Unit> = Completable
        .fromAction {
            LogUtils.d("TabloRepo enableBt bluetoothAdapter.isEnabled ${bluetoothAdapter.isEnabled}")
            if (bluetoothAdapter.isEnabled) return@fromAction

            val activity = currentActivity
                .filter { it.t != null }
                .map { it.t!! }
                .firstOrError()
                .blockingGet()

            LogUtils.d("TabloRepo enableBt activity $activity")

            val prevVersion = isBtEnabled.version

            activity.startActivityForResult(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                REQUEST_ENABLE_BT
            )

            while (prevVersion == isBtEnabled.version) Thread.yield()

            if (!isBtEnabled.value) throw BluetoothDeniedException()
        }
        .toSingleDefault(Unit)
        .doOnSubscribe { LogUtils.d("TabloRepo enableBt doOnSubscribe") }
        .subscribeOn(sendScheduler)

    private fun getCurrentSocket(): Single<BluetoothSocket> = Single
        .create { emitter: SingleEmitter<BluetoothSocket> ->

            ValueToWait.makeUpdate { currentSocket = currentSocket.createNext(Wrapper(null)) }

            emitter.setCancellable {
                LogUtils.d("TabloRepo getCurrentSocket setCancellable called bluetoothAdapter.isDiscovering ${bluetoothAdapter.isDiscovering}")
                if (bluetoothAdapter.isDiscovering) {
                    LogUtils.d("TabloRepo getCurrentSocket setCancellable bluetoothAdapter.cancelDiscovery() ${bluetoothAdapter.cancelDiscovery()}")
                }
            }

            val prevVersion = currentSocket.version

            LogUtils.d("TabloRepo getCurrentSocket bluetoothAdapter.isDiscovering ${bluetoothAdapter.isDiscovering}")
            if (!bluetoothAdapter.isDiscovering) {
                LogUtils.d("TabloRepo getCurrentSocket bluetoothAdapter.startDiscovery() ${bluetoothAdapter.startDiscovery()}")
            }

            while (prevVersion == currentSocket.version) Thread.yield()

            val socket = currentSocket.value.t

            LogUtils.d("TabloRepo getCurrentSocket socket $socket")

            if (!emitter.isDisposed) {
                if (socket != null) {
                    emitter.onSuccess(socket)
                } else {
                    emitter.onError(TabloNotFoundException())
                }
            }
        }
        .subscribeOn(sendScheduler)

    private class MessagesSender(private val socket: BluetoothSocket) {

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
                    Thread.sleep(50)
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

                Thread.sleep(1000)//todo:remove

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

    fun onActivityCreate(activity: AppCompatActivity) {
        @Suppress("UNUSED")
        activity.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun onCreate() {
                currentActivity.onNext(activity.wrap())
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                currentActivity.onNext(Wrapper(null))
            }
        })
    }

    @Suppress("UNUSED_PARAMETER")
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT) {
            val enabled = resultCode == Activity.RESULT_OK
            ValueToWait.makeUpdate { isBtEnabled = isBtEnabled.createNext(enabled) }
        }
    }
}