package cn.edu.tsinghua.thss.cercis.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import cn.edu.tsinghua.thss.cercis.http.CercisHttpService
import cn.edu.tsinghua.thss.cercis.http.LoginRequest
import cn.edu.tsinghua.thss.cercis.http.SignUpRequest
import cn.edu.tsinghua.thss.cercis.module.AuthorizedLiveEvent
import cn.edu.tsinghua.thss.cercis.util.LOG_TAG
import cn.edu.tsinghua.thss.cercis.util.NetworkResponse
import cn.edu.tsinghua.thss.cercis.util.UserId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext val context: Context,
    @AuthorizedLiveEvent val authorized: MutableLiveData<Boolean?>,
    private val httpService: CercisHttpService,
) {
    private val sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    var currentUserId: UserId
        get() = sharedPreferences.getLong("user_id", -1L)
        set(value) {
            sharedPreferences.edit().putLong("user_id", value).apply()
        }

    val loggedIn = MutableLiveData(currentUserId != -1L).apply {
        Log.d(LOG_TAG, "${authorized.hashCode()}")
        authorized.observeForever {
            if (it == false) {
                currentUserId = -1L
                postValue(false)
            }
        }
    }

    suspend fun signUp(signUpRequest: SignUpRequest) = httpService.signUp(signUpRequest).also {
        if (it is NetworkResponse.Success) {
            currentUserId = it.data.userId
            loggedIn.postValue(true)
        }
    }

    suspend fun login(loginRequest: LoginRequest) = httpService.login(loginRequest).also {
        if (it is NetworkResponse.Success) {
            currentUserId = it.data.userId
            loggedIn.postValue(true)
        }
    }

    suspend fun logout() = httpService.logout().also {
        if (it is NetworkResponse.Success) {
            currentUserId = -1L
            loggedIn.postValue(false)
        }
    }

    fun getUserDatabaseAbsolutePath(name: String) =
        "${context.getDatabasePath(currentUserId.toString()).absolutePath}/$name"
}
