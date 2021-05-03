package cn.cercis.repository

import cn.cercis.dao.LoginHistoryDao
import cn.cercis.dao.UserDao
import cn.cercis.entity.LoginHistory
import cn.cercis.entity.User
import cn.cercis.entity.UserDetail
import cn.cercis.http.CercisHttpService
import cn.cercis.http.NetworkBoundResource
import cn.cercis.http.UserProfileResponse
import cn.cercis.http.UserWrapper
import cn.cercis.util.NetworkResponse
import cn.cercis.util.UserId
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@ActivityRetainedScoped
class UserRepository @Inject constructor(
    val httpService: CercisHttpService,
    val userDao: UserDao,
    val loginHistoryDao: LoginHistoryDao,
) {
    fun getUser(userId: UserId): NetworkBoundResource<User, UserWrapper> {
        TODO("httpApi")
    }
}
