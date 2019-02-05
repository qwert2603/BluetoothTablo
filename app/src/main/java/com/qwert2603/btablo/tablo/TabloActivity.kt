package com.qwert2603.btablo.tablo

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Html
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.qwert2603.andrlib.base.mvi.BaseActivity
import com.qwert2603.andrlib.base.mvi.ViewAction
import com.qwert2603.andrlib.util.setVisible
import com.qwert2603.btablo.BuildConfig
import com.qwert2603.btablo.R
import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.utils.LogUtils
import com.qwert2603.btablo.utils.doOnTextChange
import com.qwert2603.btablo.utils.toIntOrZero
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_tablo.*
import java.text.SimpleDateFormat
import java.util.*

class TabloActivity : BaseActivity<TabloViewState, TabloView, TabloPresenter>(), TabloView {

    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device.name
                val deviceMacAddress = device.address
                LogUtils.d("BT device found $deviceName $deviceMacAddress")
                if (deviceMacAddress == "3C:CB:7C:39:DA:95") {
                    bluetoothAdapter.cancelDiscovery()

                    val socket = device.createRfcommSocketToServiceRecord(UUID.randomUUID())
                    LogUtils.d("BT socket $socket")

//                    DIHolder.tabloRepo.onSocket(socket)
                }
            }
        }
    }

    override fun createPresenter() = TabloPresenter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tablo)

        /*todo
        * максимальная длина поля называния команды?
        * счёт -- только целые неотрицательные числа?
        * максимальный размер счёта?
        * формат отправки данных?
        * ответ табло, что данные приняты успешно / неуспешно (контрольная сумма)?
        * пинг табло раз в секунду?
        */

        val settingsRepo = DIHolder.settingsRepo

        team1_EditText.setText(settingsRepo.team1)
        team2_EditText.setText(settingsRepo.team2)
        points1_EditText.setText(settingsRepo.points1.toString())
        points2_EditText.setText(settingsRepo.points2.toString())

        team1_EditText.doOnTextChange { settingsRepo.team1 = it }
        team2_EditText.doOnTextChange { settingsRepo.team2 = it }
        points1_EditText.doOnTextChange { settingsRepo.points1 = it.toIntOrZero() }
        points2_EditText.doOnTextChange { settingsRepo.points2 = it.toIntOrZero() }

        @Suppress("DEPRECATION")
        about_TextView.text = Html.fromHtml(
            getString(
                R.string.about_text_format,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(BuildConfig.BIULD_TIME)),
                SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(BuildConfig.BIULD_TIME))
            )
        )

        // todo: receive and consume BT disabled events
        registerReceiver(btReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

        val refreshSocketAction = {
            if (bluetoothAdapter.isEnabled) {
                LogUtils.d("BT startDiscovery ${bluetoothAdapter.startDiscovery()}")
            } else {
                startActivityForResult(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQUEST_ENABLE_BT
                )
            }
        }
//        DIHolder.tabloRepo.refreshSocketAction = refreshSocketAction
        refreshSocketAction()
    }

    override fun onDestroy() {
//        DIHolder.tabloRepo.refreshSocketAction = null
        bluetoothAdapter.cancelDiscovery()
        unregisterReceiver(btReceiver)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                LogUtils.d("BT enabled")
                LogUtils.d("BT startDiscovery ${bluetoothAdapter.startDiscovery()}")
            } else {
                LogUtils.d("BT denied")
//                DIHolder.tabloRepo.onSocket(null)
            }
        }
    }

    override fun sendClicks(): Observable<Any> = RxView.clicks(send_Button)

    override fun anyFieldChanged(): Observable<Any> = Observable.merge(
        listOf(
            RxTextView.textChanges(team1_EditText),
            RxTextView.textChanges(team2_EditText),
            RxTextView.textChanges(points1_EditText),
            RxTextView.textChanges(points2_EditText)
        )
            .map { it.skipInitialValue() }
    )

    override fun render(vs: TabloViewState) {
        super.render(vs)

        sendingState_LinearLayout.setVisible(vs.sendingState != null)

        if (vs.sendingState != null) {
            sending_ProgressBar.setVisible(vs.sendingState == SendingState.SENDING)
            sendingState_TextView.text = getString(
                when (vs.sendingState) {
                    SendingState.SENDING -> R.string.sending_state_sending
                    SendingState.ERROR -> R.string.sending_state_error
                    SendingState.SUCCESS -> R.string.sending_state_success
                }
            )
            @Suppress("DEPRECATION")
            sendingState_TextView.setTextColor(
                resources.getColor(
                    when (vs.sendingState) {
                        SendingState.SENDING -> R.color.sending_state_sending
                        SendingState.ERROR -> R.color.sending_state_error
                        SendingState.SUCCESS -> R.color.sending_state_success
                    }
                )
            )
        }

        hasUnsentChanges_TextView.setVisible(vs.hasUnsentChanges)
    }

    override fun executeAction(va: ViewAction) {
        // nth.
    }

}