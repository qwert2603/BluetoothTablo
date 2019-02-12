package com.qwert2603.btablo.utils

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.RadioButton
import androidx.appcompat.widget.LinearLayoutCompat
import com.qwert2603.btablo.R

@Suppress("DEPRECATION")
class HoldView(context: Context?, attrs: AttributeSet?) : LinearLayoutCompat(context, attrs) {

    var isTeam2: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            render()
        }

    var onChangesFromUser: ((Boolean) -> Unit)? = null

    init {
        orientation = LinearLayoutCompat.HORIZONTAL

        val outValue = TypedValue()
        getContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)

        for (i in 0 until 2) {
            val radioButton = RadioButton(context)
            radioButton.isEnabled = false
            radioButton.isClickable = false
            radioButton.isFocusable = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                radioButton.buttonTintList = resources.getColorStateList(R.color.accent_check_button)
            }
            addView(radioButton)
        }

        render()

        setOnClickListener {
            isTeam2 = !isTeam2
            onChangesFromUser?.invoke(isTeam2)
        }
    }

    private fun render() {
        (getChildAt(0) as RadioButton).isChecked = !isTeam2
        (getChildAt(1) as RadioButton).isChecked = isTeam2
    }
}