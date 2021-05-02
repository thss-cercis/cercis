package cn.edu.tsinghua.thss.cercis.http

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import cn.edu.tsinghua.thss.cercis.module.AuthorizedLiveEvent
import cn.edu.tsinghua.thss.cercis.util.LOG_TAG
import cn.edu.tsinghua.thss.cercis.util.SingleLiveEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationData @Inject constructor(
    @ApplicationContext val context: Context,
    @AuthorizedLiveEvent val authorized: SingleLiveEvent<Boolean?>
) {
    private val sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    /**
     * Checks if a user is logged in.
     *
     * Setting this value would trigger a write-back to shared preferences.
     */
    val loggedIn = MutableLiveData(sharedPreferences.getBoolean("logged_in", false)).also {
        Log.d(LOG_TAG, "read logged_in: ${it.value}")
        it.observeForever { value ->
            Log.d(LOG_TAG, "write logged_in: $value")
            sharedPreferences.edit().putBoolean("logged_in", value).apply()
        }
        Log.d(LOG_TAG, "${authorized.hashCode()}")
        authorized.observeForever { value ->
            Log.d(LOG_TAG, "logged in set to $value")
            if (value != null && value != it.value) {
                it.postValue(value)
            }
        }
    }

    /**
     * Currently logged in user id.
     *
     * -1 if no previous login.
     */
    val userId = MutableLiveData(sharedPreferences.getLong("user_id", -1)).also {
        it.observeForever { value ->
            sharedPreferences.edit().putLong("user_id", value).apply()
        }
    }
}