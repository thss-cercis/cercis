package cn.edu.tsinghua.thss.cercis.util

import android.content.Context
import android.content.SharedPreferences

object PreferencesHelper {
    object Auth {
        private fun getAuthPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        }

        /**
         * Checks if first startup.
         */
        fun isFirstStartup(context: Context): Boolean {
            return getAuthPreferences(context).getBoolean("first_startup", true)
        }

        /**
         * Sets if first startup.
         */
        fun setFirstStartup(context: Context, firstStartup: Boolean) {
            getAuthPreferences(context).edit().putBoolean("first_startup", true).apply()
        }
    }
}