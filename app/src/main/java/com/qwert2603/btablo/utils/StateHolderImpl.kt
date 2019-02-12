package com.qwert2603.btablo.utils

import androidx.annotation.CallSuper
import com.qwert2603.andrlib.util.StateHolder

abstract class StateHolderImpl<VS : Any>(initialViewState: VS? = null) : StateHolder<VS> {

    private var everRendered = false

    final override var prevViewState: VS? = null

    final override lateinit var currentViewState: VS

    init {
        if (initialViewState != null) {
            everRendered = true
            currentViewState = initialViewState
        }
    }

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
        resetPrevViewState()
        render(currentViewState)
    }

    fun resetPrevViewState() {
        everRendered = false
        prevViewState = null
    }
}