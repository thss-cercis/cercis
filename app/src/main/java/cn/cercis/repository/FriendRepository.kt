package cn.cercis.repository

import cn.cercis.common.ApplyId
import cn.cercis.dao.FriendDao
import cn.cercis.entity.FriendEntry
import cn.cercis.entity.FriendRequest
import cn.cercis.http.*
import cn.cercis.util.NetworkResponse
import cn.cercis.util.mapRun
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@ActivityRetainedScoped
@FlowPreview
@ExperimentalCoroutinesApi
class FriendRepository @Inject constructor(
    private val httpService: CercisHttpService,
    private val authRepository: AuthRepository,
    private val friendDao: FriendDao,
) {
    fun getFriendList() = object : NetworkBoundResource<List<FriendEntry>>() {
        override suspend fun fetchFromNetwork(): NetworkResponse<List<FriendEntry>> {
            return httpService.getFriendList().use { friends }.convert {
                it.mapRun {
                    FriendEntry(friendUserId = id, remark = "", displayName = displayName)
                }
            }
        }

        override suspend fun saveNetworkResult(data: List<FriendEntry>) {
            friendDao.saveFriendList(data)
        }

        override fun loadFromDb(): Flow<List<FriendEntry>?> {
            return friendDao.loadFriendList()
        }
    }

    fun getFriendRequestSentList() = object : NetworkBoundResource<List<FriendRequest>>() {
        override suspend fun saveNetworkResult(data: List<FriendRequest>) {
            friendDao.saveFriendRequestList(data)
        }

        override suspend fun fetchFromNetwork(): NetworkResponse<List<FriendRequest>> {
            return httpService.getFriendRequestSentList().use { requests }
        }

        override fun loadFromDb(): Flow<List<FriendRequest>?> {
            return friendDao.loadFriendRequestSentList(authRepository.currentUserId)
        }
    }

    fun getFriendRequestReceivedList() = object : NetworkBoundResource<List<FriendRequest>>() {
        override suspend fun saveNetworkResult(data: List<FriendRequest>) {
            friendDao.saveFriendRequestList(data)
        }

        override suspend fun fetchFromNetwork(): NetworkResponse<List<FriendRequest>> {
            return httpService.getFriendRequestReceivedList().use { requests }
        }

        override fun loadFromDb(): Flow<List<FriendRequest>?> {
            return friendDao.loadFriendRequestReceivedList(authRepository.currentUserId)
        }
    }

    suspend fun acceptFriendRequest(
        applyId: ApplyId,
        displayName: String? = null,
    ): EmptyNetworkResponse {
        return httpService.acceptAddingFriend(
            AcceptAddingFriendRequest(applyId = applyId, displayName = displayName)
        )
    }

    suspend fun rejectFriendRequest(
        applyId: ApplyId,
    ): EmptyNetworkResponse {
        return httpService.rejectAddingFriend(
            RejectAddingFriendRequest(applyId = applyId)
        )
    }
}
