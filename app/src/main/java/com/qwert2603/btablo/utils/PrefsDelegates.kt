package com.qwert2603.btablo.utils

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefsString(
    private val prefs: SharedPreferences,
    private val key: String,
    private val defaultValue: String = ""
) : ReadWriteProperty<Any, String> {
    override fun getValue(thisRef: Any, property: KProperty<*>): String = prefs.getString(key, defaultValue)!!

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String) {
        prefs.edit { putString(key, value) }
    }
}

class PrefsInt(
    private val prefs: SharedPreferences,
    private val key: String,
    private val defaultValue: Int = 0
) : ReadWriteProperty<Any, Int> {
    override fun getValue(thisRef: Any, property: KProperty<*>): Int = prefs.getInt(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
        prefs.edit { putInt(key, value) }
    }
}

abstract class ObservableField<T> {
    protected val lock = Any()

    abstract var field: T
    abstract val changes: Observable<T>

    fun updateField(updater: (T) -> T): T {
        synchronized(lock) {
            field = updater(field)
            return field
        }
    }
}


object PreferenceUtils {

    inline fun <reified T : Any> createPrefsObjectNullable(
        prefs: SharedPreferences,
        key: String,
        gson: Gson
    ) = object : ReadWriteProperty<Any, T?> {
        override fun getValue(thisRef: Any, property: KProperty<*>): T? =
            if (key in prefs) {
                gson.fromJson(prefs.getString(key, ""), object : TypeToken<T>() {}.type)
            } else {
                null
            }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
            prefs.edit {
                if (value != null) {
                    putString(key, gson.toJson(value))
                } else {
                    remove(key)
                }
            }
        }
    }

    inline fun <reified T : Any> createPrefsObjectObservable(
        prefs: SharedPreferences,
        key: String,
        gson: Gson,
        defaultValue: T
    ): ObservableField<T> {
        val changes = BehaviorSubject.create<T>()

        return object : ObservableField<T>() {
            init {
                changes.onNext(field)
            }

            override var field: T
                get() =
                    if (key in prefs) {
                        try {
                            gson.fromJson<T>(prefs.getString(key, ""), object : TypeToken<T>() {}.type)
                        } catch (t: Throwable) {
                            defaultValue
                        }
                    } else {
                        defaultValue
                    }
                set(value) {
                    synchronized(lock) {
                        prefs.edit { putString(key, gson.toJson(value)) }
                        changes.onNext(value)
                    }
                }

            override val changes: Observable<T> = changes.hide()

        }
    }

}