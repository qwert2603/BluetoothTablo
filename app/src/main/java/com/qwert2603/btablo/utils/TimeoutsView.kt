package com.qwert2603.btablo.utils

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.CheckBox
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.children
import com.qwert2603.andrlib.util.color
import com.qwert2603.btablo.R

class TimeoutsView(context: Context?, attrs: AttributeSet?) : LinearLayoutCompat(context, attrs) {

    companion object {
        const val MAX_TIMEOUTS = 3
    }

    var timeouts: Int = 0
        set(value) {
            if (value == field) return
            field = value
            children
                .map { it as CheckBox }
                .toList()
                .reversed()
                .forEachIndexed { index, checkBox ->
                    checkBox.isChecked = index < timeouts
                }
        }

    var onTimeoutsChangesFromUser: ((Int) -> Unit)? = null

    init {
        orientation = LinearLayoutCompat.VERTICAL

        val outValue = TypedValue()
        getContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)

        for (i in 0 until MAX_TIMEOUTS) {
            val checkBox = CheckBox(context)
            checkBox.isEnabled = false
            checkBox.isClickable = false
            checkBox.isFocusable = false
            checkBox.buttonTintList = ColorStateList.valueOf(resources.color(R.color.colorAccent))
            addView(checkBox)
        }

        if (isInEditMode) {
            timeouts = 2
        }

        setOnClickListener {
            timeouts = (timeouts + 1)
                .let { if (it > MAX_TIMEOUTS) 0 else it }
            onTimeoutsChangesFromUser?.invoke(timeouts)
        }
    }
}