package cn.cercis.repository

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import cn.cercis.common.LOG_TAG
import cn.cercis.common.SEARCH_PAGE_SIZE
import cn.cercis.common.UserId
import cn.cercis.dao.FriendDao
import cn.cercis.dao.UserDao
import cn.cercis.entity.User
import cn.cercis.http.CercisHttpService
import cn.cercis.http.WrappedSearchUserPayload.UserSearchResult
import cn.cercis.util.resource.DataSource
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.viewmodel.CommonListItemData
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
