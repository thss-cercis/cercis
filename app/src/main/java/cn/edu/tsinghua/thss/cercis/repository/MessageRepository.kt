package cn.edu.tsinghua.thss.cercis.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cn.edu.tsinghua.thss.cercis.dao.ChatDao
import cn.edu.tsinghua.thss.cercis.entity.Message
import cn.edu.tsinghua.thss.cercis.dao.MessageDao
import cn.edu.tsinghua.thss.cercis.dao.User
import cn.edu.tsinghua.thss.cercis.entity.Chat
import cn.edu.tsinghua.thss.cercis.entity.ChatType
import cn.edu.tsinghua.thss.cercis.module.AppModule
import cn.edu.tsinghua.thss.cercis.service.MessageService
import cn.edu.tsinghua.thss.cercis.util.ChatId
import cn.edu.tsinghua.thss.cercis.util.MessageId
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
        private val messageDao: MessageDao,
        private val chatDao: ChatDao,
        @ApplicationContext val context: Context,
) {
    val connectionStatus = MutableLiveData(MessageService.ConnectionStatus.DISCONNECTED)

    /**
     * This field keeps the oldest liable messageId for each chat. Not persisted.
     *
     * The field helps the repo to decide whether an attempt to fetch chat history is necessary.
     */
    private val liableOldestMessageId = ConcurrentHashMap<Long, Long>()

    /**
     * Gets the most recent messages that is newer than messageId.
     */
    fun getChatMessagesNewerThan(chatId: ChatId, messageId: MessageId): Flow<List<Message>> {
        return messageDao.getChatMessagesNewerThan(chatId, messageId)
    }

    /**
     * Gets all chat message.
     */
    fun getChatAllMessages(chatId: ChatId): Flow<List<Message>> {
        return messageDao.getChatAllMessages(chatId)
    }

    /**
     * Requests for `count` more messages and gets a new newer than live data.
     *
     * This method returns a live data giving both messages not older than `messageId` and
     * `previousCount` messages that are older than `messageId`.
     *
     * Notice: if no Internet connection is available, previous messages will be given via local
     * cache.
     *
     * @param chatId Chat ID
     * @param messageId Message ID as a baseline
     * @param previousCount loads [previousCount] more messages before messageId
     */
    suspend fun getChatMessagesNewerThanWithPreviousMessages(chatId: ChatId, messageId: MessageId, previousCount: Long): LiveData<List<Message>> {
        TODO("finish this")
    }

    suspend fun getChatRecentMessages(chatId: ChatId, previousCount: Long): LiveData<List<Message>> {
        TODO("finish this")
    }

    /**
     * Gets basic info about a chat.
     */
    fun getChat(chatId: ChatId): LiveData<Chat> {
        return MutableLiveData(Chat(
                id = chatId,
                name = "Chat $chatId",
                type = ChatType.CHAT_SINGLE,
        ))
//        return chatDao.getChat(chatId).asLiveData()
    }

    /**
     * Gets participants of a chat.
     */
    fun getChatParticipants(chatId: ChatId): LiveData<List<User>> {
        return MutableLiveData(ArrayList())
    }

    /**
     * Insert a message into database.
     *
     * Called from [MessageService]
     */
    fun submitMessage(vararg messages: Message) {
        messageDao.insertMessage(*messages)
    }

    /**
     * Submits connection status.
     *
     * Called from [MessageService].
     */
    fun submitConnectionStatus(connectionStatus: MessageService.ConnectionStatus) {
        this.connectionStatus.postValue(connectionStatus)
    }

    /**
     * A rpc method telling if new messages should be fetched from server.
     */
    private suspend fun shouldDownloadNewMessages(chatId: ChatId, messageId: MessageId, previousCount: Long): Boolean {
        try {
            val liable = liableOldestMessageId.getOrDefault(chatId, 0)
            if (liable == 0L) {
                return true
            }
            return messageDao.countMessagesBetween(chatId, liable, messageId) + 1 < previousCount
        } catch (t: Exception) {
            return false
        }
    }

    /**
     * Informs the service to fetch new messages.
     */
    private fun informFetchPreviousMessages(chatId: ChatId, messageId: MessageId, previousCount: Long) {
        TODO("Finish this")
    }
}
