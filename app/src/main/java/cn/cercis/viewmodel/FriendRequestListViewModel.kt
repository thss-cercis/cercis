package cn.cercis.viewmodel

import android.util.Log
import androidx.annotation.MainThread
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
import cn.cercis.util.*
import cn.cercis.viewmodel.FriendRequestListViewModel.RecyclerData.Companion.DELIMITER_0
import cn.cercis.viewmodel.FriendRequestListViewModel.RecyclerData.Companion.DELIMITER_1
import cn.cercis.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class FriendRequestListViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    sealed class RecyclerData(val id: ApplyId, val type: Int) {
        data class FriendRequestWithUpdateMark(
            val applyId: ApplyId,
            val fromId: UserId,
            val toId: UserId,
            val state: Int,
            val requestedDisplayName: String,
            val remark: String,
            val updateMark: Int,
            val createdAt: Timestamp,
            val loading: SourceReplaceableLiveData<Boolean>,
        ) : RecyclerData(applyId, TYPE_DATA)

        data class Delimiter(val delimiterId: Long) : RecyclerData(delimiterId, TYPE_DELIMITER)

        companion object {
            const val DELIMITER_0 = -1L
            const val DELIMITER_1 = -2L
            const val TYPE_DATA = 0
            const val TYPE_DELIMITER = 1
        }
    }

    private val atomicInteger = AtomicInteger(0)
    private val users =
        HashMap<UserId, LiveData<User>>()
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
//                MutableLiveData(Resource.Success((0L..10L).map {
//                    FriendRequest(
//                        applyId = it,
//                        fromId = it,
//                        toId = it,
//                        state = 0,
//                        displayName = "$it",
//                        remark = "$it",
//                        createdAt = it,
//                    )
//                }) as Resource<List<FriendRequest>>).let { newSource ->
//                    source = newSource
//                    liveData.addSource(newSource) {
//                        liveData.value = it
//                    }
//                }
                // * replace the code above with the following code to enable real data
                source = friendRepository.getFriendRequestReceivedList().asFlow()
                    .asLiveData(viewModelScope.coroutineContext + Dispatchers.IO).apply {
                        liveData.addSource(this) {
                            liveData.value = it
                        }
                    }
                // ********
            }

            val liveData = liveData
        }.apply {
            refresh()
        }
    }
    val requests: LiveData<List<RecyclerData>> by lazy {
        // enable force fresh
        Transformations.map(requestSource.liveData) { resource ->
            val updateMark = atomicInteger.get()
            Log.d(LOG_TAG, "updated with mark $updateMark")
            resource?.data?.let { list ->
                val (pending, finished) =
                    list.map { friendEntry ->
                        RecyclerData.FriendRequestWithUpdateMark(
                            applyId = friendEntry.applyId,
                            fromId = friendEntry.fromId,
                            toId = friendEntry.toId,
                            state = friendEntry.state,
                            requestedDisplayName = friendEntry.displayName ?: "",
                            remark = friendEntry.remark,
                            createdAt = friendEntry.createdAt,
                            updateMark = updateMark,
                            loading = SourceReplaceableLiveData<Boolean>().apply { value = false },
                        )
                    }.sortedByDescending {
                        it.createdAt
                    }.partition { it.state == STATE_PENDING }
                return@let listOf(RecyclerData.Delimiter(DELIMITER_0)) + pending +
                        listOf(RecyclerData.Delimiter(DELIMITER_1)) + finished
            } ?: listOf()
        }
    }
    val requestListLoading by lazy { Transformations.map(requestSource.liveData) { it is Resource.Loading } }
    val errorMessage = MutableLiveData<Pair<RecyclerData.FriendRequestWithUpdateMark, NetworkResponse<Any>>>(null)

    @MainThread
    fun acceptRequest(friendRequest: RecyclerData.FriendRequestWithUpdateMark) {
        if (friendRequest.loading.value == false) {
            friendRequest.loading.value = true
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    errorMessage.postValue(friendRequest to friendRepository.acceptFriendRequest(
                        friendRequest.applyId))
                } finally {
                    friendRequest.loading.postValue(false)
                }
            }
        }
    }

    fun getUserInfo(userId: UserId): LiveData<User> {
        // TODO replace with real data
        return users.computeIfAbsent(userId) {
            // ********
//            MutableLiveData(User(
//                id = userId,
//                nickname = "$userId",
//                mobile = "12345$userId",
//                avatar = "",
//                bio = "${System.currentTimeMillis()}",
//                chatId = 0,
//                updated = 0,
//            ))
            // * replace the code above with the following code to enable real data
            Transformations.map(userRepository.getUser(userId).asFlow().asLiveData(
                viewModelScope.coroutineContext + Dispatchers.IO
            )) { user ->
                user?.data
            }
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