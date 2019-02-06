package com.qwert2603.btablo.tablo

import android.os.Bundle
import android.text.Html
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.qwert2603.andrlib.base.mvi.ViewAction
import com.qwert2603.andrlib.util.setVisible
import com.qwert2603.btablo.BuildConfig
import com.qwert2603.btablo.R
import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.model.BluetoothConnectionException
import com.qwert2603.btablo.model.BluetoothDeniedException
import com.qwert2603.btablo.model.TabloNotFoundException
import com.qwert2603.btablo.model.WrongChecksumException
import com.qwert2603.btablo.utils.BluetoothActivity
import com.qwert2603.btablo.utils.doOnTextChange
import com.qwert2603.btablo.utils.toIntOrZero
import com.qwert2603.permesso.exception.PermissionDeniedException
import io.reactivex.Observable
import kotlinx.android.synthetic.main.activity_tablo.*
import java.text.SimpleDateFormat
import java.util.*

class TabloActivity : BluetoothActivity<TabloViewState, TabloView, TabloPresenter>(), TabloView {

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
            sending_ProgressBar.setVisible(vs.sendingState == SendingState.Sending)
            sendingState_TextView.text = getString(
                when (vs.sendingState) {
                    SendingState.Sending -> R.string.sending_state_sending
                    is SendingState.Error -> when (vs.sendingState.t) {
                        is PermissionDeniedException -> R.string.sending_state_error_no_geo_permission
                        is BluetoothDeniedException -> R.string.sending_state_error_bluetooth_denied
                        is TabloNotFoundException -> R.string.sending_state_error_tablo_not_found
                        is BluetoothConnectionException -> R.string.sending_state_error_connection_exception
                        is WrongChecksumException -> R.string.sending_state_error_wrong_checksum
                        else -> R.string.sending_state_error
                    }
                    SendingState.Success -> R.string.sending_state_success
                }
            )
            @Suppress("DEPRECATION")
            sendingState_TextView.setTextColor(
                resources.getColor(
                    when (vs.sendingState) {
                        SendingState.Sending -> R.color.sending_state_sending
                        is SendingState.Error -> R.color.sending_state_error
                        SendingState.Success -> R.color.sending_state_success
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