package cn.cercis.repository

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import cn.cercis.util.helper.PreferencesHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KProperty

/**
 * Provides global configurations of the app.
 */
@Singleton
class GlobalConfigRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val pref = context.getSharedPreferences("config", MODE_PRIVATE)

    private fun SharedPreferences.stringProperty(key: String, defaultValue: String) =
        DelegatedPropertyImpl(this,
            { getString(key, defaultValue) ?: defaultValue },
            { putString(key, it) })

    private fun SharedPreferences.nullableStringProperty(key: String, defaultValue: String?) =
        DelegatedPropertyImpl(this, { getString(key, defaultValue) }, { putString(key, it) })

    private fun SharedPreferences.intProperty(key: String, defaultValue: Int) =
        DelegatedPropertyImpl(this, { getInt(key, defaultValue) }, { putInt(key, it) })

    private fun SharedPreferences.longProperty(key: String, defaultValue: Long) =
        DelegatedPropertyImpl(this, { getLong(key, defaultValue) }, { putLong(key, it) })

    private fun SharedPreferences.booleanProperty(key: String, defaultValue: Boolean) =
        DelegatedPropertyImpl(this, { getBoolean(key, defaultValue) }, { putBoolean(key, it) })

    interface DelegatedProperty<T> {
        operator fun getValue(thisRef: GlobalConfigRepository, property: KProperty<*>): T

        operator fun setValue(thisRef: GlobalConfigRepository, property: KProperty<*>, value: T)
    }

    class DelegatedPropertyImpl<T>(
        private val sharedPreferences: SharedPreferences,
        private val getter: SharedPreferences.() -> T,
        private val setter: SharedPreferences.Editor.(T) -> Unit,
    ) : DelegatedProperty<T> {
        override fun getValue(thisRef: GlobalConfigRepository, property: KProperty<*>): T {
            return sharedPreferences.getter()
        }

        override fun setValue(thisRef: GlobalConfigRepository, property: KProperty<*>, value: T) {
            sharedPreferences.edit().apply { setter(value); apply() }
        }
    }
}
