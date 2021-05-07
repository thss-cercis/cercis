package cn.cercis.util.helper

import android.content.Context
import android.content.SharedPreferences
import cn.cercis.common.NO_USER

object PreferencesHelper {
    object Auth {
        private fun getAuthPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        }

        /**
         * Called in [cn.cercis.StartupActivity]
         */
        fun isLoggedIn(context: Context): Boolean {
            return getAuthPreferences(context).getLong("user_id", NO_USER) != NO_USER
        }
    }
}
