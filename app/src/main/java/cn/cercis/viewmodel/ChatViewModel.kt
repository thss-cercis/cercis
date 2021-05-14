package cn.cercis.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.*
import cn.cercis.Constants
import cn.cercis.common.ChatId
import cn.cercis.common.MESSAGE_PAGE_SIZE
import cn.cercis.common.MessageId
import cn.cercis.common.UserId
import cn.cercis.entity.Chat
import cn.cercis.entity.Message
import cn.cercis.entity.User
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.asInitiatedLiveData
import cn.cercis.util.resource.Resource
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
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel(), LifecycleObserver {
    private val chatId = savedStateHandle.get<Long>("chatId")!!
    val chatInitData = savedStateHandle.get<Chat>("chat")!!
    private val chatParticipants = messageRepository.getChatMemberList(chatId).flow().map { res ->
        res.data?.map { userRepository.getUser(it.userId).dbFlow().asLiveData(coroutineContext) }
    }
    val chatDisplay = messageRepository.getParticipatedChat(chatId).filterNotNull().flatMapLatest {
        messageRepository.getChatDisplay(authRepository.currentUserId, it)
    }.asInitiatedLiveData(coroutineContext, savedStateHandle.get<Chat>("chat")!!.let {
        CommonListItemData(it.avatar, it.name, "")
    })
    val chatMessages = messageRepository.createMessageDataSource(chatId, MESSAGE_PAGE_SIZE)

    fun side(senderId: ChatId): Side {
        if (senderId == authRepository.currentUserId) {
            return Side.SELF
        }
        return Side.OTHER
    }

    private var messageLoading = false

    // range of messages being displayed
    private val currentIdStart: MessageId = 0
    private val currentIdEnd: MessageId = 0

    @MainThread
    fun loadUser(userId: UserId): LiveData<Resource<User>> {
        TODO()
    }

    @MainThread
    private fun loadCurrentMessages(count: Long) {
        preventDoubleSubmitRun {
            messageRepository.getChatRecentMessages(count, 15)
        }
    }

    @MainThread
    private fun preventDoubleSubmitRun(runnable: suspend () -> Unit) {
        if (messageLoading) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                runnable()
            } finally {
                messageLoading = false
            }
        }
    }

    enum class Side {
        SELF, OTHER
    }
}
