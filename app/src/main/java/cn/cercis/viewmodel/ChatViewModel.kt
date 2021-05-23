package cn.cercis.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.*
import cn.cercis.R
import cn.cercis.common.*
import cn.cercis.entity.Chat
import cn.cercis.entity.Message
import cn.cercis.repository.*
import cn.cercis.repository.MessageRepository.MessageUploadProgress
import cn.cercis.repository.MessageRepository.MessageUploadProgress.*
import cn.cercis.repository.MessageRepository.PendingMessage.TextMessage
import cn.cercis.util.getString
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
    private val globalConfigRepository: GlobalConfigRepository,
) : ViewModel(), LifecycleObserver {
    // caching user id from auth repository. this should not be a problem since this view model
    // will not survive currentUserId change.
    private val currentUserId = authRepository.currentUserId
    private val chatId = savedStateHandle.get<Long>("chatId")!!
    val chatInitData = savedStateHandle.get<Chat>("chat")!!
    val chatInitDisplay = savedStateHandle.get<CommonListItemData>("chatInitDisplay")
    val latestMessage = messageRepository.getLatestMessage(chatId).asLiveData(coroutineContext)
    val lastRead = messageRepository.getLastRead(chatId).asLiveData(coroutineContext)
    val unreadCount = generateMediatorLiveData(latestMessage, lastRead) {
        max(0L, (latestMessage.value?.messageId ?: 0L) - (lastRead.value ?: 0L))
    }
    private val chatParticipants = messageRepository.getChatMemberList(chatId).flow().map { res ->
        res.data?.map { userRepository.getUser(it.userId).dbFlow().asLiveData(coroutineContext) }
    }
    private val chatMessages = messageRepository.createMessageDataSource(chatId, MESSAGE_PAGE_SIZE)

    @SuppressLint("NullSafeMutableLiveData") // stupid workaround for IDE bugs
    val chatMessageList = MutableLiveData<List<Message>>(listOf())
    val isAtBottom = MutableLiveData(true)
    val unreadBubbleVisible = generateMediatorLiveData(unreadCount, isAtBottom) {
        return@generateMediatorLiveData (unreadCount.value ?: 0L > 0L && isAtBottom.value == false)
    }
    private val users = HashMap<UserId, LiveData<CommonListItemData>>()
    private val pendingMessages = ArrayList<Pair<Message, LiveData<MessageUploadProgress>>>()
    private val lastReadSubmitted: MutableStateFlow<MessageId> = MutableStateFlow(0L)

    // messages that are sending but not sent
    private val pendingMessageList = messageRepository.pendingMessageList.map {
        it.filter { pending -> pending.unsentMessage.chatId == chatId }
    }
    val failedMessageCount = pendingMessageList.mapLatest { list ->
        list.count { it is UploadFailed || it is SubmitFailed }
    }
    val pendingMessageDisplayCount = pendingMessageList.mapLatest { list ->
        Log.d(this@ChatViewModel.LOG_TAG, "list size: ${list.size}")
        val filteredList = list.filter { it is Uploading || it is Submitting }
        if (filteredList.isNotEmpty()) {
            // delay 200ms to avoid flickering
            delay(200)
        }
        Log.d(this@ChatViewModel.LOG_TAG, "submit list size: ${filteredList.size}")
        filteredList.size
    }

    // display of chat default info
    val chatDisplay = messageRepository.getParticipatedChat(chatId).filterNotNull().flatMapLatest {
        messageRepository.getChatDisplay(currentUserId, it)
    }.combine(pendingMessageDisplayCount) { t1, t2 ->
        if (t2 == 0) {
            t1
        } else t1?.copy(displayName = getString(R.string.chat_sending_message_count).format(t2))
    }.filterNotNull()
        .asInitializedLiveData(coroutineContext,
            chatInitDisplay ?: savedStateHandle.get<Chat>("chat")!!.let {
                CommonListItemData(it.avatar, it.name, "")
            }).map {
            Log.d(LOG_TAG, "read: $it")
            it
        }

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

    @MainThread
    fun informVisibleRange(start: MessageId, end: MessageId) {
        chatMessages.informVisibleRange(start, end)
        isAtBottom.value = (end >= (latestMessage.value?.messageId ?: 0L))
    }

    fun submitLastRead(messageId: MessageId) {
        lastReadSubmitted.value = max(lastReadSubmitted.value, messageId)
    }

    fun sendTextMessage(message: String) {
        messageRepository.addMessageToPendingList(TextMessage(chatId, message))
    }

    fun retryAllPendingMessages() {
        messageRepository.retryAllPendingMessages(chatId)
    }

    fun dropAllPendingMessages() {
        messageRepository.dropAllPendingMessages(chatId)
    }

    fun side(senderId: ChatId): MessageDirection {
        if (senderId == currentUserId) {
            return MessageDirection.OUTGOING
        }
        return MessageDirection.INCOMING
    }

    fun onSwipeRefresh() {
        chatMessages.triggerReload()
//        chatMessages.loadMorePrevious(MESSAGE_PAGE_SIZE)
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
