package cn.cercis.repository

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import cn.cercis.R
import cn.cercis.common.*
import cn.cercis.dao.ChatDao
import cn.cercis.dao.ChatMemberDao
import cn.cercis.dao.MessageDao
import cn.cercis.entity.*
import cn.cercis.http.CercisHttpService
import cn.cercis.http.CreatePrivateChatRequest
import cn.cercis.http.GetChatsLatestMessagesRequest
import cn.cercis.util.getString
import cn.cercis.util.resource.DataSource
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.resource.Resource
import cn.cercis.viewmodel.CommonListItemData
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
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
    @ApplicationContext val context: Context,
) {
    /**
     * This field keeps the oldest liable messageId for each chat. Not persisted.
     *
     * The field helps the repo to decide whether an attempt to fetch chat history is necessary.
     */
    private val liableOldestMessageId = ConcurrentHashMap<Long, Long>()

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

    /**
     * Gets the most recent messages that is newer than messageId.
     */
    fun getChatMessagesNewerThan(chatId: ChatId, messageId: MessageId): Flow<List<Message>> {
        return messageDao.loadChatMessagesNewerThan(chatId, messageId)
    }

    fun debugInsertAllChats(chatList: List<Chat>) {
        chatDao.updateAllChats(chatList)
    }

    /**
     * Populates messages into the db and replace deleted messages with special ones.
     */
    private fun insertMessagesAndPerformWithdraw(messages: List<Message>) {
        val withdrawedMessages = messages.filter {
            it.type == MessageType.WITHDRAW.type
        }.map {
            Message(
                id = it.id,
                chatId = it.chatId,
                type = MessageType.DELETED.type,
                content = "",
                senderId = it.senderId,
            )
        }
        messageDao.insertIgnoreAndInsertReplace(
            messages,
            withdrawedMessages
        )
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

    /**
     * Gets all chat message.
     */
    fun getChatAllMessages(chatId: ChatId): Flow<List<Message>> {
        return messageDao.loadChatAllMessages(chatId)
    }

    /**
     * Requests for `count` more messages and gets a new newer than live data.
     *
     * This method returns a live data giving both messages not older than `messageId` and
     * `previousCount` messages that are older than `messageId`.
     *
     * * NOTE: if no Internet connection is available, previous messages will be given via local
     * cache.
     *
     * @param chatId Chat ID
     * @param messageId Message ID as a baseline
     * @param previousCount loads [previousCount] more messages before messageId
     */
    suspend fun getChatMessagesNewerThanWithPreviousMessages(
        chatId: ChatId,
        messageId: MessageId,
        previousCount: Long,
    ): LiveData<List<Message>> {
        TODO("finish this")
    }

    suspend fun getChatRecentMessages(
        chatId: ChatId,
        previousCount: Long,
    ): LiveData<List<Message>> {
        TODO("finish this")
    }

    /**
     * Gets chat with another user.
     */
    suspend fun getPrivateChatWith(userId: UserId, otherId: UserId) = object : DataSource<Chat>() {
        override suspend fun fetch(): NetworkResponse<Chat> {
            val chat = httpService.getPrivateChatWith(otherId)
            if (chat is NetworkResponse.Success) {
                // save member list
                Log.d(LOG_TAG, "private chat got. loading chat members.")
                // no need to double-check a private chat's members, so  no force fetch
                return getChatMemberList(chat.data.id, false).fetchAndSave().use { chat.data }
            } else if (chat is NetworkResponse.Reject) {
                // create new private chat if non is given
                return httpService.createPrivateChat(CreatePrivateChatRequest(otherId))
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
        getChatMemberList(chatId, false).flow().map { member ->
            member.data?.firstOrNull { it.userId != selfUserId }?.userId?.let {
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
        if (chatLatestMessageIdRes !is NetworkResponse.Success) {
            return chatLatestMessageIdRes
        }
        val allLatest = messageDao.loadAllChatLatestMessages().map { it.id }.toSet()
        Log.d(LOG_TAG, "latestMessages: $allLatest")
        val shouldFetch = chatLatestMessageIdRes.data.filter { it.latestMessageId !in allLatest }
        Log.d(LOG_TAG, "shouldFetch: $shouldFetch")
        return httpService.getChatsLatestMessages(GetChatsLatestMessagesRequest(shouldFetch.map { it.chatId }))
            .apply {
                if (this is NetworkResponse.Success) {
                    insertMessagesAndPerformWithdraw(data)
                }
            }
    }

    /**
     * Gets basic info about a chat participated.
     *
     * * NOTE: this method will not trigger chat list loading.
     */
    fun getParticipatedChat(chatId: ChatId) = chatDao.getChat(chatId)

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
                    ).flatMapLatest {
                        Log.d(LOG_TAG, "receiving other's user id: $it")
                        it.data?.let { userId ->
                            userRepository.getUserWithFriendDisplay(userId, true)
                                .map { it?.copy(description = digest(msg)) }
                        } ?: MutableStateFlow(null)
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
                                val digestMsg = it?.let {
                                    getString(R.string.message_digest).format(
                                        it.displayName,
                                        digest(msg),
                                    )
                                } ?: digest(msg)
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

    private fun digest(message: Message?): String {
        return when (message?.type?.asMessageType()) {
            MessageType.TEXT -> message.content.substring(0, MESSAGE_DIGEST_LENGTH)
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
                msg?.let { msg.id - (read ?: 0) } ?: 0
            }
    }

    /**
     * Updates the last read message id for a chat.
     */
    suspend fun updateLastRead(chatId: ChatId, messageId: MessageId) {
        chatDao.updateChatLastRead(ChatLastRead(chatId, messageId))
    }

    fun createMessageDataSource(chatId: ChatId, pageSize: Long): MessageDataSource {
        return MessageDataSource(chatId, pageSize)
    }

    suspend fun checkNeedLoadingMessageRange(
        chatId: ChatId, start: MessageId, end: MessageId
    ): List<Pair<MessageId, MessageId>> {
        val messageList = messageDao.loadMessagesBetweenOnce(chatId, start, end).sortedBy { it.id }
        val pairs = arrayListOf<Pair<MessageId, MessageId>>()
        var last = start
        for (message in messageList) {
            if (message.id != last) {
                pairs.add(last to message.id - 1)
            }
            last = message.id + 1
        }
        if (last != end) {
            pairs.add(last to end)
        }
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
                    val res = httpService.getRangeMessages(chatId, range.first, range.second)
                    if (res !is NetworkResponse.Success) {
                        return if (result.isEmpty()) {
                            res
                        } else {
                            NetworkResponse.Success(result)
                        }
                    } else {
                        result.addAll(res.data)
                    }
                }
                return NetworkResponse.Success(result)
            }

            override suspend fun saveToDb(data: List<Message>) {
                insertMessagesAndPerformWithdraw(data)
            }

            override fun loadFromDb(): Flow<List<Message>> {
                return messageDao.loadMessagesBetween(chatId, start, end)
            }
        }

    inner class MessageDataSource(val chatId: ChatId, private val pageSize: Long) {
        val lockToLatest = MutableStateFlow(false)
        private val requestedEndIndex = MutableStateFlow(0L)
        private val startIndex: MutableStateFlow<MessageId> = MutableStateFlow(0L)
        private val range = lockToLatest.flatMapLatest { lock ->
            if (lock) {
                // unload previous messages
                var start0 = startIndex.value
                getLatestMessage(chatId).filterNotNull().map {
                    it.id.apply {
                        // updates start index
                        if (startIndex.value == start0) {
                            start0 = -1L
                            startIndex.value = max(0L, this - pageSize * 2)
                        }
                        // updates end index
                        requestedEndIndex.value = this
                    }
                }.combine(startIndex) { endIdx, startIdx ->
                    startIdx to endIdx
                }
            } else {
                requestedEndIndex.combine(startIndex) { endIdx, startIdx ->
                    startIdx to endIdx
                }
            }
        }

        @MainThread
        fun loadMorePrevious(count: Long) {
            startIndex.apply {
                val prevStart = value
                val newStart = max(prevStart - count, 0L)
                value = newStart
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
            lockToLatest.value = lock
        }

        val messageFlow = flow {
            val lastRead = chatDao.loadChatLastReadOnce(chatId)
            val latestMsg = getLatestMessage(chatId).first()?.id
            val lastIndexV = min(lastRead ?: 0L, latestMsg ?: 0L)
            startIndex.value = max(lastIndexV - pageSize + 1, 0L)
            requestedEndIndex.value = lastIndexV
            this.emitAll(range.distinctUntilChanged().flatMapLatest { rangePair ->
                val (start, end) = rangePair
                messageRangeDataSource(chatId, start, end).flow()
                    .map { it.data }
                    .filterNotNull()
                    .map { it.sortedBy { msg -> msg.id } }
            })
        }
    }
}
