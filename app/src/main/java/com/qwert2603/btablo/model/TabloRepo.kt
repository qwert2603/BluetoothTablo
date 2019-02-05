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
import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.utils.LogUtils
import com.qwert2603.btablo.utils.Wrapper
import com.qwert2603.btablo.utils.wrap
import io.reactivex.Completable
import io.reactivex.CompletableEmitter
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import java.util.*

class TabloRepo {

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val ALCATEL_MAC = "3C:CB:7C:39:DA:95"
    }

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val btStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return

            if (intent.hasExtra(BluetoothAdapter.EXTRA_STATE)) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)
                LogUtils.d("BT state = $state")
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
            if (deviceMacAddress == ALCATEL_MAC) {
                bluetoothAdapter.cancelDiscovery()

                val socket = device.createRfcommSocketToServiceRecord(UUID.randomUUID())
                LogUtils.d("BT socket $socket")
                currentSocket.onNext(socket.wrap())
            }
        }
    }

    private val isBtEnabled = BehaviorSubject.createDefault(false)

    private val currentActivity = BehaviorSubject.create<Wrapper<AppCompatActivity>>()

    private val emittersLock = Any()
    private val enableBtEmitters = mutableSetOf<CompletableEmitter>()

    private val currentSocket = BehaviorSubject.create<Wrapper<BluetoothSocket>>()

    init {
        DIHolder.appContext.registerReceiver(btStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        DIHolder.appContext.registerReceiver(btFoundReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    fun sendData(message: String): Completable = enableBt()
        .andThen(Completable.fromAction {
            Thread.sleep(2000)
//            if (Random.nextBoolean()) throw Exception("stub!")
        })
        .subscribeOn(DIHolder.modelSchedulersProvider.io)
        .also { LogUtils.d("TabloRepo sendData $message") }

    private fun getSocket(): Single<BluetoothSocket> = enableBt()
        .andThen(Single.create { })

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
                            synchronized(emittersLock) {
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

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT) {
            val enabled = resultCode == Activity.RESULT_OK
            isBtEnabled.onNext(enabled)
            synchronized(emittersLock) {
                enableBtEmitters.forEach {
                    if (enabled) {
                        it.onComplete()
                    } else {
                        it.onError(BluetoothDeniedException())
                    }
                }
                enableBtEmitters.clear()
            }
            bluetoothAdapter.startDiscovery()
        }
    }
}