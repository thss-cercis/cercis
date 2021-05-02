package cn.edu.tsinghua.thss.cercis.repository

import androidx.lifecycle.LiveData
import cn.edu.tsinghua.thss.cercis.entity.User
import cn.edu.tsinghua.thss.cercis.dao.UserDao
import cn.edu.tsinghua.thss.cercis.entity.LoginHistory
import cn.edu.tsinghua.thss.cercis.entity.UserDetail
import cn.edu.tsinghua.thss.cercis.http.AuthenticationData
import cn.edu.tsinghua.thss.cercis.http.CercisHttpService
import cn.edu.tsinghua.thss.cercis.util.NetworkBoundResource
import cn.edu.tsinghua.thss.cercis.util.NetworkResponse
import cn.edu.tsinghua.thss.cercis.util.Resource
import cn.edu.tsinghua.thss.cercis.util.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@FlowPreview
@ExperimentalCoroutinesApi
@Singleton
class UserRepository @Inject constructor(
    val httpService: CercisHttpService,
    val authenticationData: AuthenticationData,
    val userDao: UserDao,
) {
    private val userLiveDataCache = HashMap<UserId, Resource<User>>()

    val loginHistory = userDao.loadAllLoginHistory()

    /**
     * Looks up a user with intermediate cache.
     */
    fun loadUser(userId: UserId): LiveData<Resource<User>> {
        TODO()
    }

    /**
     * Gets the current user.
     */
    fun userDetail() = object : NetworkBoundResource<UserDetail, UserDetail>() {
        override suspend fun saveNetworkResult(item: UserDetail) {
            userDao.insertLoginHistory(LoginHistory(userId = item.id, mobile = item.mobile))
            userDao.saveUserDetail(item)
        }

        override fun shouldFetch(data: UserDetail?): Boolean = true

        override fun loadFromDb(): Flow<UserDetail> {
            return userDao.loadUserDetail(authenticationData.userId.value!!)
        }

        override suspend fun fetchFromNetwork(): NetworkResponse<UserDetail> {
            // TODO handle user_id inconsistency, or just ignore it
            return httpService.userDetail()
        }
    }
}
