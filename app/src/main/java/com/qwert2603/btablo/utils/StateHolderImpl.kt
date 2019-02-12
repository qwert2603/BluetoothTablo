package com.qwert2603.btablo.utils

import androidx.annotation.CallSuper
import com.qwert2603.andrlib.util.StateHolder

abstract class StateHolderImpl<VS : Any> : StateHolder<VS> {

    private var everRendered = false

    override var prevViewState: VS? = null

    override lateinit var currentViewState: VS

    @CallSuper
    open fun render(vs: VS) {
        if (everRendered) {
            prevViewState = currentViewState
        } else {
            everRendered = true
        }
        currentViewState = vs
    }

    fun renderAll() {
        everRendered = false
        prevViewState = null
        render(currentViewState)
    }
}