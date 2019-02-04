package com.qwert2603.bluetooth_tablo

import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

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

        send_Button.setOnClickListener {
            LogUtils.d("send ${settingsRepo.getMessage()}")
        }

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
    }
}
