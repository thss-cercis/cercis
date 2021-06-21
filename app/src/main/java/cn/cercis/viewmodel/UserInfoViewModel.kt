package cn.cercis.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.*
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.common.UserId
import cn.cercis.entity.Chat
import cn.cercis.entity.FriendEntry
import cn.cercis.entity.User
import cn.cercis.http.EmptyNetworkResponse
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.FriendRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.getString
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.asInitializedLiveData
import cn.cercis.util.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class UserInfoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
) : ViewModel() {
    private val userId = savedStateHandle.get<UserId>("userId")!!
    private val initialUserInfo = savedStateHandle.get<User>("user")
    private val userInfoFlow = userRepository.getUser(userId).flow()
    val userInfo = userInfoFlow.map { it.data }
        .asInitializedLiveData(coroutineContext, initialUserInfo)
    val busyGettingChat = MutableLiveData(false)
    val friendEntry: LiveData<FriendEntry?> = friendRepository.getFriendList().flow().map {
        it.data
    }.filterNotNull().map {
        it.firstOrNull { friendEntry -> friendEntry.friendUserId == userId }
            .apply {
                Log.d(this@UserInfoViewModel.LOG_TAG, "$userId is friend?: $this")
            }
    }.asLiveData(coroutineContext)
    val isFriend: LiveData<Boolean?> = friendEntry.map { it != null }
    val showIfFriend = isFriend.map { if (it == true) View.VISIBLE else View.GONE }
    val showIfNotFriend = isFriend.map { if (it == false) View.VISIBLE else View.GONE }

    suspend fun getChat(): Resource<Chat> {
        busyGettingChat.postValue(true)
        try {
            return withTimeoutOrNull(5000) {
                messageRepository.getPrivateChatWith(authRepository.currentUserId, userId)
                    .fallbackFlow()
                    .first() // only takes first result
                    .apply { Log.d(LOG_TAG, "getChat with $userId: $this") }
            } ?: Resource.Error(-1, getString(R.string.error_server_error), null)
        } finally {
            busyGettingChat.postValue(false)
        }
    }

    suspend fun deleteFriend(): EmptyNetworkResponse {
        return friendRepository.deleteFriend(userId).apply {
            friendRepository.getFriendListAndSave()
        }
    }

    suspend fun editFriendDisplayName(displayName: String): EmptyNetworkResponse {
        return friendRepository.editFriendDisplayName(userId, displayName).apply {
            friendRepository.getFriendListAndSave()
        }
    }

    suspend fun sendFriendApply(displayName: String?, remark: String?): EmptyNetworkResponse {
        return friendRepository.sendFriendRequest(userId, remark, displayName)
    }
}
