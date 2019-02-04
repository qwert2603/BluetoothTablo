package com.qwert2603.btablo

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_search.*
import java.util.*

class SearchActivity : AppCompatActivity() {

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
                    socket.close()

                    startActivity(Intent(this@SearchActivity, TabloActivity::class.java))
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search)

        // todo: receive and consume BT disabled events
        registerReceiver(btReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

        if (bluetoothAdapter.isEnabled) {
            searchAnthDevice()
        } else {
            requestEnableBT()
        }

        enableBT_Button.setOnClickListener { requestEnableBT() }
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
                searchAnthDevice()
            } else {
                LogUtils.d("BT denied")
                progressBar.setVisible(false)
                enableBT_Button.setVisible(true)
            }
        }
    }

    private fun requestEnableBT() {
        progressBar.setVisible(true)
        enableBT_Button.setVisible(false)
        startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT)
    }

    private fun searchAnthDevice() {
        // todo: search timeout: ~12s
        LogUtils.d("BT startDiscovery ${bluetoothAdapter.startDiscovery()}")
        progressBar.setVisible(true)
        enableBT_Button.setVisible(false)
    }
}