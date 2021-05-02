package cn.edu.tsinghua.thss.cercis.repository

import android.content.Context
import cn.edu.tsinghua.thss.cercis.util.UserId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    var userId: UserId
        get() = sharedPreferences.getLong("user_id", -1L)
        set(value) {
            sharedPreferences.edit().putLong("user_id", value).apply()
        }

    val loggedIn get() = userId != -1L

    fun logout() { userId = -1L }
}
