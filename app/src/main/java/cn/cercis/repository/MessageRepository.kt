package cn.cercis.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cn.cercis.common.ChatId
import cn.cercis.common.MessageId
import cn.cercis.common.UserId
import cn.cercis.dao.ChatDao
import cn.cercis.dao.ChatMemberDao
import cn.cercis.dao.MessageDao
import cn.cercis.entity.*
import cn.cercis.http.CercisHttpService
import cn.cercis.service.NotificationService
import cn.cercis.util.resource.DataSource
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.resource.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.math.max

@FlowPreview
@ExperimentalCoroutinesApi
@ActivityRetainedScoped
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val chatMemberDao: ChatMemberDao,
    private val httpService: CercisHttpService,
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

    fun getSingleMessage(chatId: ChatId, messageId: MessageId) = object : DataSource<Message>() {
        override suspend fun fetch(): NetworkResponse<Message> {
            return httpService.getSingleMessage(chatId, messageId)
        }

        override suspend fun saveToDb(data: Message) {
            messageDao.insertMessage(data)
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
    suspend fun getPrivateChatWith(userId: UserId) = httpService.getPrivateChatWith(userId)

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

    /**
     * Gets basic info about a chat participated.
     *
     * * NOTE: this method will not trigger chat list loading.
     */
    fun getParticipatedChat(chatId: ChatId) = chatDao.getChat(chatId)

    /**
     * Gets participants of a chat.
     */
    fun getChatParticipants(chatId: ChatId): LiveData<List<User>> {
        return MutableLiveData(ArrayList())
    }

    /**
     * Inserts a message into database.
     *
     * Called from [NotificationService]
     */
    fun submitMessage(vararg messages: Message) {
        messageDao.insertMessage(*messages)
    }

    /**
     * Gets the latest message of a chat.
     *
     * * NOTE: if no message is present for the chat, a null will be emitted.
     */
    fun getLatestMessage(chatId: ChatId): Flow<Message?> {
        return messageDao.loadLatestMessage(chatId)
    }


    /**
     * Gets latest [messageCount] messages from the chat. If new messages arrived, the returned list
     * will still start from the previous starting position.
     */
    fun getChatLatestNMessages(chatId: ChatId, messageCount: Long): Flow<List<Message>> = flow {
        val startMessageId = messageDao.loadLatestMessage(chatId).first()?.let {
            max(0L, it.id - messageCount + 1)
        } ?: 0L
        emitAll(messageDao.loadChatMessagesNewerThan(chatId, startMessageId))
    }

    /**
     * Informs the service to fetch new messages.
     */
    private fun informFetchPreviousMessages(
        chatId: ChatId,
        messageId: MessageId,
        previousCount: Long,
    ) {
        TODO("Finish this")
    }
}
