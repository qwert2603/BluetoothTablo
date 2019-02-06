package com.qwert2603.btablo.start

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.qwert2603.btablo.R
import com.qwert2603.btablo.server.ServerActivity
import com.qwert2603.btablo.tablo.TabloActivity
import kotlinx.android.synthetic.main.activity_start.*

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        server_Button.setOnClickListener {
            startActivity(Intent(this, ServerActivity::class.java))
            finish()
        }
        client_Button.setOnClickListener {
            startActivity(Intent(this, TabloActivity::class.java))
            finish()
        }
    }
}