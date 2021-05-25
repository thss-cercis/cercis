package cn.cercis.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import cn.cercis.common.LOG_TAG
import cn.cercis.common.UserId
import cn.cercis.entity.FriendEntry
import cn.cercis.entity.User
import cn.cercis.repository.FriendRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.addSource
import cn.cercis.util.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ContactListViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    data class FriendEntryWithUpdateMark(
        val friendUserId: UserId,
        val alias: String,
        val updateMark: Int,
    )

    data class Friend(
        // user
        val id: UserId,
        val nickname: String,
        val mobile: String,
        val avatar: String,
        val bio: String,
        // friend entry
        val alias: String,
    ) {
        constructor(user: User, friendEntry: FriendEntryWithUpdateMark) : this(
            id = user.id,
            nickname = user.nickname,
            mobile = user.mobile,
            avatar = user.avatar,
            bio = user.bio,
            alias = friendEntry.alias,
        )

        fun displayName(): String {
            return if (alias.isEmpty()) {
                nickname
            } else {
                alias
            }
        }
    }

    private val atomicInteger = AtomicInteger(0)
    private val users = HashMap<UserId, LiveData<Friend>>()
    private val friendListSource by lazy {
        val liveData = MediatorLiveData<Resource<List<FriendEntry>>>()
        var source: LiveData<Resource<List<FriendEntry>>>? = null
        object {
            fun refresh() {
                source?.let {
                    liveData.removeSource(it)
                }
                source = friendRepository.getFriendList()
                    .asLiveData(coroutineContext).also { liveData.addSource(it) }
            }
            val liveData = liveData
        }.apply {
            refresh()
        }
    }
    val friendList: LiveData<List<FriendEntryWithUpdateMark>> by lazy {
        // enable force fresh
        Transformations.map(friendListSource.liveData) {
            val updateMark = atomicInteger.get()
            Log.d(LOG_TAG, "updated with mark $updateMark")
            it?.data?.map { friendEntry ->
                FriendEntryWithUpdateMark(
                    friendUserId = friendEntry.friendUserId,
                    alias = friendEntry.displayName,
                    updateMark = updateMark,
                )
            } ?: listOf()
        }
    }
    val friendListLoading by lazy { Transformations.map(friendListSource.liveData) { it is Resource.Loading } }

    /**
     * Gets a user's info, with cached [cn.cercis.util.resource.DataSource] object, to prevent
     * redundant GETs.
     */
    fun getUserInfo(userId: UserId, friendEntry: FriendEntryWithUpdateMark): LiveData<Friend> {
        return users.computeIfAbsent(userId) {
            Log.d(LOG_TAG, "loading user $userId")
            Transformations.map(
                userRepository.getUser(userId).asLiveData(coroutineContext)
            ) { user ->
                user?.data?.let {
                    Friend(it, friendEntry)
                }
            }
        }
    }

    /**
     * Refreshes users in the user list
     */
    fun refreshFriendList() {
        // the following steps should not be reordered
        // add updateMark by 1 to fail all caches
        atomicInteger.incrementAndGet()
        // clear users to enforce re-fetch
        users.clear()
        // trigger friendList reload
        friendListSource.refresh()
    }
}
