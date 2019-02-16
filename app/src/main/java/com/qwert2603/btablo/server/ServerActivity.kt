package com.qwert2603.btablo.server

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qwert2603.btablo.R
import com.qwert2603.btablo.model.BluetoothRepoImpl
import com.qwert2603.btablo.model.TabloConst
import kotlinx.android.synthetic.main.activity_server.*
import kotlin.concurrent.thread

class ServerActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        clear_Button.setOnClickListener { textView.text = "" }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "BT is disabled!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE))

        val serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
            getString(R.string.app_name),
            BluetoothRepoImpl.BT_UUID
        )

        fun addText(text: String) {
            textView.post { textView.text = "${textView.text}$text" }
        }

        thread {
            try {
                val socket = serverSocket.accept()
                serverSocket.close()

                addText("client: ${socket.remoteDevice.name} ${socket.remoteDevice.address}\n")

                while (true) {
                    val byte = socket.inputStream.read().toByte()
                    addText(String.format("%02x", byte) + ' ')
                    if (byte == TabloConst.STOP_BYTE) {
                        addText("\n")
                    }
                }
            } catch (t: Throwable) {
                addText("error: $t")
            }
        }
    }
}