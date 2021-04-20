package cn.edu.tsinghua.thss.cercis.util

import android.content.Context
import android.content.SharedPreferences

object PreferencesHelper {
    object Auth {
        private fun getAuthPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        }

        /**
         * Called in [cn.edu.tsinghua.thss.cercis.StartupActivity]
         */
        fun isLoggedIn(context: Context): Boolean {
            return getAuthPreferences(context).getBoolean("logged_in", false)
        }
    }
}