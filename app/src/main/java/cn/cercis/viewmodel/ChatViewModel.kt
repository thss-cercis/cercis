package cn.cercis.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.*
import cn.cercis.common.*
import cn.cercis.entity.Chat
import cn.cercis.entity.Message
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.MessageRepository.MessageUploadProgress
import cn.cercis.repository.MessageRepository.PendingMessage.TextMessage
import cn.cercis.repository.UserRepository
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.asInitializedLiveData
import cn.cercis.util.livedata.generateMediatorLiveData
import cn.cercis.util.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.max

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    authRepository: AuthRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
) : ViewModel(), LifecycleObserver {
    // caching user id from auth repository. this should not be a problem since this view model
    // will not survive currentUserId change.
    private val currentUserId = authRepository.currentUserId
    private val chatId = savedStateHandle.get<Long>("chatId")!!
    val chatInitData = savedStateHandle.get<Chat>("chat")!!
    val latestMessage = messageRepository.getLatestMessage(chatId).asLiveData(coroutineContext)
    val lastRead = messageRepository.getLastRead(chatId).asLiveData(coroutineContext)
    val unreadCount = generateMediatorLiveData(latestMessage, lastRead) {
        max(0L, (latestMessage.value?.messageId ?: 0) - (lastRead.value ?: 0))
    }
    private val chatParticipants = messageRepository.getChatMemberList(chatId).flow().map { res ->
        res.data?.map { userRepository.getUser(it.userId).dbFlow().asLiveData(coroutineContext) }
    }
    val chatDisplay = messageRepository.getParticipatedChat(chatId).filterNotNull().flatMapLatest {
        messageRepository.getChatDisplay(currentUserId, it)
    }.asInitializedLiveData(coroutineContext, savedStateHandle.get<Chat>("chat")!!.let {
        CommonListItemData(it.avatar, it.name, "")
    })
    private val chatMessages = messageRepository.createMessageDataSource(chatId, MESSAGE_PAGE_SIZE)

    @SuppressLint("NullSafeMutableLiveData") // stupid workaround for IDE bugs
    val chatMessageList = MutableLiveData<List<Message>>(listOf())
    private val users = HashMap<UserId, LiveData<CommonListItemData>>()
    private val pendingMessages = ArrayList<Pair<Message, LiveData<MessageUploadProgress>>>()
    private val lastReadSubmitted: MutableStateFlow<MessageId> = MutableStateFlow(0L)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            chatMessages.messageFlow.collect { res: Resource<List<Message>> ->
                when (res) {
                    is Resource.Error -> Unit
                    is Resource.Loading -> Unit
                    is Resource.Success -> chatMessageList.postValue(res.data.reversed())
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                lastReadSubmitted.collect {
                    if (it != 0L) {
                        Log.d(LOG_TAG, "updated last read to $it")
                        messageRepository.updateLastRead(chatId, it)
                    }
                }
            }
        }
    }

    fun submitLastRead(messageId: MessageId) {
        lastReadSubmitted.value = max(lastReadSubmitted.value, messageId)
    }

    fun sendTextMessage(message: String) {
        messageRepository.addMessageToPendingList(TextMessage(chatId, message))
    }

    fun side(senderId: ChatId): MessageDirection {
        if (senderId == currentUserId) {
            return MessageDirection.OUTGOING
        }
        return MessageDirection.INCOMING
    }

    fun onSwipeRefresh() {
        chatMessages.loadMorePrevious(MESSAGE_PAGE_SIZE)
    }

    fun lockToLatest() {
        chatMessages.setLockToLatest(true)
    }

    fun isLockedToLatest(): Boolean {
        return chatMessages.lockToLatest.value
    }

    @MainThread
    fun loadUser(userId: UserId): LiveData<CommonListItemData> {
        return users.computeIfAbsent(userId) { uid ->
            Log.d(LOG_TAG, "GenerateChatMemberView for $userId")
            userRepository.getUserWithFriendDisplay(uid, false)
                .combine(
                    messageRepository.getChatMemberList(chatId).flow()
                ) { friendItem, chatMemberList ->
                    chatMemberList.data?.firstOrNull { it.userId == uid }.let { member ->
                        CommonListItemData(
                            displayName = member?.displayName.takeUnless { it.isNullOrEmpty() }
                                ?: friendItem.displayName,
                            avatar = friendItem.avatar,
                            description = friendItem.description
                        )
                    }
                }.asLiveData(coroutineContext)
        }
    }

    enum class MessageDirection(val type: Int) {
        INCOMING(1), OUTGOING(2)
    }
}
