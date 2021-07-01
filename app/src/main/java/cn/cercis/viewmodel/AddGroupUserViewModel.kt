package cn.cercis.viewmodel

import androidx.lifecycle.*
import cn.cercis.common.ChatId
import cn.cercis.common.UserId
import cn.cercis.entity.FriendUser
import cn.cercis.http.EmptyNetworkResponse
import cn.cercis.repository.FriendRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.AutoResetLiveData
import cn.cercis.util.livedata.asInitializedLiveData
import cn.cercis.util.livedata.generateMediatorLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class AddGroupUserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    friendRepository: FriendRepository,
    private val messageRepository: MessageRepository,
) : ViewModel() {
    val chatId = savedStateHandle.get<ChatId>("chatId")!!
    private val refreshTime = MutableStateFlow(System.currentTimeMillis())
    private val listFlow: Flow<List<FriendUser>> = refreshTime.flatMapLatest {
        friendRepository.getFriendUserList()
    }
    private val selectedUsers = MutableLiveData<HashSet<UserId>>(HashSet())
    private val listLiveData = listFlow.asInitializedLiveData(coroutineContext, listOf())
    private val busyLoading = MutableLiveData(false)
    private val members = messageRepository.getChatMemberList(chatId).fallbackFlow()
        .mapLatest { res ->
            res.data?.let { list -> HashSet<UserId>(list.map { it.userId }) } ?: HashSet()
        }
        .asLiveData(coroutineContext)
    val friendList = generateMediatorLiveData(listLiveData, selectedUsers, members) {
        listLiveData.value!!.map {
            Triple(
                it,
                selectedUsers.value!!.contains(it.friendUserId),
                members.value?.contains(it.friendUserId) ?: false
            )
        }
    }
    val selectedUserList = generateMediatorLiveData(listLiveData, selectedUsers) {
        listLiveData.value!!.filter { selectedUsers.value!!.contains(it.friendUserId) }
    }
    val selectedUserCount =
        selectedUserList.map { it?.size ?: 0 }.apply { this as MutableLiveData; value = 0 }
    val buttonClickable = generateMediatorLiveData(busyLoading, selectedUserCount) {
        busyLoading.value == false && (selectedUserCount.value ?: 0) > 0
    }
    val selectUserResponse = AutoResetLiveData<EmptyNetworkResponse?>(null)

    fun toggleUserSelected(userId: UserId) {
        selectedUsers.value!!.let {
            when (it.contains(userId)) {
                true -> it.remove(userId)
                false -> it.add(userId)
            }
        }
        selectedUsers.postValue(selectedUsers.value)
    }

    fun addToGroup() {
        busyLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                selectedUserList.value?.let { list ->
                    selectUserResponse.postValue(messageRepository.addMembersToGroup(chatId,
                        list.asSequence().map { it.friendUserId }
                            .filterNot { members.value?.contains(it) ?: false }.toList()).apply {
                        messageRepository.getChatMemberList(chatId).fetchAndSave()
                    })
                }
            } finally {
                busyLoading.postValue(false)
            }
        }
    }
}
