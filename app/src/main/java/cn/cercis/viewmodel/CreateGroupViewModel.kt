package cn.cercis.viewmodel

import androidx.lifecycle.*
import cn.cercis.common.UserId
import cn.cercis.common.mapRun
import cn.cercis.entity.Chat
import cn.cercis.entity.FriendUser
import cn.cercis.repository.FriendRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.asInitializedLiveData
import cn.cercis.util.livedata.generateMediatorLiveData
import cn.cercis.util.resource.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.collections.HashSet

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val chatRepository: MessageRepository,
) : ViewModel() {
    private val refreshTime = MutableStateFlow(System.currentTimeMillis())
    private val listFlow: Flow<List<FriendUser>> = refreshTime.flatMapLatest {
        friendRepository.getFriendUserList().flow().map { it.data }.filterNotNull().map { list ->
            userLoaded.addAll(list.filter { it.nickname != null }.mapRun { friendUserId })
            list
        }
    }
    private val userLoaded = Collections.newSetFromMap(ConcurrentHashMap<UserId, Boolean>())
    private val selectedUsers = MutableLiveData<HashSet<UserId>>(HashSet())
    private val listLiveData = listFlow.asInitializedLiveData(coroutineContext, listOf())
    private val busyLoading = MutableLiveData<Boolean>(false)
    val friendList = generateMediatorLiveData(listLiveData, selectedUsers) {
        listLiveData.value!!.map { it to selectedUsers.value!!.contains(it.friendUserId) }
    }
    val selectedUserList = generateMediatorLiveData(listLiveData, selectedUsers) {
        listLiveData.value!!.filter { selectedUsers.value!!.contains(it.friendUserId) }
    }
    val selectedUserCount =
        selectedUserList.map { it?.size ?: 0 }.apply { this as MutableLiveData; value = 0 }
    val buttonClickable = generateMediatorLiveData(busyLoading, selectedUserCount) {
        busyLoading.value == false && (selectedUserCount.value ?: 0) > 0
    }
    val networkResponse = MutableLiveData<NetworkResponse<Chat>>(null)

    fun toggleUserSelected(userId: UserId) {
        selectedUsers.value!!.let {
            when (it.contains(userId)) {
                true -> it.remove(userId)
                false -> it.add(userId)
            }
        }
        selectedUsers.postValue(selectedUsers.value)
    }

    private fun loadUser(userId: UserId) {
        if (!userLoaded.contains(userId)) {
            // prevent double submit
            userLoaded.add(userId)
            viewModelScope.launch(Dispatchers.IO) {
                if (userRepository.getUser(userId).fetchAndSave() !is NetworkResponse.Success) {
                    // encourage reload
                    userLoaded.remove(userId)
                }
            }
        }
    }

    fun createGroup() {
        busyLoading.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                selectedUserList.value?.let {
                    networkResponse.postValue(chatRepository.createGroup(it.mapRun { friendUserId }))
                }
            } finally {
                busyLoading.postValue(false)
            }
        }
    }
}
