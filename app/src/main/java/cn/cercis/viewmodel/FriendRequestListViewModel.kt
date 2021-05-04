package cn.cercis.viewmodel

import android.util.Log
import androidx.lifecycle.*
import cn.cercis.common.ApplyId
import cn.cercis.common.LOG_TAG
import cn.cercis.common.Timestamp
import cn.cercis.common.UserId
import cn.cercis.entity.FriendRequest
import cn.cercis.entity.FriendRequest.Companion.STATE_PENDING
import cn.cercis.entity.User
import cn.cercis.repository.FriendRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.Resource
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@FlowPreview
@ActivityRetainedScoped
@ExperimentalCoroutinesApi
class FriendRequestListViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    data class FriendRequestWithUpdateMark(
        val applyId: ApplyId,
        val fromId: UserId,
        val toId: UserId,
        val state: Int,
        val requestedDisplayName: String,
        val remark: String,
        val updateMark: Int,
        val createdAt: Timestamp,
    )

    private val atomicInteger = AtomicInteger(0)
    private val users = HashMap<UserId, LiveData<Pair<User, FriendRequestWithUpdateMark>>>()
    private val requestSource by lazy {
        val liveData = MediatorLiveData<Resource<List<FriendRequest>>>()
        var source: LiveData<Resource<List<FriendRequest>>>? = null
        object {
            fun refresh() {
                source?.let {
                    liveData.removeSource(it)
                }

                // TODO replace with real data
                // ********
                MutableLiveData(Resource.Success((0L..10L).map {
                    FriendRequest(
                        applyId = it,
                        fromId = it,
                        toId = it,
                        state = 0,
                        displayName = "$it",
                        remark = "$it",
                        createdAt = it,
                    )
                }) as Resource<List<FriendRequest>>).let { newSource ->
                    source = newSource
                    liveData.addSource(newSource) {
                        liveData.value = it
                    }
                }
                // * replace the code above with the following code to enable real data
//                source = friendRepository.getFriendRequestList().asFlow()
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
    val requests: LiveData<List<FriendRequestWithUpdateMark>> by lazy {
        // enable force fresh
        Transformations.map(requestSource.liveData) {
            val updateMark = atomicInteger.get()
            Log.d(LOG_TAG, "updated with mark $updateMark")
            it?.data?.map { friendEntry ->
                FriendRequestWithUpdateMark(
                    applyId = friendEntry.applyId,
                    fromId = friendEntry.fromId,
                    toId = friendEntry.toId,
                    state = friendEntry.state,
                    requestedDisplayName = friendEntry.displayName!!,
                    remark = friendEntry.remark,
                    createdAt = friendEntry.createdAt,
                    updateMark = updateMark,
                )
            } ?: listOf()
        }
    }
    val finishedRequests: LiveData<List<FriendRequestWithUpdateMark>> by lazy {
        Transformations.map(requests) { list ->
            list?.filter { it.state != STATE_PENDING } ?: listOf()
        }
    }
    val pendingRequests: LiveData<List<FriendRequestWithUpdateMark>> by lazy {
        Transformations.map(requests) { list ->
            list?.filter { it.state == STATE_PENDING } ?: listOf()
        }
    }
    val requestListLoading by lazy { Transformations.map(requestSource.liveData) { it is Resource.Loading } }

    fun getUserInfo(
        userId: UserId,
        requestEntry: FriendRequestWithUpdateMark,
    ): LiveData<Pair<User, FriendRequestWithUpdateMark>> {
        // TODO replace with real data
        return users.computeIfAbsent(userId) {
            // ********
            MutableLiveData(User(
                id = userId,
                nickname = "$userId",
                mobile = "12345$userId",
                avatar = "",
                bio = "${System.currentTimeMillis()}",
                chatId = 0,
                updated = 0,
            ) to requestEntry)
            // * replace the code above with the following code to enable real data
//            Transformations.map(userRepository.getUser(userId).asFlow().asLiveData(
//                viewModelScope.coroutineContext + Dispatchers.IO
//            )) { user ->
//                user?.data?.let {
//                    it to requestEntry
//                }
//            }
            // ********
        }
    }

    /**
     * Refreshes users in the user list
     */
    fun refreshRequestList() {
        // the following steps should not be changed
        // add updateMark by 1 to fail all caches
        atomicInteger.incrementAndGet()
        // clear users to enforce re-fetch
        users.clear()
        // trigger friendList reload
        requestSource.refresh()
    }
}