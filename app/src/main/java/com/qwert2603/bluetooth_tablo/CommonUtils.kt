package com.qwert2603.bluetooth_tablo

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText

fun EditText.doOnTextChange(action: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            action(s.toString())
        }
    })
}


fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.INVISIBLE else View.GONE
}

fun String.toIntOrZero(): Int = toIntOrNull() ?: 0