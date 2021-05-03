package cn.cercis.viewmodel

import androidx.lifecycle.*
import cn.cercis.entity.FriendEntry
import cn.cercis.entity.User
import cn.cercis.repository.FriendRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.Resource
import cn.cercis.util.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

@FlowPreview
@HiltViewModel
@ExperimentalCoroutinesApi
class ContactListViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    private val users = HashMap<UserId, LiveData<User>>()
    private val friendListSource by lazy {
        val liveData = MediatorLiveData<Resource<List<FriendEntry>>>()
        var source: LiveData<Resource<List<FriendEntry>>>? = null
        object {
            fun refresh() {
                source?.let {
                    liveData.removeSource(it)
                }
                source = friendRepository.getFriendList().asFlow()
                    .asLiveData(viewModelScope.coroutineContext + Dispatchers.IO).apply {
                        liveData.addSource(this) {
                            liveData.value = it
                        }
                    }
            }

            val liveData = liveData
        }
    }
    val friendList: LiveData<List<FriendEntry>> by lazy {
        // TODO replace with real data
        // ********
        MutableLiveData((0L..10L).map {
            FriendEntry(
                id = it,
                friendUserId = it,
                remarks = "",
                alias = "",
            )
        })
        // * replace the code above with the following code to enable real data
//        Transformations.map(friendListResource.liveData) { it?.data ?: listOf() }
        // ********
    }
    val friendListLoading by lazy { Transformations.map(friendListSource.liveData) { it is Resource.Loading } }

    /**
     * Gets a user's info, with cached [cn.cercis.http.NetworkBoundResource] object, to prevent
     * redundant GETs.
     */
    fun getUserInfo(userId: UserId): LiveData<User> {
        // TODO replace with real data
        return users.computeIfAbsent(userId) {
            // ********
            MutableLiveData(User(
                id = userId,
                nickname = "$userId",
                mobile = "12345$userId",
                chatId = 0,
                avatar = "",
                bio = "",
                updated = 0,
            ))
            // * replace the code above with the following code to enable real data
//            Transformations.map(userRepository.getUser(userId).asFlow().asLiveData(
//                viewModelScope.coroutineContext + Dispatchers.IO
//            )) { it.data }
            // ********
        }
    }

    /**
     * Refreshes users in the user list
     */
    fun refreshFriendList() {
        users.clear()
        friendListSource.refresh()
    }
}
