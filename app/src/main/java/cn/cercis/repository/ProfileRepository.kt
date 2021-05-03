package cn.cercis.repository

import cn.cercis.dao.LoginHistoryDao
import cn.cercis.dao.UserDao
import cn.cercis.entity.LoginHistory
import cn.cercis.entity.UserDetail
import cn.cercis.http.CercisHttpService
import cn.cercis.http.EmptyNetworkResponse
import cn.cercis.http.NetworkBoundResource
import cn.cercis.util.ChatId
import cn.cercis.util.NetworkResponse
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@ActivityRetainedScoped
class ProfileRepository @Inject constructor(
    val httpService: CercisHttpService,
    val userDao: UserDao,
    val loginHistoryDao: LoginHistoryDao,
) {
    fun getCurrentUserDetail() = object : NetworkBoundResource<UserDetail, UserDetail>() {
        override suspend fun saveNetworkResult(item: UserDetail) {
            loginHistoryDao.insertLoginHistory(LoginHistory(userId = item.id, mobile = item.mobile))
            userDao.saveUserDetail(item)
        }

        override fun shouldFetch(data: UserDetail?) = true

        override fun loadFromDb() = userDao.loadUserDetail()

        override suspend fun fetchFromNetwork(): NetworkResponse<UserDetail> {
            // TODO handle user_id inconsistency, or just ignore it
            return httpService.getUserDetail()
        }
    }

    fun updateCurrentUserDetail(
        nickname: String? = null,
        avatar: String? = null,
        bio: String? = null,
        email: String? = null,
    ): EmptyNetworkResponse {
        TODO("httpApi")
    }
}
