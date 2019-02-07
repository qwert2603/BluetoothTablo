package com.qwert2603.btablo.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.qwert2603.andrlib.util.LogUtils
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kotlin.random.Random

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

fun String.toIntOrZero(): Int = toIntOrNull() ?: 0


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
