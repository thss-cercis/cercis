package cn.cercis.repository

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import cn.cercis.common.LOG_TAG
import cn.cercis.common.SEARCH_PAGE_SIZE
import cn.cercis.common.UserId
import cn.cercis.common.mapRun
import cn.cercis.dao.FriendDao
import cn.cercis.dao.UserDao
import cn.cercis.entity.FriendEntry
import cn.cercis.entity.User
import cn.cercis.http.CercisHttpService
import cn.cercis.http.WrappedSearchUserPayload.UserSearchResult
import cn.cercis.util.resource.DataSource
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.viewmodel.CommonListItemData
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@ActivityRetainedScoped
class UserRepository @Inject constructor(
    val httpService: CercisHttpService,
    private val userDao: UserDao,
    private val friendDao: FriendDao,
) {
    fun getUser(userId: UserId) = object : DataSource<User>() {
        override suspend fun fetch(): NetworkResponse<User> {
            return httpService.getUser(userId).use { user }.use {
                User(
                    id = userId,
                    nickname = nickname,
                    mobile = mobile,
                    avatar = avatar, // TODO
                    bio = bio,
                    updated = System.currentTimeMillis(),
                )
            }
        }

        override suspend fun saveToDb(data: User) {
            userDao.saveUser(data)
        }

        override fun loadFromDb(): Flow<User?> {
            return userDao.loadUser(userId)
        }
    }

    private suspend fun getUsersAndSave(
        userIds: List<UserId>,
        loadOnlyMissingUsers: Boolean = true,
    ) {
        if (loadOnlyMissingUsers) {
            userDao.loadUsersOnce(userIds).let {
                val set = HashSet(it.mapRun { id })
                getUsersAndSave(userIds.filterNot { userId -> userId in set })
            }
        } else {
            for (userId in userIds) {
                getUser(userId).fetchAndSave()
            }
        }
    }

    /**
     * Combines list with users.
     */
    fun <T> CoroutineScope.withUsers(
        input: Flow<List<T>>,
        toUserId: T.() -> UserId,
        loadOnlyMissingUsers: Boolean = true,
    ): Flow<List<Pair<T, User?>>> {
        return input.flatMapLatest { inputList ->
            val userIdList = inputList.map(toUserId)
            launch(Dispatchers.IO) { getUsersAndSave(userIdList, loadOnlyMissingUsers) }
            userDao.loadUsers(userIdList).map { userList ->
                val userSet = HashMap<UserId, User>().apply { userList.forEach { put(it.id, it) } }
                inputList.mapRun {
                    toUserId().let {
                        this to userSet[it]
                    }
                }
            }
        }
    }

    /**
     * Combines list with friend entry.
     *
     * @param loadOnlyMissingUsers if true, only uncached users are downloaded
     */
    fun <T> CoroutineScope.withFriends(
        input: Flow<List<T>>,
        toUserId: T.() -> UserId,
        loadOnlyMissingUsers: Boolean = true,
    ): Flow<List<Triple<T, User?, FriendEntry?>>> {
        return input.flatMapLatest { inputList ->
            val userIdList = inputList.map(toUserId)
            launch(Dispatchers.IO) { getUsersAndSave(userIdList, loadOnlyMissingUsers) }
            userDao.loadUsers(userIdList)
                .combine(friendDao.loadFriendList()) { userList, friendList ->
                    val userSet = HashMap<UserId, User>().apply {
                        userList.forEach { put(it.id, it) }
                    }
                    val friendSet = HashMap<UserId, FriendEntry>().apply {
                        friendList.forEach { put(it.friendUserId, it) }
                    }
                    inputList.mapRun {
                        toUserId().let {
                            Triple(this, userSet[it], friendSet[it])
                        }
                    }
                }
        }
    }

    /**
     * Gets a user's display with info from friend entries.
     *
     * * NOTE: this method will not trigger friend list loading.
     *
     * @return emits null on user loading failure
     */
    fun getUserWithFriendDisplay(
        userId: UserId,
        fetchUser: Boolean,
    ): Flow<CommonListItemData> =
        getUser(userId).let { if (fetchUser) it.flow().map { res -> res.data } else it.dbFlow() }
            .filterNotNull()
            .flatMapLatest { user ->
                friendDao.loadFriendEntry(user.id).map { entry ->
                    CommonListItemData(
                        avatar = user.avatar,
                        displayName = entry?.displayName.let { if (it.isNullOrEmpty()) user.nickname else it },
                        description = user.bio,
                    )
                }
            }


    suspend fun searchUser(
        userId: UserId? = null,
        mobile: String? = null,
        nickname: String? = null,
        offset: Long? = null,
        limit: Long? = null,
    ): NetworkResponse<List<UserSearchResult>> {
        return httpService.searchUser(
            userId = userId,
            mobile = mobile,
            nickname = nickname,
            offset = offset,
            limit = limit,
        ).use { users }
    }

    fun searchUserPagingSource(
        id: UserId? = null,
        mobile: String? = null,
        nickname: String? = null,
    ) = object : PagingSource<Long, UserSearchResult>() {
        override fun getRefreshKey(state: PagingState<Long, UserSearchResult>): Long? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            }
        }

        override suspend fun load(params: LoadParams<Long>): LoadResult<Long, UserSearchResult> {
            Log.d(LOG_TAG, "Loading more pages")
            val nextPageNumber = params.key ?: 0
            val response = searchUser(
                id, mobile, nickname,
                offset = nextPageNumber * SEARCH_PAGE_SIZE,
                limit = SEARCH_PAGE_SIZE.toLong(),
            )
            return when (response) {
                is NetworkResponse.Success -> LoadResult.Page(
                    data = response.data,
                    prevKey = null,
                    nextKey = if (response.data.size == SEARCH_PAGE_SIZE) {
                        nextPageNumber + 1L
                    } else {
                        null
                    }
                )
                else -> LoadResult.Error(Exception(response.message))
            }
        }
    }
}
