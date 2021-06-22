package cn.cercis.repository

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.paging.*
import cn.cercis.Constants.STATIC_BASE
import cn.cercis.R
import cn.cercis.common.*
import cn.cercis.dao.ChatDao
import cn.cercis.dao.ChatMemberDao
import cn.cercis.dao.MessageDao
import cn.cercis.entity.*
import cn.cercis.http.*
import cn.cercis.repository.MessageRepository.PendingMessage.*
import cn.cercis.util.getString
import cn.cercis.util.helper.FileUploadUtils
import cn.cercis.util.resource.DataSource
import cn.cercis.util.resource.DataSourceBase
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.resource.NetworkResponse.Success
import cn.cercis.util.resource.Resource
import cn.cercis.viewmodel.CommonListItemData
import com.squareup.moshi.JsonAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min


@FlowPreview
@ExperimentalCoroutinesApi
@ActivityRetainedScoped
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val chatMemberDao: ChatMemberDao,
    private val httpService: CercisHttpService,
    private val userRepository: UserRepository,
    private val fileUploadUtils: FileUploadUtils,
    @ApplicationContext val context: Context,
) {
    fun getAllChats() = object : DataSource<List<Chat>>() {
        override suspend fun fetch(): NetworkResponse<List<Chat>> {
            return httpService.getChatList()
        }

        override suspend fun saveToDb(data: List<Chat>) {
            chatDao.updateAllChats(data)
        }

        override fun loadFromDb(): Flow<List<Chat>> {
            return chatDao.loadAllChats()
        }
    }

    fun getChat(chatId: ChatId): Flow<Chat?> = chatDao.loadChat(chatId)

    fun getAllChatsWithLatestMessageOrderedByUpdate() =
        object : DataSourceBase<List<ChatWithLatestMessage>, List<Chat>>() {
            override suspend fun fetch(): NetworkResponse<List<Chat>> {
                return httpService.getChatList()
            }

            override suspend fun saveToDb(data: List<Chat>) {
                chatDao.updateAllChats(data)
            }

            override fun loadFromDb(): Flow<List<ChatWithLatestMessage>> {
                return messageDao.loadAllChatsOrderedByUpdateOrLatestMessage()
            }
        }

    fun debugInsertAllChats(chatList: List<Chat>) {
        chatDao.updateAllChats(chatList)
    }

    /**
     * Populates messages into the db and replace deleted messages with special ones.
     */
    private fun insertMessagesAndPerformWithdraw(messages: List<Message>) {
        val withdrawedMessages = messages.filter {
            //            is withdraw            //          valid content         //
            it.type == MessageType.WITHDRAW.type && it.message.toLongOrNull() != null
        }.map {
            Message(
                messageId = it.message.toLong(),
                chatId = it.chatId,
                type = MessageType.DELETED.type,
                message = "",
                senderId = it.senderId,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        }
        messageDao.insertIgnoreAndInsertReplace(
            messages,
            withdrawedMessages
        )
    }

    suspend fun saveSingleMessage(chatId: ChatId, messageId: MessageId): NetworkResponse<Message> {
        return getSingleMessage(chatId, messageId).fetchAndSave().use {
            if (chatDao.loadChatOnce(chatId) == null) {
                getChatList().fetchAndSave()
            }
            this
        }
    }

    fun getSingleMessage(chatId: ChatId, messageId: MessageId) = object : DataSource<Message>() {
        override suspend fun fetch(): NetworkResponse<Message> {
            return httpService.getSingleMessage(chatId, messageId)
        }

        override suspend fun saveToDb(data: Message) {
            insertMessagesAndPerformWithdraw(listOf(data))
        }

        override fun loadFromDb(): Flow<Message?> {
            return messageDao.loadSingleMessage(chatId, messageId)
        }

        override fun shouldFetch(data: Message?) = data == null
    }

    private suspend fun sendMessage(
        chatId: ChatId,
        type: MessageType,
        content: String,
    ): NetworkResponse<Message> {
        return httpService.sendMessage(
            SendMessageRequest(
                chatId, message = content, type = type.type
            )
        ).apply {
            if (this is Success) {
                insertMessagesAndPerformWithdraw(listOf(this.data))
            }
        }
    }


    /**
     * Indicates uploading progress of a message.
     */
    sealed class MessageUploadProgress(val unsentMessage: UnsentMessage) {
        data class Uploading(
            val pendingMessage: PendingMessage,
        ) : MessageUploadProgress(pendingMessage)

        data class UploadFailed(
            val pendingMessage: PendingMessage,
            val message: String,
        ) : MessageUploadProgress(pendingMessage)

        data class Submitting(
            val preparedMessage: PreparedMessage,
        ) : MessageUploadProgress(preparedMessage)

        data class SubmitFailed(
            val preparedMessage: PreparedMessage,
            val message: String,
        ) : MessageUploadProgress(preparedMessage)
    }

    sealed interface UnsentMessage {
        val messageSerial: Int
        val chatId: ChatId
        val messageType: MessageType
        val attachAfter: MessageId
    }

    /**
     * Message with resource to upload.
     */
    sealed class PendingMessage(
        override val chatId: ChatId,
        override val attachAfter: MessageId,
        override val messageType: MessageType,
        override val messageSerial: Int = PendingMessageIndex.incrementAndGet(),
        open val file: File? = null,
    ) : UnsentMessage {
        data class TextMessage(
            override val chatId: ChatId,
            override val attachAfter: MessageId,
            val text: String,
        ) : PendingMessage(chatId, attachAfter, MessageType.TEXT)

        data class ImageMessage(
            override val chatId: ChatId,
            override val attachAfter: MessageId,
            override val file: File,
        ) : PendingMessage(chatId, attachAfter, MessageType.IMAGE)

        data class AudioMessage(
            override val chatId: ChatId,
            override val attachAfter: MessageId,
            override val file: File,
        ) : PendingMessage(chatId, attachAfter, MessageType.AUDIO)

        data class VideoMessage(
            override val chatId: ChatId,
            override val attachAfter: MessageId,
            override val file: File,
        ) : PendingMessage(chatId, attachAfter, MessageType.VIDEO)

        data class LocationMessage(
            override val chatId: ChatId,
            override val attachAfter: MessageId,
            val latitude: Double,
            val longitude: Double,
            val description: String,
        ) : PendingMessage(chatId, attachAfter, MessageType.LOCATION)

        data class WithdrawMessage(override val chatId: ChatId, val messageId: MessageId) :
            PendingMessage(chatId, messageId, MessageType.WITHDRAW)
    }

    /**
     * Message with resource uploaded and ready to be submitted to Cercis server.
     */
    data class PreparedMessage(
        override val messageSerial: Int,
        override val chatId: ChatId,
        override val attachAfter: MessageId,
        override val messageType: MessageType,
        val content: String,
    ) : UnsentMessage

    private val messagesWaitingToUploadResource =
        Channel<PendingMessage>(capacity = Channel.UNLIMITED)
    private val messagesWaitingToSubmit = Channel<PreparedMessage>(capacity = Channel.UNLIMITED)

    /**
     * This list is edited in:
     * * [addMessageToPendingList]
     * * [processMessageSending]
     */
    private val pendingMessages = ConcurrentHashMap<Int, MessageUploadProgress>()

    val pendingMessageList = MutableStateFlow<List<MessageUploadProgress>>(listOf())
    private fun updatePendingMessageList() {
        pendingMessageList.value = pendingMessages.toList().sortedBy { it.first }.map { it.second }
    }

    /**
     * Called from [cn.cercis.MainActivity] to start message processing.
     */
    fun processMessageSending(scope: CoroutineScope) {
        repeat(5) {
            Log.d(LOG_TAG, "started queue consumer")
            // resource uploading handlers
            scope.launch(Dispatchers.IO) {
                for (pendingMessage in messagesWaitingToUploadResource) {
                    val res = uploadResource(pendingMessage)
                    Log.d(LOG_TAG, "tried to upload(${pendingMessage.messageSerial}): $res")
                    if (res is Success) {
                        messagesWaitingToSubmit.send(res.data)
                        pendingMessages[pendingMessage.messageSerial] =
                            MessageUploadProgress.Submitting(res.data)
                    } else {
                        pendingMessages[pendingMessage.messageSerial] =
                            MessageUploadProgress.UploadFailed(pendingMessage, res.message ?: "")
                    }
                    updatePendingMessageList()
                }
            }
        }
        repeat(3) {
            scope.launch(Dispatchers.IO) {
                // message submitting handlers
                for (preparedMessage in messagesWaitingToSubmit) {
                    val res = sendMessage(
                        preparedMessage.chatId,
                        preparedMessage.messageType,
                        preparedMessage.content
                    )
                    Log.d(LOG_TAG, "tried to send(${preparedMessage.messageSerial}): $res")
                    if (res is Success) {
                        pendingMessages.remove(preparedMessage.messageSerial)
                    } else {
                        pendingMessages[preparedMessage.messageSerial] =
                            MessageUploadProgress.SubmitFailed(preparedMessage, res.message ?: "")
                    }
                    updatePendingMessageList()
                }
            }
        }
    }

    fun addMessageToPendingList(message: PendingMessage) {
        Log.d(LOG_TAG, "added to pending list: $message")
        pendingMessages[message.messageSerial] = MessageUploadProgress.Uploading(message)
        messagesWaitingToUploadResource.sendBlocking(message)
        updatePendingMessageList()
    }

    fun dropAllPendingMessages(chatId: ChatId) {
        pendingMessages.entries.filter { it.value.unsentMessage.chatId == chatId }.forEach {
            pendingMessages.remove(it.key)
        }
        updatePendingMessageList()
    }

    fun retryAllPendingMessages(chatId: ChatId) {
        pendingMessages.entries.filter { it.value.unsentMessage.chatId == chatId }.forEach {
            if (it.value is MessageUploadProgress.UploadFailed) {
                resubmitUploadFailedMessage(it.key)
            } else if (it.value is MessageUploadProgress.SubmitFailed) {
                resubmitSubmitFailedMessage(it.key)
            }
        }
        updatePendingMessageList()
    }

    fun resubmitUploadFailedMessage(messageSerial: Int) {
        pendingMessages.remove(messageSerial)?.unsentMessage.let {
            if (it is PendingMessage) {
                addMessageToPendingList(it)
            }
        }
    }

    fun resubmitSubmitFailedMessage(messageSerial: Int) {
        pendingMessages.remove(messageSerial)?.unsentMessage.let {
            if (it is PreparedMessage) {
                messagesWaitingToSubmit.sendBlocking(it)
                pendingMessages[it.messageSerial] =
                    MessageUploadProgress.Submitting(it)
            }
        }
    }

    /**
     * Uploads resources for a message, and returns a PreparedMessage if succeeded, or null if failed.
     */
    private suspend fun uploadResource(message: PendingMessage): NetworkResponse<PreparedMessage> {
        return when (message) {
            is AudioMessage, is ImageMessage, is VideoMessage  ->
                fileUploadUtils.uploadFile(message.file!!)
                    .use {
                        PreparedMessage(
                            message.messageSerial,
                            message.chatId,
                            message.attachAfter,
                            message.messageType,
                            STATIC_BASE + this
                        )
                    }
            is LocationMessage -> Success(
                PreparedMessage(
                    message.messageSerial,
                    message.chatId,
                    message.attachAfter,
                    MessageType.LOCATION,
                    "${message.longitude}#${message.latitude}#${message.description}"
                )
            )
            is TextMessage -> Success(
                PreparedMessage(
                    message.messageSerial,
                    message.chatId,
                    message.attachAfter,
                    MessageType.TEXT,
                    message.text
                )
            )
            is WithdrawMessage -> return Success(
                PreparedMessage(
                    message.messageSerial,
                    message.chatId,
                    message.attachAfter,
                    MessageType.WITHDRAW,
                    message.messageId.toString()
                )
            )
        }
    }

    /**
     * Gets chat with another user.
     */
    fun getPrivateChatWith(userId: UserId, otherId: UserId) = object : DataSource<Chat>() {
        override suspend fun fetch(): NetworkResponse<Chat> {
            val chat = httpService.getPrivateChatWith(otherId)
            if (chat is Success) {
                // save member list
                Log.d(LOG_TAG, "private chat got. loading chat members.")
                // no need to double-check a private chat's members, so no force fetch
                return getChatMemberList(chat.data.id, false).fetchAndSave().use { chat.data }
            } else if (chat is NetworkResponse.Reject) {
                // create new private chat if non is given
                val created = httpService.createPrivateChat(CreatePrivateChatRequest(otherId))
                if (created is Success) {
                    return getChatMemberList(created.data.id, false).fetchAndSave()
                        .use { created.data }
                }
                return created
            }
            return chat
        }

        override suspend fun saveToDb(data: Chat) {
            chatDao.insertChat(data)
        }

        override fun loadFromDb(): Flow<Chat?> {
            return chatMemberDao.loadSharedChats(userId, otherId)
                .filter { it == null || it.type == ChatType.CHAT_PRIVATE }
        }
    }

    /**
     * Gets all members' userIds in a chat.
     */
    fun getChatMemberList(chatId: ChatId, forceFetch: Boolean = true) =
        object : DataSource<List<ChatMember>>() {
            override suspend fun fetch() = httpService.getChatMemberList(chatId)

            override suspend fun saveToDb(data: List<ChatMember>) {
                chatMemberDao.updateChatMemberList(chatId, data)
            }

            override fun loadFromDb() = chatMemberDao.loadChatMembers(chatId)

            override fun shouldFetch(data: List<ChatMember>?) =
                forceFetch || (data?.isEmpty() ?: true)
        }

    /**
     * Gets another user's id in a private chat.
     */
    fun getOtherUserId(selfUserId: UserId, chatId: ChatId): Flow<Resource<UserId>> =
        getChatMemberList(chatId, false).flow().map { it.data }
            .filterNotNull().filter { it.isNotEmpty() }.map { member ->
                member.firstOrNull { it.userId != selfUserId }?.userId?.let {
                    Resource.Success(it)
                } ?: Resource.Error(0, "Unknown error", null)
            }

    /**
     * Gets a full chat list.
     */
    fun getChatList() = object : DataSource<List<Chat>>() {
        override suspend fun fetch(): NetworkResponse<List<Chat>> {
            return httpService.getChatList()
        }

        override suspend fun saveToDb(data: List<Chat>) {
            // TODO this makes caching chats impossible, but probably this doesn't matter
            chatDao.updateAllChats(data)
        }

        override fun loadFromDb(): Flow<List<Chat>> {
            return chatDao.loadAllChats()
        }
    }

    suspend fun fetchAndSaveLatestMessages(): NetworkResponse<*> {
        val chatLatestMessageIdRes = httpService.getAllChatsLatestMessageId()
        if (chatLatestMessageIdRes !is Success) {
            return chatLatestMessageIdRes
        }
        val allLatest = messageDao.loadAllChatLatestMessagesOnce().toSet()
        val shouldFetch = chatLatestMessageIdRes.data.filter {
            ChatIdMessageId(it.chatId, it.latestMessageId) !in allLatest
        }
        if (shouldFetch.isNotEmpty()) {
            Log.d(LOG_TAG, "should fetch: $shouldFetch")
        }
        return httpService.getChatsLatestMessages(GetChatsLatestMessagesRequest(shouldFetch.map { it.chatId }))
            .apply {
                if (this is Success) {
                    insertMessagesAndPerformWithdraw(data)
                }
            }
    }

    suspend fun createGroup(name: String, groupMemberList: List<UserId>): NetworkResponse<Chat> {
        return httpService.createGroupChat(CreateGroupChatRequest(
            name = name,
            memberIds = groupMemberList,
        )).apply {
            if (this is Success) {
                chatDao.insertChat(this.data)
            }
        }
    }

    suspend fun addMembersToGroup(chatId: ChatId, newMembers: List<UserId>): EmptyNetworkResponse {
        for (user in newMembers) {
            // interrupt inviting when network error occurred
            when (val res = httpService.inviteGroupMember(InviteGroupMemberRequest(chatId, user))) {
                is NetworkResponse.NetworkError -> break
                is NetworkResponse.Reject -> return res
                else -> continue
            }
        }
        return Success(EmptyPayload())
    }

    /**
     * Gets basic info about a chat participated.
     *
     * * NOTE: this method will not trigger chat list loading.
     */
    fun getParticipatedChat(chatId: ChatId) = chatDao.loadChat(chatId)

    /**
     * Gets the display info for a chat.
     *
     * A null value indicates loading.
     */
    fun getChatDisplay(currentUserId: UserId, chat: Chat): Flow<CommonListItemData?> {
        return messageDao.loadLatestMessage(chat.id).flatMapLatest { msg ->
            when (chat.type) {
                ChatType.CHAT_PRIVATE -> {
                    getOtherUserId(
                        currentUserId,
                        chat.id
                    ).map { it.data }.filterNotNull().flatMapLatest {
                        it.let { userId ->
                            userRepository.getUserWithFriendDisplay(userId, true)
                                .map { display -> display.copy(description = digest(msg)) }
                        }
                    }
                }
                else -> {
                    if (msg == null) {
                        MutableStateFlow(
                            CommonListItemData(
                                avatar = chat.avatar,
                                displayName = chat.name,
                                description = digest(msg),
                            )
                        )
                    } else {
                        userRepository.getUserWithFriendDisplay(msg.senderId, true)
                            .map {
                                val digestMsg = it.let {
                                    getString(R.string.message_digest).format(
                                        it.displayName,
                                        digest(msg),
                                    )
                                }
                                CommonListItemData(
                                    avatar = chat.avatar,
                                    displayName = chat.name,
                                    description = digestMsg
                                )
                            }
                    }
                }
            }
        }
    }

    fun digest(messageType: MessageType?, content: String?): String {
        return when (messageType) {
            MessageType.TEXT -> content?.substring(0, min(content.length, MESSAGE_DIGEST_LENGTH))
                ?: ""
            MessageType.IMAGE -> "[${getString(R.string.message_type_image)}]"
            MessageType.AUDIO -> "[${getString(R.string.message_type_audio)}]"
            MessageType.VIDEO -> "[${getString(R.string.message_type_video)}]"
            MessageType.LOCATION -> "[${getString(R.string.message_type_location)}]"
            MessageType.UNKNOWN -> "[${getString(R.string.message_type_unknown)}]"
            MessageType.WITHDRAW -> "[${getString(R.string.message_type_withdraw)}]"
            MessageType.DELETED -> ""
            null -> ""
        }
    }

    fun digest(message: Message?): String {
        return digest(message?.type?.asMessageType(), message?.message)
    }

    /**
     * Gets the latest message of a chat.
     */
    fun getLatestMessage(chatId: ChatId): Flow<Message?> {
        return messageDao.loadLatestMessage(chatId)
    }

    /**
     * Gets unread count for a chat.
     */
    fun unreadCount(chatId: ChatId): Flow<Long> {
        return chatDao.loadChatLastRead(chatId)
            .combine(messageDao.loadLatestMessage(chatId)) { read, msg ->
                msg?.let { msg.messageId - (read ?: 0) } ?: 0
            }
    }

    /**
     * Updates the last read message id for a chat.
     */
    suspend fun updateLastRead(chatId: ChatId, messageId: MessageId) {
        chatDao.updateChatLastRead(chatId, messageId)
    }

    fun getLastRead(chatId: ChatId): Flow<MessageId?> = chatDao.loadChatLastRead(chatId)

    fun createMessageDataSource(chatId: ChatId, pageSize: Long): MessageDataSource {
        return MessageDataSource(chatId, pageSize)
    }

    suspend fun checkNeedLoadingMessageRange(
        chatId: ChatId, start: MessageId, end: MessageId,
    ): List<Pair<MessageId, MessageId>> {
        if (start > end) {
            return listOf()
        }
        val messageList =
            messageDao.loadMessagesBetweenOnce(chatId, start, end).sortedBy { it.messageId }
        val pairs = arrayListOf<Pair<MessageId, MessageId>>()
        var last = start
        for (message in messageList) {
            if (message.messageId != last) {
                pairs.add(last to message.messageId - 1)
            }
            last = message.messageId + 1
        }
        if (last < end) {
            pairs.add(last to end)
        }
        Log.d(LOG_TAG, "requested range: $start to $end")
        Log.d(LOG_TAG, "need loading ranges: $pairs")
        return pairs
    }

    private fun messageRangeDataSource(chatId: ChatId, start: MessageId, end: MessageId) =
        object : DataSource<List<Message>>() {
            override suspend fun fetch(): NetworkResponse<List<Message>> {
                // check for empty holes
                val ranges = checkNeedLoadingMessageRange(chatId, start, end)
                val result = ArrayList<Message>()
                // load ranged messages
                for (range in ranges) {
                    val res = httpService.getRangeMessages(chatId, range.first, range.second + 1)
                    if (res !is Success) {
                        return if (result.isEmpty()) {
                            res
                        } else {
                            Success(result)
                        }
                    } else {
                        result.addAll(res.data)
                    }
                }
                return Success(result)
            }

            override suspend fun saveToDb(data: List<Message>) {
                insertMessagesAndPerformWithdraw(data)
            }

            override fun loadFromDb(): Flow<List<Message>> {
                return messageDao.loadMessagesBetween(chatId, start, end)
            }

            override fun shouldFetch(data: List<Message>?): Boolean {
                return data != null && data.size.toLong() != end - start + 1
            }
        }

//    /**
//     * Creates a paging source for messages.
//     */
//    fun createMessagePagingSource(
//        chatId: ChatId,
//    ): PagingSource<MessageRange, Message> {
//        // although this is not a good design...
//        val startingPosition = runBlocking(Dispatchers.IO) { chatDao.loadChatLastReadOnce(chatId) }
//        // invalidate paging source if new messages arrived
//        return object : PagingSource<MessageRange, Message>() {
//            override fun getRefreshKey(state: PagingState<MessageRange, Message>): MessageRange? {
//                state.anchorPosition?.let {
//                    state.closestPageToPosition(it)?.prevKey?.let {
//
//                    }
//                }
//            }
//
//            override suspend fun load(params: LoadParams<MessageRange>): LoadResult<MessageRange, Message> {
//                val latestMessage = messageDao.loadLatestMessage(chatId).first()
//                    ?: return LoadResult.Page(
//                        data = listOf(),
//                        prevKey = null,
//                        nextKey = null,
//                    )
//                val latestId = latestMessage.messageId
//                val loadSize = params.loadSize.toLong()
//                val range = params.key ?: run {
//                    val isStartingPositionValid =
//                        startingPosition != null && startingPosition <= latestId && startingPosition >= 1L
//                    // there's no need to double check (startingPosition != null), but parser disagrees
//                    if (isStartingPositionValid && startingPosition != null) {
//                        max(1L, startingPosition - loadSize) to startingPosition
//                    } else {
//                        max(1L, latestId - loadSize) to latestId
//                    }
//                }
//                val result = messageDao.loadMessagesBetweenOnce(chatId, range.first, range.second)
//                if (result.size.toLong() != range.second - range.first + 1L) {
//                    // loading for the page failed, need reload
//                }
//                val prevKey = if (range.first > 1L) {
//                    max(1L, range.first - loadSize) to range.first - 1L
//                } else {
//                    null
//                }
//                val nextKey = if (range.second < latestId) {
//                    range.second + 1L to min(range.second + loadSize, latestId)
//                } else {
//                    null
//                }
//                TODO()
//            }
//        }
//    }


    inner class MessageDataSource(val chatId: ChatId, private val pageSize: Long) {
        val lockToLatest = MutableStateFlow(false)
        private val reloadTriggerValue = AtomicLong(0L)
        private val reloadTrigger = MutableStateFlow(0L)
        private val requestedEndIndex = MutableStateFlow(0L)
        private val startIndex: MutableStateFlow<MessageId> = MutableStateFlow(0L)
        private val range = lockToLatest.flatMapLatest { lock ->
            if (lock) {
                // unload previous messages
                // destroy visibility range on scrolling to bottom
                visibilityRange.value = 0L to 0L
                getLatestMessage(chatId).filterNotNull()
                    .combine(visibilityRange) { latest, visible ->
                        latest.messageId.apply {
                            // updates start index
                            if (visible.first != 0L) {
                                val maxStart = max(visible.first - pageSize, 1L)
                                if (startIndex.value > maxStart) {
                                    // doubling size to prevent entering this function once and once again
                                    startIndex.value = max(visible.first - 2 * pageSize, 1L)
                                    Log.d(LOG_TAG,
                                        "(1) expand to ${startIndex.value}, because oldest visible is ${visible.first}")
                                }
                            }
                            // updates end index
                            requestedEndIndex.value = this
                        }
                    }.combine(startIndex) { endIdx, startIdx ->
                        startIdx to endIdx
                    }
            } else {
                combine(requestedEndIndex, startIndex, visibilityRange, getLatestMessage(chatId))
                { endIdx, startIdx, visible, latest ->
                    val maxStart = max(visible.first - pageSize, 1L)
                    if (visible.first != 0L) {
                        if (startIndex.value > maxStart) {
                            // doubling size to prevent entering this function once and once again
                            startIndex.value = max(visible.first - 2 * pageSize, 1L)
                            Log.d(LOG_TAG,
                                "(2) expand to ${startIndex.value}, because oldest visible is ${visible.first}")
                        }
                        if (latest != null) {
                            val minEnd = min(visible.second + pageSize, latest.messageId)
                            if (requestedEndIndex.value < minEnd) {
                                requestedEndIndex.value =
                                    min(visible.second + 2 * pageSize, latest.messageId)
                            }
                            Log.d(LOG_TAG,
                                "(2) expand to ${requestedEndIndex.value}, because latest visible is ${visible.second}")
                        }
                    }
                    startIdx to endIdx
                }
            }
        }
        private val visibilityRange = MutableStateFlow(0L to 0L)

        @MainThread
        fun loadMorePrevious(count: Long) {
            startIndex.apply {
                val prevStart = value
                val newStart = max(prevStart - count, 0L)
                if (newStart != prevStart) {
                    value = newStart
                }
            }
        }

        @MainThread
        fun loadMoreNext(count: Long) {
            if (!lockToLatest.value) {
                requestedEndIndex.value += count
            }
        }

        @MainThread
        fun setLockToLatest(lock: Boolean) {
            if (!lockToLatest.value) {
                lockToLatest.value = lock
                startIndex.value = max(1L, startIndex.value - pageSize * 2)
            }
        }

        fun triggerReload() {
            reloadTrigger.value = reloadTriggerValue.incrementAndGet()
        }

        @MainThread
        fun informVisibleRange(start: MessageId, end: MessageId) {
            visibilityRange.value = start to end
        }

        /**
         * Gets message List as a flow.
         * All messages returned are promised to be sorted by message id ascending
         */
        val messageFlow = flow {
            val lastRead = (chatDao.loadChatLastReadOnce(chatId) ?: 0L) + pageSize
            val latestMsg = getLatestMessage(chatId).first()?.messageId ?: 0L
            val lastIndexV = if (lastRead < latestMsg) lastRead else latestMsg
            startIndex.value = max(lastIndexV - pageSize * 2, 0L)
            requestedEndIndex.value = lastIndexV
            if (latestMsg == lastIndexV) {
                lockToLatest.value = true
            }
            /**
             * When requested range changed, reflect it to [range].
             *
             * When [range] changed
             */
            this.emitAll(reloadTrigger.flatMapLatest {
                range.distinctUntilChanged().flatMapLatest { rangePair ->
                    val (start, end) = rangePair
                    Log.d(this@MessageRepository.LOG_TAG, "set range to $rangePair")
                    messageRangeDataSource(chatId, max(1L, start), end).flow()
//                    .map { it.data }
//                    .filterNotNull()
//                    .map { it.sortedBy { msg -> msg.id } }
                }
            })
        }
    }

    companion object {
        // * this value should always be greater than or equals to 1
        private val PendingMessageIndex = AtomicInteger(1)
        private val UpdateMark = AtomicInteger(0)
    }
}

typealias MessageRange = Pair<MessageId, MessageId>
