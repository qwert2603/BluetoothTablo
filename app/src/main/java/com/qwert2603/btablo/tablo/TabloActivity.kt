package com.qwert2603.btablo.tablo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.core.view.isVisible
import com.qwert2603.btablo.BuildConfig
import com.qwert2603.btablo.R
import com.qwert2603.btablo.di.DIHolder
import com.qwert2603.btablo.mac_settings.MacSettingsActivity
import com.qwert2603.btablo.model.BluetoothConnectionException
import com.qwert2603.btablo.model.BluetoothDeniedException
import com.qwert2603.btablo.model.TabloNotFoundException
import com.qwert2603.btablo.model.WrongChecksumException
import com.qwert2603.btablo.utils.*
import com.qwert2603.permesso.exception.PermissionDeniedException
import kotlinx.android.synthetic.main.activity_tablo.*
import kotlinx.android.synthetic.main.include_attack.*
import kotlinx.android.synthetic.main.include_buttons_plus_minus.view.*
import kotlinx.android.synthetic.main.include_buttons_start_stop.view.*
import kotlinx.android.synthetic.main.include_fouls.view.*
import kotlinx.android.synthetic.main.include_hold.*
import kotlinx.android.synthetic.main.include_points.*
import kotlinx.android.synthetic.main.include_teams.*
import kotlinx.android.synthetic.main.include_time.*
import java.text.SimpleDateFormat
import java.util.*

class TabloActivity : BluetoothActivity() {

    private var isRendering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tablo)

        setSupportActionBar(toolbar)

        setAboutText()

        val vsObservableField: ObservableField<TabloViewState> = DIHolder.settingsRepo.vs

        vsObservableField.changes
            .map { it.game }
            .distinctUntilChanged()
            .observeOn(DIHolder.uiScheduler)
            .subscribe { renderGame(it) }
            .disposeOnDestroy(this)

        vsObservableField.changes
            .observeOn(DIHolder.uiScheduler)
            .subscribe { render(it) }
            .disposeOnDestroy(this)

        DIHolder.settingsRepo.sendingState
            .observeOn(DIHolder.uiScheduler)
            .subscribe { renderSendingState(it) }
            .disposeOnDestroy(this)

        DIHolder.settingsRepo.isStarted
            .observeOn(DIHolder.uiScheduler)
            .subscribe {
                time_startStop.start_Button.isEnabled = !it
                time_startStop.stop_Button.isEnabled = it

                minutes_EditText.isEnabled = !it
                seconds_EditText.isEnabled = !it
            }
            .disposeOnDestroy(this)

        DIHolder.settingsRepo.isAttackStarted
            .observeOn(DIHolder.uiScheduler)
            .subscribe {
                attack_StartStop.start_Button.isEnabled = !it
                attack_StartStop.stop_Button.isEnabled = it

                attackSeconds_EditText.isEnabled = !it
            }
            .disposeOnDestroy(this)

        setListeners(vsObservableField)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tablo, menu)
        super.onCreateOptionsMenu(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.mac_settings) {
            startActivity(Intent(this, MacSettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun renderGame(game: Game) {
        LogUtils.d { "TabloActivity renderGame $game" }

        background_ImageView.setImageResource(
            when (game) {
                Game.FOOTBALL -> R.drawable.background_football
                Game.VOLLEYBALL -> R.drawable.background_volleyball
                Game.BASKETBALL -> R.drawable.background_basketball
            }
        )

        attack_Panel.isVisible = game == Game.BASKETBALL
        val timeoutsVisible = game in listOf(Game.VOLLEYBALL, Game.BASKETBALL)
        listOf(timeouts1, timeouts2).forEach { it.isVisible = timeoutsVisible }
        when (game) {
            Game.FOOTBALL -> {
                fouls2.setPaddingRelative(resources.getDimensionPixelSize(R.dimen.edit_text_margin_start), 0, 0, 0)
                hold_LinearLayout.isVisible = false
            }
            Game.VOLLEYBALL -> {
                fouls2.setPaddingRelative(0, 0, 0, 0)
                hold_LinearLayout.isVisible = true
                hold_TextView.setText(R.string.text_inning)
            }
            Game.BASKETBALL -> {
                fouls2.setPaddingRelative(0, 0, 0, 0)
                hold_LinearLayout.isVisible = true
                hold_TextView.setText(R.string.text_hold)
            }
        }.also { }
        period_TextInputLayout.hint = getString(
            when (game) {
                Game.FOOTBALL -> R.string.text_half
                Game.VOLLEYBALL -> R.string.text_match
                Game.BASKETBALL -> R.string.text_quarter
            }
        )
        val foulsString = getString(
            when (game) {
                Game.FOOTBALL -> R.string.hint_fouls
                Game.VOLLEYBALL -> R.string.text_points_by_games
                Game.BASKETBALL -> R.string.hint_fouls
            }
        )
        listOf(fouls1.fouls_TextInputLayout, fouls2.fouls_TextInputLayout).forEach { it.hint = foulsString }
    }

    private fun render(vs: TabloViewState) {

        isRendering = true

        LogUtils.d { "TabloActivity render $vs" }

        toolbar.title = "${getString(R.string.app_name)} / ${getString(vs.game.nameRes)}"

        team1_EditText.setTextQQ(vs.team1)
        team2_EditText.setTextQQ(vs.team2)

        minutes_EditText.setTextFromInt(vs.minutes)
        seconds_EditText.setTextFromInt(vs.seconds)

        points1_EditText.setTextFromInt(vs.points1)
        points2_EditText.setTextFromInt(vs.points2)
        period_EditText.setTextFromInt(vs.period)

        points1_PlusMinus.plus_Button.isEnabled = vs.isPoints1PlusEnabled()
        points1_PlusMinus.minus_Button.isEnabled = vs.isPoints1MinusEnabled()

        points2_PlusMinus.plus_Button.isEnabled = vs.isPoints2PlusEnabled()
        points2_PlusMinus.minus_Button.isEnabled = vs.isPoints2MinusEnabled()

        period_PlusMinus.plus_Button.isEnabled = vs.isPeriodPlusEnabled()
        period_PlusMinus.minus_Button.isEnabled = vs.isPeriodMinusEnabled()

        fouls1.fouls_EditText.setTextFromInt(vs.fouls1)
        fouls2.fouls_EditText.setTextFromInt(vs.fouls2)

        fouls1.fouls_PlusMinus.plus_Button.isEnabled = vs.isFouls1PlusEnabled()
        fouls1.fouls_PlusMinus.minus_Button.isEnabled = vs.isFouls1MinusEnabled()

        fouls2.fouls_PlusMinus.plus_Button.isEnabled = vs.isFouls2PlusEnabled()
        fouls2.fouls_PlusMinus.minus_Button.isEnabled = vs.isFouls2MinusEnabled()

        timeouts1.timeouts = vs.timeouts1
        timeouts2.timeouts = vs.timeouts2

        holdView.isTeam2 = vs.holdIsTeam2

        attackSeconds_EditText.setTextFromInt(vs.attackSeconds)

        isRendering = false
    }

    private fun renderSendingState(sendingState: SendingState) {
        if (sendingState == SendingState.NotSent) {
            toolbar.subtitle = ""
            return
        }

        toolbar.subtitle = getString(
            when (sendingState) {
                SendingState.Sending -> R.string.sending_state_sending
                is SendingState.Error -> when (sendingState.t) {
                    is PermissionDeniedException -> R.string.sending_state_error_no_geo_permission
                    is BluetoothDeniedException -> R.string.sending_state_error_bluetooth_denied
                    is TabloNotFoundException -> R.string.sending_state_error_tablo_not_found
                    is BluetoothConnectionException -> R.string.sending_state_error_connection_exception
                    is WrongChecksumException -> R.string.sending_state_error_wrong_checksum
                    else -> R.string.sending_state_error
                }
                SendingState.Success -> R.string.sending_state_success
                SendingState.NotSent -> null!!
            }
        )
        @Suppress("DEPRECATION")
        toolbar.setSubtitleTextColor(
            resources.getColor(
                when (sendingState) {
                    SendingState.Sending -> R.color.sending_state_sending
                    is SendingState.Error -> R.color.sending_state_error
                    SendingState.Success -> R.color.sending_state_success
                    SendingState.NotSent -> null!!
                }
            )
        )
    }

    private fun setListeners(vsObservableField: ObservableField<TabloViewState>) {
        Game.values().forEach { game ->
            val button = layoutInflater.inflate(R.layout.include_reset_button, resetButtons_container, false) as Button
            resetButtons_container.addView(button)
            button.setText(game.nameRes)
            button.setOnClickListener {
                DIHolder.settingsRepo.setStarted(false)
                DIHolder.settingsRepo.setAttackStarted(false)
                DIHolder.settingsRepo.prepareForSendAll()
                vsObservableField.updateField { TabloViewState.createDefault(game) }
                hideKeyboard()
            }
        }

        team1_EditText.doOnTextChangeQQ { vsObservableField.updateField { vs -> vs.copy(team1 = it) } }
        team2_EditText.doOnTextChangeQQ { vsObservableField.updateField { vs -> vs.copy(team2 = it) } }

        sendTeams_Button.setOnClickListener { DIHolder.settingsRepo.sendTeams() }

        minutes_EditText.doOnTextChangeIntQQ { vsObservableField.updateField { vs -> vs.copy(minutes = it) } }
        seconds_EditText.doOnTextChangeIntQQ {
            vsObservableField.updateField { vs ->
                vs.copy(seconds = it.takeIf { it <= TabloViewState.MAX_SECONDS } ?: 0)
            }
        }

        time_startStop.start_Button.setOnClickListener {
            DIHolder.settingsRepo.setStarted(true)
            hideKeyboard()
        }
        time_startStop.stop_Button.setOnClickListener { DIHolder.settingsRepo.setStarted(false) }

        fun updateIntChanges(
            editText: EditText,
            plusButton: Button,
            minusButton: Button,
            getter: TabloViewState.() -> Int,
            updater: TabloViewState.(Int) -> TabloViewState,
            maxValue: Int,
            minvalue: Int = 0
        ) {
            editText.doOnTextChangeIntQQ { value -> vsObservableField.updateField { vs -> updater(vs, value) } }
            plusButton.setOnClickListener {
                vsObservableField.updateField { vs ->
                    val newValue = vs.getter().inc().coerceAtMost(maxValue)
                    vs.updater(newValue)
                }
            }
            minusButton.setOnClickListener {
                vsObservableField.updateField { vs ->
                    val newValue = vs.getter().dec().coerceAtLeast(minvalue)
                    vs.updater(newValue)
                }
            }
        }

        updateIntChanges(
            editText = points1_EditText,
            plusButton = points1_PlusMinus.plus_Button,
            minusButton = points1_PlusMinus.minus_Button,
            getter = { points1 },
            updater = { copy(points1 = it) },
            maxValue = TabloViewState.MAX_POINTS
        )
        updateIntChanges(
            editText = points2_EditText,
            plusButton = points2_PlusMinus.plus_Button,
            minusButton = points2_PlusMinus.minus_Button,
            getter = { points2 },
            updater = { copy(points2 = it) },
            maxValue = TabloViewState.MAX_POINTS
        )
        updateIntChanges(
            editText = period_EditText,
            plusButton = period_PlusMinus.plus_Button,
            minusButton = period_PlusMinus.minus_Button,
            getter = { period },
            updater = { copy(period = it) },
            maxValue = TabloViewState.MAX_PERIOD
        )
        updateIntChanges(
            editText = fouls1.fouls_EditText,
            plusButton = fouls1.fouls_PlusMinus.plus_Button,
            minusButton = fouls1.fouls_PlusMinus.minus_Button,
            getter = { fouls1 },
            updater = { copy(fouls1 = it) },
            maxValue = TabloViewState.MAX_FOULS
        )
        updateIntChanges(
            editText = fouls2.fouls_EditText,
            plusButton = fouls2.fouls_PlusMinus.plus_Button,
            minusButton = fouls2.fouls_PlusMinus.minus_Button,
            getter = { fouls2 },
            updater = { copy(fouls2 = it) },
            maxValue = TabloViewState.MAX_FOULS
        )

        timeouts1.onTimeoutsChangesFromUser = { vsObservableField.updateField { vs -> vs.copy(timeouts1 = it) } }
        timeouts2.onTimeoutsChangesFromUser = { vsObservableField.updateField { vs -> vs.copy(timeouts2 = it) } }

        holdView.onChangesFromUser = { vsObservableField.updateField { vs -> vs.copy(holdIsTeam2 = it) } }

        attackSeconds_EditText.doOnTextChangeIntQQ { vsObservableField.updateField { vs -> vs.copy(attackSeconds = it) } }

        attack_StartStop.start_Button.setOnClickListener {
            DIHolder.settingsRepo.setAttackStarted(true)
            hideKeyboard()
        }
        attack_StartStop.stop_Button.setOnClickListener { DIHolder.settingsRepo.setAttackStarted(false) }

        signal1_Button.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                DIHolder.settingsRepo.setSignal1(true)
            }
            false
        }
        signal2_Button.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                DIHolder.settingsRepo.setSignal2(true)
            }
            false
        }

        sec24_Button.setOnClickListener {
            vsObservableField.updateField { vs -> vs.copy(attackSeconds = 24) }
            DIHolder.settingsRepo.setAttackStarted(true)
        }
        sec14_Button.setOnClickListener {
            vsObservableField.updateField { vs -> vs.copy(attackSeconds = 14) }
            DIHolder.settingsRepo.setAttackStarted(true)
        }

        sendAll_Button.setOnClickListener {
            DIHolder.settingsRepo.sendAll()
        }
    }

    private fun setAboutText() {
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

    private fun hideKeyboard() {
        tablo_CoordinatorLayout.requestFocus()

        val currentFocus = currentFocus ?: return
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
    }

    private fun EditText.doOnTextChangeQQ(action: (String) -> Unit) {
        this.doOnTextChange { s ->
            if (!isRendering) {
                val filtered = s.filter {
                    it in 'a'..'z' || it in 'A'..'Z' || it in 'а'..'я' || it in 'А'..'Я' || it in '0'..'9' || it == ' '
                }
                action(filtered)
            }
        }
    }

    private fun EditText.doOnTextChangeIntQQ(action: (Int) -> Unit) = doOnTextChangeQQ { action(it.toIntOrZero()) }

    companion object {

        private fun EditText.setTextQQ(text: String) {
            if (text != this.text.toString()) {
                this.setText(text)
                this.setSelection(text.length)
            }
        }

        private fun EditText.setTextFromInt(i: Int) {
            this.setTextQQ(i.toString())
            if (this.isEnabled && i == 0) {
                this.setSelection(0, 1)
            }
        }
    }
}