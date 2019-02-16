package com.qwert2603.btablo.utils

interface StateHolder<VS> {
    val prevViewState: VS?
    val currentViewState: VS
}

inline fun <VS, T> StateHolder<VS>.renderIfChanged(crossinline field: VS.() -> T, crossinline renderer: (T) -> Unit) {
    val prevViewState = prevViewState
    val currentField = field(currentViewState)
    if (prevViewState == null) {
        renderer(currentField)
        return
    }
    val prevField = field(prevViewState)
    if (currentField !== prevField) {
        renderer(currentField)
    }
}

inline fun <VS, T, U> StateHolder<VS>.renderIfChangedTwo(crossinline fields: VS.() -> Pair<T, U>, crossinline renderer: (Pair<T, U>) -> Unit) {
    val prevViewState = prevViewState
    val currentField = fields(currentViewState)
    if (prevViewState == null) {
        renderer(currentField)
        return
    }
    val prevField = fields(prevViewState)
    if (currentField.first !== prevField.first || currentField.second !== prevField.second) {
        renderer(currentField)
    }
}

inline fun <VS, T, U, V> StateHolder<VS>.renderIfChangedThree(crossinline fields: VS.() -> Triple<T, U, V>, crossinline renderer: (Triple<T, U, V>) -> Unit) {
    val prevViewState = prevViewState
    val currentField = fields(currentViewState)
    if (prevViewState == null) {
        renderer(currentField)
        return
    }
    val prevField = fields(prevViewState)
    if (currentField.first !== prevField.first || currentField.second !== prevField.second || currentField.third !== prevField.third) {
        renderer(currentField)
    }
}

inline fun <VS, T> StateHolder<VS>.renderIfChangedEqual(crossinline field: VS.() -> T, crossinline renderer: (T) -> Unit) {
    val prevViewState = prevViewState
    val currentField = field(currentViewState)
    if (prevViewState == null) {
        renderer(currentField)
        return
    }
    val prevField = field(prevViewState)
    if (currentField != prevField) {
        renderer(currentField)
    }
}

inline fun <VS, T> StateHolder<VS>.renderIfChangedWithFirstRendering(crossinline field: VS.() -> T, crossinline renderer: (T, isFirstRendering: Boolean) -> Unit) {
    val prevViewState = prevViewState
    val currentField = field(currentViewState)
    if (prevViewState == null || currentField !== field(prevViewState)) {
        renderer(currentField, prevViewState == null)
    }
}