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
import com.qwert2603.btablo.utils.CachingSingle
import com.qwert2603.btablo.utils.Wrapper
import com.qwert2603.btablo.utils.cacheIfSuccess
import com.qwert2603.btablo.utils.wrap
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class BluetoothRepoImpl : BluetoothRepo {

    private class ValueToWait<T>(
        @Volatile private var value: T
    ) {
        @Volatile
        private var version: Long = 1

        fun makeUpdate(nextValue: T) {
            synchronized(this) {
                value = nextValue
                ++version
            }
        }

        fun waitNext(): T {
            val prevVersion = version

            while (prevVersion == version) Thread.yield()

            return value
        }
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 2
        private const val ALCATEL_MAC = "3C:CB:7C:39:DA:95"
        private const val REDMI_MAC = "E0:62:67:66:E7:D6"
        val BT_UUID: UUID = UUID.fromString("a3768bc3-601a-4f7b-ab72-798c5c2e44a8")
    }

    override val activityCallbacks = object : BluetoothRepo.ActivityCallbacks {
        override fun onActivityCreate(activity: AppCompatActivity) {
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
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == REQUEST_ENABLE_BT) {
                val enabled = resultCode == Activity.RESULT_OK
                isBtEnabled.makeUpdate(enabled)
            }
        }
    }

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val currentActivity = BehaviorSubject.createDefault<Wrapper<AppCompatActivity>>(Wrapper(null))

    private val isBtEnabled = ValueToWait(false)

    private val currentSocket = ValueToWait(Wrapper<BluetoothSocket>(null))

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
                currentSocket.makeUpdate(socket.wrap())
            }
        }
    }

    private val cachingMessagesSender: CachingSingle<MessagesSender> = requestGeoPermission()
        .flatMap { enableBt() }
        .flatMap {
            getCurrentSocket()
                .timeout(15, TimeUnit.SECONDS, sendScheduler, Single.error(TabloNotFoundException()))
        }
        .map { MessagesSender(it) }
        .cacheIfSuccess("cachingMessagesSender")

    init {
        DIHolder.appContext.registerReceiver(btFoundReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    override fun sendData(text: ByteArray): Completable = Single
        .defer { cachingMessagesSender.get() }
        .flatMapCompletable { messagesSender ->
            Completable
                .create { emitter ->
                    val uuid = UUID.randomUUID().toString()
                    val msg = MessagesSender.Message(uuid, String(text)) { t: Throwable? ->
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
                    LogUtils.d("TabloRepo sendData doOnError $it")
                    messagesSender.stop()
                }
        }
        .retry { i, t ->
            LogUtils.d("TabloRepo sendData retry $i $t")
            cachingMessagesSender.reset()
            return@retry i == 1 && t is BluetoothConnectionException
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

            activity.startActivityForResult(
                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                REQUEST_ENABLE_BT
            )

            if (!isBtEnabled.waitNext()) throw BluetoothDeniedException()
        }
        .toSingleDefault(Unit)
        .doOnSubscribe { LogUtils.d("TabloRepo enableBt doOnSubscribe") }
        .subscribeOn(sendScheduler)

    private fun getCurrentSocket(): Single<BluetoothSocket> = Single
        .create { emitter: SingleEmitter<BluetoothSocket> ->

            currentSocket.makeUpdate(Wrapper(null))

            emitter.setCancellable {
                LogUtils.d("TabloRepo getCurrentSocket setCancellable called bluetoothAdapter.isDiscovering ${bluetoothAdapter.isDiscovering}")
                if (bluetoothAdapter.isDiscovering) {
                    LogUtils.d("TabloRepo getCurrentSocket setCancellable bluetoothAdapter.cancelDiscovery() ${bluetoothAdapter.cancelDiscovery()}")
                }
            }

            LogUtils.d("TabloRepo getCurrentSocket bluetoothAdapter.isDiscovering ${bluetoothAdapter.isDiscovering}")
            if (!bluetoothAdapter.isDiscovering) {
                LogUtils.d("TabloRepo getCurrentSocket bluetoothAdapter.startDiscovery() ${bluetoothAdapter.startDiscovery()}")
            }

            val socket = currentSocket.waitNext().t

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
}