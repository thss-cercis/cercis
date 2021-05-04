package cn.cercis.repository

import cn.cercis.common.NO_USER
import cn.cercis.common.UserId
import cn.cercis.dao.UserDao
import cn.cercis.entity.User
import cn.cercis.http.CercisHttpService
import cn.cercis.http.NetworkBoundResource
import cn.cercis.util.NetworkResponse
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@ActivityRetainedScoped
class UserRepository @Inject constructor(
    val httpService: CercisHttpService,
    val userDao: UserDao,
) {
    fun getUser(userId: UserId) = object : NetworkBoundResource<User>() {
        override suspend fun fetchFromNetwork(): NetworkResponse<User> {
            return httpService.getUser(userId).use { user }.use {
                User(
                    id = userId,
                    nickname = nickname,
                    mobile = mobile,
                    chatId = NO_USER, // TODO
                    avatar = avatar,
                    bio = bio,
                    updated = System.currentTimeMillis(),
                )
            }
        }

        override suspend fun saveNetworkResult(data: User) {
            userDao.saveUser(data)
        }

        override fun loadFromDb(): Flow<User?> {
            return userDao.loadUser(userId)
        }
    }
}
