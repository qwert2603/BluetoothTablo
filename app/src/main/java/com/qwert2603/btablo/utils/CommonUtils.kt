package com.qwert2603.btablo.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.annotation.IntRange
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.qwert2603.btablo.model.TabloConst
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import kotlin.random.Random

fun EditText.doOnTextChange(withInitial: Boolean = false, action: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            action(s.toString())
        }
    })

    if (withInitial) {
        action(text.toString())
    }
}

fun String.toIntOrZero(): Int = toIntOrNull() ?: 0

fun Int.toStringZeroToEmpty() = if (this != 0) toString() else ""


fun EditText.doOnTextChangeInt(action: (Int) -> Unit) = doOnTextChange { action(it.toIntOrZero()) }

fun <T> Single<T>.cacheIfSuccess(name: String = Random.nextInt(1000000).toString()) = CachingSingle(this, name)

class CachingSingle<T>(private val origin: Single<T>, private val name: String) {
    @Volatile
    private lateinit var cached: Single<T>

    @Volatile
    private var disposable: Disposable? = null

    init {
        reset()
    }

    private fun createCached() = origin
        .toObservable()
        .cache()
        .firstOrError()

    @Synchronized
    fun reset() {
        LogUtils.d("CachingSingle $name reset")
        disposable?.dispose()
        disposable = null
        cached = createCached()
    }

    @Synchronized
    private fun makeSubscribe(source: Single<T>) {
        if (disposable != null) return
        LogUtils.d("CachingSingle $name makeSubscribe")
        disposable = source.subscribe(
            {},
            {
                LogUtils.d("CachingSingle $name doOnError $it")
                reset()
            }
        )
    }

    @Synchronized
    fun get(): Single<T> {
        makeSubscribe(cached)
        return cached
    }
}

fun noThrow(action: () -> Unit) {
    try {
        action()
    } catch (t: Throwable) {
    }
}

@IntRange(from = 0, to = 9)
fun Int.digitAt(digit: Int): Int {
    var divider = 1
    for (i in 0 until digit) {
        divider *= 10
    }
    return this / divider % 10
}

fun Int.convertToByte(): Int =
    if (TabloConst.TEST_MODE) {
        (this.toByte() + '0'.toByte())
    } else {
        this and 0xFF
    }

fun String.convertToBytes(): IntArray = this
    .map { it.toInt() }
    .map {
        if (it in 'А'.toInt()..'Я'.toInt() || it in 'а'.toInt()..'я'.toInt()) {
            it - TabloConst.OFFSET_RUSSIAN
        } else {
            it
        }
    }
    .toIntArray()


fun <T, U> makePair() = BiFunction { t: T, u: U -> Pair(t, u) }
fun <T, U> firstOfTwo() = BiFunction { t: T, _: U -> t }
fun <T, U> secondOfTwo() = BiFunction { _: T, u: U -> u }
fun <T, U, V> makeTriple() = Function3 { t: T, u: U, v: V -> Triple(t, u, v) }

fun Disposable.disposeOnDestroy(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun on() {
            this@disposeOnDestroy.dispose()
        }
    })
}

fun String.isMacAddress(): Boolean {
    if (this.length != TabloConst.MAC_ADDRESS_LENGTH) return false
    for (i in 0 until TabloConst.MAC_ADDRESS_LENGTH) {
        if (i % 3 == 2) {
            if (this[i] != ':') return false
        } else {
            if (this[i] !in '0'..'9' && this[i] !in 'A'..'F') return false
        }
    }
    return true
}