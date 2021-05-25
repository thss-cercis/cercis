package cn.cercis.repository

import cn.cercis.common.ApplyId
import cn.cercis.common.UserId
import cn.cercis.common.mapRun
import cn.cercis.dao.FriendDao
import cn.cercis.dao.UserDao
import cn.cercis.entity.FriendEntry
import cn.cercis.entity.FriendRequest
import cn.cercis.entity.FriendUser
import cn.cercis.entity.User
import cn.cercis.http.*
import cn.cercis.util.resource.DataSource
import cn.cercis.util.resource.DataSourceBase
import cn.cercis.util.resource.NetworkResponse
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
    private val userDao: UserDao,
) {
    fun getFriendList() = object : DataSource<List<FriendEntry>>() {
        override suspend fun fetch(): NetworkResponse<List<FriendEntry>> {
            return httpService.getFriendList().use { friends }.convert {
                it.mapRun {
                    FriendEntry(friendUserId = id, displayName = displayName)
                }
            }
        }

        override suspend fun saveToDb(data: List<FriendEntry>) {
            friendDao.replaceFriendList(data)
        }

        override fun loadFromDb(): Flow<List<FriendEntry>?> {
            return friendDao.loadFriendList()
        }
    }

    fun getFriendUserList() = object: DataSourceBase<List<FriendUser>, List<Pair<FriendEntry, User?>>>() {
        override suspend fun fetch(): NetworkResponse<List<Pair<FriendEntry, User?>>> {
            return httpService.getFriendList().use { friends }.convert {
                it.mapRun {
                    Pair(FriendEntry(friendUserId = id, displayName = displayName), null)
                }
            }
        }

        override suspend fun saveToDb(data: List<Pair<FriendEntry, User?>>) {
            friendDao.replaceFriendList(data.map { it.first })
            userDao.saveUserList(data.mapNotNull { it.second })
        }

        override fun loadFromDb(): Flow<List<FriendUser>> {
            return friendDao.loadFriendDisplayList()
        }
    }

    suspend fun sendFriendRequest(
        id: UserId,
        remark: String? = null,
        displayName: String? = null,
    ) = httpService.addFriend(
        AddFriendRequest(
            id = id,
            remark = remark,
            displayName = displayName,
        )
    )

    fun getFriendRequestSentList() = object : DataSource<List<FriendRequest>>() {
        override suspend fun saveToDb(data: List<FriendRequest>) {
            friendDao.saveFriendRequestList(data)
        }

        override suspend fun fetch(): NetworkResponse<List<FriendRequest>> {
            return httpService.getFriendRequestSentList().use { requests }
        }

        override fun loadFromDb(): Flow<List<FriendRequest>?> {
            return friendDao.loadFriendRequestSentList(authRepository.currentUserId)
        }
    }

    fun getFriendRequestReceivedList() = object : DataSource<List<FriendRequest>>() {
        override suspend fun saveToDb(data: List<FriendRequest>) {
            friendDao.saveFriendRequestList(data)
        }

        override suspend fun fetch(): NetworkResponse<List<FriendRequest>> {
            return httpService.getFriendRequestReceivedList().use { requests }
        }

        override fun loadFromDb(): Flow<List<FriendRequest>?> {
            return friendDao.loadFriendRequestReceivedList(authRepository.currentUserId)
        }
    }

    suspend fun acceptFriendRequest(
        applyId: ApplyId,
        displayName: String? = null,
    ) = httpService.acceptAddingFriend(
        AcceptAddingFriendRequest(
            applyId = applyId,
            displayName = displayName
        )
    )

    suspend fun rejectFriendRequest(applyId: ApplyId) =
        httpService.rejectAddingFriend(RejectAddingFriendRequest(applyId))

    suspend fun updateFriendRemark(
        id: UserId,
        remark: String? = null,
        displayName: String? = null,
    ) =
        httpService.updateFriendRemark(
            UpdateFriendRemarkRequest(
                id = id,
                remark = remark,
                displayName = displayName,
            )
        )

    suspend fun deleteFriend(id: UserId) =
        httpService.deleteFriend(DeleteFriendRequest(id))
}
