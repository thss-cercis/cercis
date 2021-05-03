package cn.cercis.viewmodel

import androidx.annotation.MainThread
import androidx.lifecycle.*
import cn.cercis.Constants
import cn.cercis.entity.Message
import cn.cercis.entity.User
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.ChatId
import cn.cercis.util.MessageId
import cn.cercis.util.Resource
import cn.cercis.util.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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
    private val chatId = savedStateHandle.get<Long>("chatId") ?: -1
    private val chatParticipants = messageRepository.getChatParticipants(chatId)
    val chat = messageRepository.getChat(chatId)

    fun side(senderId: ChatId): Side {
        if (senderId == authRepository.currentUserId) {
            return Side.SELF
        }
        return Side.OTHER
    }

    /**
     * Messages to be displayed.
     */
    private val messageList: ArrayList<Message> = ArrayList()
    private var messageListDataSource0: LiveData<List<Message>>? = null
    val messageListDataSource: MediatorLiveData<List<Message>> = MediatorLiveData()


    /** used by [loadMorePreviousMessage] */
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
    private fun reloadMessageList(messages: List<Message>) {
        messageList.clear()
        messageList.addAll(messages)
    }

    @MainThread
    private fun replaceDataSource(messageDataSource: LiveData<List<Message>>) {
        messageListDataSource0?.let { messageListDataSource.removeSource(it) }
        messageListDataSource0 = messageDataSource
        messageListDataSource.addSource(messageDataSource) {
            if (it != null) {
                reloadMessageList(it)
            }
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

    /**
     * Loads more previous messages when for example, swiped to the top.
     */
    @MainThread
    private fun loadMorePreviousMessage(count: Long) {
        preventDoubleSubmitRun {
            val newData = messageRepository.getChatMessagesNewerThanWithPreviousMessages(
                    chatId,
                    currentIdStart,
                    Constants.REFRESH_COUNT,
            )
            messageLoading = false
        }
    }

    enum class Side {
        SELF, OTHER
    }

    enum class Type {
        MULTIPLE, SINGLE
    }
}
