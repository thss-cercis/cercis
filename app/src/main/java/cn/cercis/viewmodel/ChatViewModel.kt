package cn.cercis.viewmodel

import android.annotation.SuppressLint
import android.media.MediaRecorder
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
import cn.cercis.repository.MessageRepository.PendingMessage.*
import cn.cercis.util.getString
import cn.cercis.util.getTempFile
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.helper.instantCombine
import cn.cercis.util.livedata.asInitializedLiveData
import cn.cercis.util.livedata.generateMediatorLiveData
import cn.cercis.util.livedata.observeOnce
import cn.cercis.util.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.inject.Inject
import kotlin.math.max


@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
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
    val initialDataSet = MutableLiveData(false)
    private val chatParticipants = messageRepository.getChatMemberList(chatId).flow().map { res ->
        res.data?.map { userRepository.getUser(it.userId).dbFlow().asLiveData(coroutineContext) }
    }
    private val chatMessages = messageRepository.createMessageDataSource(chatId, MESSAGE_PAGE_SIZE)

    suspend fun getOtherUser(): Resource<UserId> {
        return messageRepository.getOtherUserId(authRepository.currentUserId, chatId).first()
    }

    data class MessageComposeId(
        val messageId: MessageId,
        val messageSerial: Int,
    ) : Comparable<MessageComposeId> {
        override fun compareTo(other: MessageComposeId): Int {
            return when {
                messageId < other.messageId -> -1
                messageId == other.messageId -> messageSerial - other.messageSerial
                else -> 1
            }
        }
    }

    /**
     * A display message can either be a sent or an unsent message.
     */
    sealed interface DisplayMessage {
        val message: String
        val senderId: UserId
        val type: Int
        val messageComposeId: MessageComposeId
    }

    data class SentDisplayMessage(
        val msg: Message,
    ) : DisplayMessage {
        override val message: String = msg.message
        override val messageComposeId = MessageComposeId(msg.messageId, 0)
        override val senderId: UserId = msg.senderId
        override val type: Int = msg.type
    }

    data class PendingDisplayMessage(
        val msgProgress: MessageUploadProgress,
        val currentUserId: UserId,
    ) : DisplayMessage {
        override val message: String = msgProgress.unsentMessage.let {
            when (it) {
                is MessageRepository.PendingMessage -> ""
                is MessageRepository.PreparedMessage -> it.content
            }
        }
        override val senderId: UserId = currentUserId
        override val messageComposeId = MessageComposeId(
            msgProgress.unsentMessage.attachAfter,
            msgProgress.unsentMessage.messageSerial)
        override val type: Int = msgProgress.unsentMessage.messageType.type

        val isSending: Boolean = msgProgress is Uploading || msgProgress is Submitting
        val isFailed: Boolean = !isSending
    }

    val initialValue = ArrayList<DisplayMessage>()
    @SuppressLint("NullSafeMutableLiveData") // stupid workaround for IDE bugs
    val chatMessageList = MutableLiveData<List<DisplayMessage>>(initialValue)
    val isAtBottom = MutableStateFlow(true)
    val fabVisible = isAtBottom.mapLatest {
        if (!it) {
            // delays being visible
            delay(400) // longer than bottom popup
        }
        it
    }.asInitializedLiveData(coroutineContext, true)
    val unreadBubbleVisible = generateMediatorLiveData(unreadCount, fabVisible) {
        return@generateMediatorLiveData (unreadCount.value ?: 0L > 0L && fabVisible.value == false)
    }
    private val users = HashMap<UserId, LiveData<CommonListItemData>>()
    private val lastReadSubmitted: MutableStateFlow<MessageId> = MutableStateFlow(0L)

    // messages that are sending but not sent
    private val pendingMessageList = messageRepository.pendingMessageList.map {
        it.filter { pending -> pending.unsentMessage.chatId == chatId }
    }.shareIn(viewModelScope, SharingStarted.Eagerly, 1)
    val failedMessageCount = pendingMessageList.mapLatest { list ->
        list.count { it is UploadFailed || it is SubmitFailed }
    }
    private val pendingMessageDisplayCount = pendingMessageList.mapLatest { list ->
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
            })

    // panel
    val expanded = MutableLiveData(false)
    val selectedPage = MutableLiveData(0)
    val buttonSelected = (0..3).map {
        generateMediatorLiveData(expanded, selectedPage) {
            expanded.value == true && selectedPage.value == it
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            instantCombine(chatMessages.messageFlow,
                pendingMessageList.mapLatest { delay(200); it }).collectLatest { pair ->
                val (res: Resource<List<Message>>?, pending) = pair
                Log.d(LOG_TAG, "pending: $pending")
                when (res) {
                    null -> Unit
                    is Resource.Error -> Unit
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        chatMessageList.postValue(
                            if (pending != null) {
                                (res.data.map { SentDisplayMessage(it) } + pending.map {
                                    PendingDisplayMessage(it, currentUserId)
                                })
                            } else {
                                res.data.map { SentDisplayMessage(it) }
                            }.sortedByDescending { it.messageComposeId }
                        )
                    }
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

    fun withdrawMessage(messageId: MessageId) {
        messageRepository.addMessageToPendingList(WithdrawMessage(chatId, messageId))
    }

    fun sendTextMessage(message: String) {
        messageRepository.addMessageToPendingList(TextMessage(chatId,
            latestMessage.value?.messageId ?: 1,
            message))
    }

    fun sendAudioMessage(file: File) {
        messageRepository.addMessageToPendingList(AudioMessage(chatId,
            latestMessage.value?.messageId ?: 1, file))
    }

    fun sendVideoMessage(file: File) {
        messageRepository.addMessageToPendingList(VideoMessage(chatId,
            latestMessage.value?.messageId ?: 1, file))
    }

    fun sendImageMessage(file: File) {
        messageRepository.addMessageToPendingList(ImageMessage(chatId,
            latestMessage.value?.messageId ?: 1, file))
    }

    fun retryAllPendingMessages() {
        messageRepository.retryAllPendingMessages(chatId)
    }

    fun retryMessage(msgProgress: MessageUploadProgress) {
        if (msgProgress is SubmitFailed) {
            messageRepository.resubmitSubmitFailedMessage(msgProgress.unsentMessage.messageSerial)
        } else if (msgProgress is UploadFailed) {
            messageRepository.resubmitUploadFailedMessage(msgProgress.unsentMessage.messageSerial)
        }
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

    @MainThread
    fun expandPanel() {
        if (expanded.value != true) {
            expanded.value = true
        }
    }

    @MainThread
    fun foldPanel() {
        if (expanded.value != false) {
            expanded.value = false
        }
    }

    @MainThread
    fun togglePanel() {
        expanded.value = expanded.value?.not() ?: true
    }

    fun isLockedToLatest(): Boolean {
        return chatMessages.lockToLatest.value
    }

    private val mediaRecorder = MediaRecorder()
    private val recordingFile = MutableLiveData<String?>(null)
    val isRecording = MutableLiveData(false)

    @MainThread
    fun startRecording() {
        if (isRecording.value != true) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioEncodingBitRate(128 * 1024)
            mediaRecorder.setAudioSamplingRate(44100)
            getTempFile(".mp4").absolutePath.let { filename ->
                recordingFile.value = filename
                mediaRecorder.setOutputFile(filename)
            }
            mediaRecorder.prepare()
            mediaRecorder.start()
            isRecording.value = true
        }
    }

    /**
     * Finishes audio recording and sends it.
     */
    @MainThread
    fun finishRecording() {
        if (isRecording.value == true) {
            try {
                mediaRecorder.stop()
                mediaRecorder.reset()
                val filename = recordingFile.value!!
                sendAudioMessage(File(filename))
            } finally {
                isRecording.value = false
                recordingFile.value = null
            }
        }
    }

    /**
     * Cancels audio recording and drops recorded file.
     */
    @MainThread
    fun cancelRecording() {
        if (isRecording.value == true) {
            try {
                mediaRecorder.stop()
                mediaRecorder.reset()
                val filename = recordingFile.value!!
                File(filename).delete()
            } finally {
                isRecording.value = false
                recordingFile.value = null
            }
        }
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

    override fun onCleared() {
        mediaRecorder.release()
    }

    enum class MessageDirection(val type: Int) {
        INCOMING(1), OUTGOING(2)
    }
}
