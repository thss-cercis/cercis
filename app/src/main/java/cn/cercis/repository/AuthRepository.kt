package cn.cercis.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import cn.cercis.common.LOG_TAG
import cn.cercis.common.NO_USER
import cn.cercis.common.UserId
import cn.cercis.http.*
import cn.cercis.module.AuthorizedEvent
import cn.cercis.util.resource.NetworkResponse
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import org.reactivestreams.Subscriber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext val context: Context,
    @AuthorizedEvent val authorized: MutableLiveData<Boolean?>,
    private val httpService: CercisHttpService,
) {
    private val sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    var currentUserId: UserId
        get() = sharedPreferences.getLong("user_id", NO_USER)
        set(value) {
            sharedPreferences.edit().putLong("user_id", value).apply()
        }

    val loggedIn = MutableLiveData(currentUserId != NO_USER).apply {
        Log.d(LOG_TAG, "${authorized.hashCode()}")
        authorized.observeForever {
            if (it == false) {
                currentUserId = NO_USER
                value = false
                authorized.value = null
            }
        }
    }

    suspend fun sendSignUpSms(mobile: String) =
        httpService.sendSignUpSms(SendSmsRequest(mobile))

    suspend fun signUp(
        nickname: String,
        mobile: String,
        verificationCode: String,
        password: String,
    ) = httpService.signUp(
        SignUpRequest(
            nickname = nickname,
            mobile = mobile,
            verificationCode = verificationCode,
            password = password,
        )
    )

    suspend fun login(
        id: UserId?,
        mobile: String?,
        password: String,
    ) = httpService.login(
        LoginRequest(
            id = id,
            mobile = mobile,
            password = password,
        )
    ).also {
        if (it is NetworkResponse.Success) {
            currentUserId = it.data.id
            loggedIn.postValue(true)
        }
    }

    suspend fun logout() = httpService.logout().also {
        if (it is NetworkResponse.Success) {
            currentUserId = NO_USER
            loggedIn.postValue(false)
        }
    }

    suspend fun sendPasswordResetSms(mobile: String) =
        httpService.sendPasswordResetSms(SendSmsRequest(mobile))

    suspend fun resetPassword(
        mobile: String,
        newPassword: String,
        verificationCode: String,
    ) = httpService.resetPassword(
        ResetPasswordRequest(
            mobile = mobile,
            newPassword = newPassword,
            verificationCode = verificationCode,
        )
    )

    fun getUserDatabaseAbsolutePath(name: String) =
        "${context.getDatabasePath(currentUserId.toString()).absolutePath}/$name"
}
