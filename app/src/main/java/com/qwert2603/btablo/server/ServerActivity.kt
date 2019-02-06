package com.qwert2603.btablo.server

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.qwert2603.btablo.R
import com.qwert2603.btablo.model.TabloRepo
import kotlinx.android.synthetic.main.activity_server.*
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.concurrent.thread

class ServerActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        clear_Button.setOnClickListener { textView.text = "" }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE))

        val serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
            getString(R.string.app_name),
            TabloRepo.BT_UUID
        )

        fun addText(text: String) {
            textView.post { textView.text = "${textView.text}\n$text" }
        }

        thread {
            try {
                val socket = serverSocket.accept()
                serverSocket.close()

                addText("socket = ${socket.isConnected} $socket")

                val bufferedReader = BufferedReader(InputStreamReader(socket.inputStream))

                while (true) {
                    val line = bufferedReader.readLine()
                    addText("in: $line")
                    val checksum = line.hashCode().toString()
                    socket.outputStream.write("$checksum\n".toByteArray())
                    socket.outputStream.flush()
                    addText("out: $checksum")
                }
            } catch (t: Throwable) {
                addText("error: $t")
            }
        }
    }
}