package cn.cercis.viewmodel

import androidx.lifecycle.*
import cn.cercis.common.ChatId
import cn.cercis.common.UserId
import cn.cercis.http.EmptyNetworkResponse
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.FriendRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.AutoResetLiveData
import cn.cercis.util.livedata.asInitializedLiveData
import cn.cercis.util.livedata.generateMediatorLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class RemoveGroupMemberViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    friendRepository: FriendRepository,
    authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
) : ViewModel() {
    val chatId = savedStateHandle.get<ChatId>("chatId")!!
    val currentUserId = authRepository.currentUserId
    private val selectedUsers = MutableLiveData<HashSet<UserId>>(HashSet())
    private val busyLoading = MutableLiveData(false)
    private val groupMemberListFlow = userRepository.run {
        viewModelScope.withFriends(messageRepository.getChatMemberList(chatId).flow()
            .map { it.data }.filterNotNull().flowOn(Dispatchers.IO), { userId }, false)
    }
    private val groupMemberList =
        groupMemberListFlow.asInitializedLiveData(coroutineContext, listOf())
    private val selfMember =
        groupMemberList.map { it.firstOrNull { member -> member.first.userId == currentUserId } }
    val groupMemberListSource =
        generateMediatorLiveData(groupMemberList, selectedUsers, selfMember) {
            groupMemberList.value!!.map { triple ->
                Triple(
                    triple,
                    selectedUsers.value!!.contains(triple.first.userId),
                    selfMember.value?.let { triple.first.permission < it.first.permission } ?: true
                )
            }
        }
    val selectedUserList = generateMediatorLiveData(groupMemberListSource, selectedUsers) {
        groupMemberListSource.value!!.filter { selectedUsers.value!!.contains(it.first.first.userId) }
    }
    val selectedUserCount =
        selectedUserList.map { it?.size ?: 0 }.apply { this as MutableLiveData; value = 0 }
    val buttonClickable = generateMediatorLiveData(busyLoading, selectedUserCount) {
        busyLoading.value == false && (selectedUserCount.value ?: 0) > 0
    }
    val removeMemberResponse = AutoResetLiveData<EmptyNetworkResponse?>(null)

    fun toggleUserSelected(userId: UserId) {
        selectedUsers.value!!.let {
            when (it.contains(userId)) {
                true -> it.remove(userId)
                false -> it.add(userId)
            }
        }
        selectedUsers.postValue(selectedUsers.value)
    }

    fun removeFromGroup() {
        busyLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                selectedUserList.value?.let { list ->
                    removeMemberResponse.postValue(messageRepository.removeMembersFromGroup(chatId,
                        list.map { it.first.first.userId }).apply {
                        messageRepository.getChatMemberList(chatId).fetchAndSave()
                    })
                }
            } finally {
                busyLoading.postValue(false)
            }
        }
    }
}
