package com.qwert2603.btablo

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*todo
        * максимальная длина поля называния команды?
        * счёт -- только целые неотрицательные числа?
        * максимальный размер счёта?
        * формат отправки данных?
        * ответ табло, что данные приняты успешно / неуспешно?
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

        val viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        send_Button.setOnClickListener {
            viewModel.send(settingsRepo.getMessage())
        }

        sendingState_LinearLayout.setVisible(false)

        viewModel.sendingState.observe(this, Observer { state: SendingState ->
            LogUtils.d("viewModel.sendingState.observe $state")

            sendingState_LinearLayout.setVisible(true)

            sending_ProgressBar.setVisible(state == SendingState.SENDING)
            sendingState_TextView.text = getString(
                when (state) {
                    SendingState.SENDING -> R.string.sending_state_sending
                    SendingState.ERROR -> R.string.sending_state_error
                    SendingState.SUCCESS -> R.string.sending_state_success
                }
            )
            @Suppress("DEPRECATION")
            sendingState_TextView.setTextColor(
                resources.getColor(
                    when (state) {
                        SendingState.SENDING -> R.color.sending_state_sending
                        SendingState.ERROR -> R.color.sending_state_error
                        SendingState.SUCCESS -> R.color.sending_state_success
                    }
                )
            )
        })

        if (bluetoothAdapter.isEnabled) {
            LogUtils.d("BT startDiscovery ${bluetoothAdapter.startDiscovery()}")
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        registerReceiver(btReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    override fun onDestroy() {
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
            }
        }
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
                    socket.close()
                }
            }
        }
    }

}