package com.qwert2603.btablo

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations

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
    visibility = if (visible) View.VISIBLE else View.GONE
}

fun String.toIntOrZero(): Int = toIntOrNull() ?: 0

fun <T, U> LiveData<T>.switchMap(mapper: (T) -> LiveData<U>): LiveData<U> = Transformations.switchMap(this, mapper)