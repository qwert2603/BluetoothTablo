package com.qwert2603.btablo.server

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qwert2603.btablo.R
import com.qwert2603.btablo.model.AnthTabloRepo
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
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "BT is disabled!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE))

        val serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
            getString(R.string.app_name),
            AnthTabloRepo.BT_UUID
        )

        fun addText(text: String) {
            textView.post { textView.text = "${textView.text}\n$text" }
        }

        thread {
            try {
                val socket = serverSocket.accept()
                serverSocket.close()

                addText("client: ${socket.remoteDevice.name}")

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