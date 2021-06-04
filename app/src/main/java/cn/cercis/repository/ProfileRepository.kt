package cn.cercis.repository

import android.net.Uri
import cn.cercis.dao.LoginHistoryDao
import cn.cercis.dao.UserDao
import cn.cercis.entity.LoginHistory
import cn.cercis.entity.UserDetail
import cn.cercis.http.CercisHttpService
import cn.cercis.http.EmptyNetworkResponse
import cn.cercis.http.UpdateUserDetailRequest
import cn.cercis.util.helper.FileUploadUtils
import cn.cercis.util.livedata.AutoResetLiveData
import cn.cercis.util.resource.DataSource
import cn.cercis.util.resource.NetworkResponse
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


@FlowPreview
@ExperimentalCoroutinesApi
@ActivityRetainedScoped
class ProfileRepository @Inject constructor(
    private val httpService: CercisHttpService,
    private val userDao: UserDao,
    private val loginHistoryDao: LoginHistoryDao,
) {
    val profileChanged = AutoResetLiveData(false)

    fun getCurrentUserDetail() = object : DataSource<UserDetail>() {
        override suspend fun fetch(): NetworkResponse<UserDetail> {
            // TODO handle user_id inconsistency, or just ignore it
            return httpService.getUserDetail()
        }

        override suspend fun saveToDb(data: UserDetail) {
            loginHistoryDao.saveLoginHistory(LoginHistory(userId = data.id, mobile = data.mobile))
            userDao.saveUserDetail(data)
        }

        override fun loadFromDb(): Flow<UserDetail?> {
            return userDao.loadUserDetail()
        }
    }

    suspend fun updateCurrentUserDetail(
        nickname: String? = null,
        avatar: String? = null,
        bio: String? = null,
        email: String? = null,
    ): EmptyNetworkResponse {
        val request = UpdateUserDetailRequest(
            nickname = nickname,
            email = email,
            avatar = avatar,
            bio = bio,
        )
        return httpService.updateUserDetail(request)
    }
}
