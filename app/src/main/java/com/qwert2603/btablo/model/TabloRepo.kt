package com.qwert2603.btablo.model

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.qwert2603.andrlib.util.LogUtils
import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.utils.Wrapper
import com.qwert2603.btablo.utils.wrap
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
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

class TabloRepo {

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val ALCATEL_MAC = "3C:CB:7C:39:DA:95"
        private const val REDMI_MAC = "E0:62:67:66:E7:D6"
        val BT_UUID: UUID = UUID.fromString("a3768bc3-601a-4f7b-ab72-798c5c2e44a8")
    }

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val btStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return

            if (intent.hasExtra(BluetoothAdapter.EXTRA_STATE)) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                LogUtils.d("BT btStateReceiver state = $state")
                isBtEnabled.onNext(state == BluetoothAdapter.STATE_ON)
            }
        }
    }

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
                LogUtils.d("BT btFoundReceiver socket $socket")
                currentSocket.onNext(socket.wrap())
                synchronized(socketEmittersLock) {
                    socketEmitters.forEach { if (!it.isDisposed) it.onSuccess(socket) }
                    socketEmitters.clear()
                }
            }
        }
    }

    private val isBtEnabled = BehaviorSubject.createDefault(false)

    private val currentActivity = BehaviorSubject.createDefault<Wrapper<AppCompatActivity>>(Wrapper(null))

    private val enableBtEmittersLock = Any()
    private val enableBtEmitters = mutableSetOf<CompletableEmitter>()

    private val socketEmittersLock = Any()
    private val socketEmitters = mutableSetOf<SingleEmitter<BluetoothSocket>>()

    private val socketLock = Any()
    private val currentSocket = BehaviorSubject.createDefault<Wrapper<BluetoothSocket>>(Wrapper(null))

    private val sendScheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    init {
        DIHolder.appContext.registerReceiver(btStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        DIHolder.appContext.registerReceiver(btFoundReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    fun sendData(message: String): Completable = getConnectedSocket()
        .flatMapCompletable { socket ->
            Completable
                .fromAction {
                    try {
                        checkNotUi()
                        LogUtils.d("TabloRepo sendData socket $socket")

                        val expectedChecksum = message.hashCode().toString()

                        LogUtils.d("TabloRepo sendData write $message")
                        socket.outputStream.write("$message\n".toByteArray())
                        socket.outputStream.flush()

                        val bufferedReader = BufferedReader(InputStreamReader(socket.inputStream))
                        val actualChecksum = bufferedReader.readLine()
                        LogUtils.d("TabloRepo sendData readLine $actualChecksum")

                        if (actualChecksum != expectedChecksum) {
                            throw WrongChecksumException()
                        }
                    } catch (t: Throwable) {
                        if (t is WrongChecksumException) {
                            throw t
                        } else {
                            noThrow { socket.close() }
                            currentSocket.onNext(Wrapper(null))
                            throw BluetoothConnectionException(t)
                        }
                    }
                }
                .subscribeOn(sendScheduler)
                .timeout(
                    10,
                    TimeUnit.SECONDS,
                    Completable.error {
                        LogUtils.d("TabloRepo sendData timeout")
                        checkNotUi()
                        noThrow { socket.close() }
                        currentSocket.onNext(Wrapper(null))
                        BluetoothConnectionException(TimeoutException())
                    }
                )
        }
        .subscribeOn(DIHolder.modelSchedulersProvider.io)
        .also { LogUtils.d("TabloRepo sendData $message") }

    private fun getConnectedSocket(): Single<BluetoothSocket> = currentSocket
        .firstOrError()
        .flatMap {
            LogUtils.d("TabloRepo getSocket currentSocket $it")
            if (it.t != null) {
                Single.just(it.t)
            } else {
                createSocket()
            }
        }
        .observeOn(DIHolder.modelSchedulersProvider.io)
        .flatMap {
            LogUtils.d("TabloRepo getSocket it.isConnected=${it.isConnected}")
            synchronized(socketLock) {
                if (!it.isConnected) {
                    try {
                        checkNotUi()
                        it.connect()
                        LogUtils.d("TabloRepo getSocket connect success")
                    } catch (t: Throwable) {
                        LogUtils.d("TabloRepo getSocket connect error")
                        noThrow { it.close() }
                        currentSocket.onNext(Wrapper(null))
                        return@flatMap Single.error<BluetoothSocket>(BluetoothConnectionException(t))
                    }
                }
            }
            Single.just(it)
        }

    private fun createSocket(): Single<BluetoothSocket> = enableBt()
        .andThen(
            Single
                .create { emitter: SingleEmitter<BluetoothSocket> ->
                    synchronized(socketEmittersLock) {
                        socketEmitters.add(emitter)
                    }
                    if (!bluetoothAdapter.isDiscovering) {
                        val startDiscovery = bluetoothAdapter.startDiscovery()
                        LogUtils.d("TabloRepo bluetoothAdapter.startDiscovery() $startDiscovery")
                        if (!startDiscovery) {
                            synchronized(socketEmittersLock) {
                                socketEmitters.forEach { if (!it.isDisposed) it.onError(BluetoothDeniedException()) }
                                socketEmitters.clear()
                            }
                        }
                    }
                }
                .subscribeOn(DIHolder.modelSchedulersProvider.io)
        )
        .timeout(
            25,
            TimeUnit.SECONDS,
            Single.error {
                LogUtils.d("TabloRepo createSocket timeout")
                LogUtils.d("TabloRepo createSocket bluetoothAdapter.cancelDiscovery() ${bluetoothAdapter.cancelDiscovery()}")
                currentSocket.onNext(Wrapper(null))
                synchronized(socketEmittersLock) {
                    socketEmitters.forEach { if (!it.isDisposed) it.onError(TabloNotFoundException()) }
                    socketEmitters.clear()
                }
                TabloNotFoundException()
            }
        )
        .subscribeOn(DIHolder.modelSchedulersProvider.io)

    private fun enableBt(): Completable = DIHolder.permesso.permissionRequester
        .requestPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        .flatMap { isBtEnabled.firstOrError() }
        .flatMapCompletable { enabled ->
            LogUtils.d("TabloRepo bt enabled = $enabled")
            if (enabled) {
                Completable.complete()
            } else {
                currentActivity
                    .doOnNext { LogUtils.d("TabloRepo currentActivity = $it") }
                    .filter { it.t != null }
                    .map { it.t!! }
                    .firstOrError()
                    .flatMapCompletable { activity ->
                        Completable.create { emitter ->
                            synchronized(enableBtEmittersLock) {
                                enableBtEmitters.add(emitter)
                            }
                            LogUtils.d("TabloRepo startActivityForResult")
                            activity.startActivityForResult(
                                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                                REQUEST_ENABLE_BT
                            )
                        }
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
            isBtEnabled.onNext(enabled)
            synchronized(enableBtEmittersLock) {
                enableBtEmitters.forEach {
                    if (!it.isDisposed) {
                        if (enabled) {
                            it.onComplete()
                        } else {
                            it.onError(BluetoothDeniedException())
                        }
                    }
                }
                enableBtEmitters.clear()
            }
        }
    }

    private fun noThrow(action: () -> Unit) {
        try {
            action()
        } catch (t: Throwable) {
        }
    }

    private fun checkNotUi() {
        if (DIHolder.uiSchedulerProvider.isOnUi()) {
            LogUtils.e("UI thread !!!")
            LogUtils.printCurrentStack()
        }
    }
}