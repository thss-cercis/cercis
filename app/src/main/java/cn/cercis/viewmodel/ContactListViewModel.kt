package cn.cercis.viewmodel

import android.util.Log
import androidx.lifecycle.*
import cn.cercis.entity.FriendEntry
import cn.cercis.entity.User
import cn.cercis.repository.FriendRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.CommonId
import cn.cercis.util.LOG_TAG
import cn.cercis.util.Resource
import cn.cercis.util.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@FlowPreview
@HiltViewModel
@ExperimentalCoroutinesApi
class ContactListViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    data class FriendEntryWithUpdateMark(
        val id: CommonId,
        val friendUserId: UserId,
        val remarks: String,
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
        val remarks: String,
        val alias: String,
    ) {
        constructor(user: User, friendEntry: FriendEntryWithUpdateMark) : this(
            id = user.id,
            nickname = user.nickname,
            mobile = user.mobile,
            avatar = user.avatar,
            bio = user.bio,
            remarks = friendEntry.remarks,
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

                // TODO replace with real data
                // ********
                MutableLiveData(Resource.Success((0L..10L).map {
                    FriendEntry(
                        id = it,
                        friendUserId = it,
                        remark = "",
                        displayName = "",
                    )
                }) as Resource<List<FriendEntry>>).let { newSource ->
                    source = newSource
                    liveData.addSource(newSource) {
                        liveData.value = it
                    }
                }
                // * replace the code above with the following code to enable real data
//                source = friendRepository.getFriendList().asFlow()
//                    .asLiveData(viewModelScope.coroutineContext + Dispatchers.IO).apply {
//                        liveData.addSource(this) {
//                            liveData.value = it
//                        }
//                    }
                // ********
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
                    id = friendEntry.id,
                    friendUserId = friendEntry.friendUserId,
                    alias = friendEntry.displayName,
                    remarks = friendEntry.remark,
                    updateMark = updateMark,
                )
            } ?: listOf()
        }
    }
    val friendListLoading by lazy { Transformations.map(friendListSource.liveData) { it is Resource.Loading } }

    /**
     * Gets a user's info, with cached [cn.cercis.http.NetworkBoundResource] object, to prevent
     * redundant GETs.
     */
    fun getUserInfo(userId: UserId, friendEntry: FriendEntryWithUpdateMark): LiveData<Friend> {
        // TODO replace with real data
        return users.computeIfAbsent(userId) {
            // ********
            MutableLiveData(Friend(
                id = userId,
                nickname = "$userId",
                mobile = "12345$userId",
                avatar = "",
                bio = "${System.currentTimeMillis()}",
                alias = "alias$userId",
                remarks = "",
            ))
            // * replace the code above with the following code to enable real data
//            Transformations.map(userRepository.getUser(userId).asFlow().asLiveData(
//                viewModelScope.coroutineContext + Dispatchers.IO
//            )) { user ->
//                user?.data?.let {
//                    Friend(it, friendEntry)
//                }
//            }
            // ********
        }
    }

    /**
     * Refreshes users in the user list
     */
    fun refreshFriendList() {
        // the following steps should not be changed
        // add updateMark by 1 to fail all caches
        atomicInteger.incrementAndGet()
        // clear users to enforce re-fetch
        users.clear()
        // trigger friendList reload
        friendListSource.refresh()
    }
}