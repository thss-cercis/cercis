package cn.edu.tsinghua.thss.cercis.repository

import androidx.lifecycle.MutableLiveData
import cn.edu.tsinghua.thss.cercis.entity.Message
import cn.edu.tsinghua.thss.cercis.dao.MessageDao
import cn.edu.tsinghua.thss.cercis.service.MessageService
import cn.edu.tsinghua.thss.cercis.util.ChatId
import cn.edu.tsinghua.thss.cercis.util.MessageId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MessageRepository @Inject constructor(
        val messageDao: MessageDao
) {
    val connectionStatus = MutableLiveData(MessageService.ConnectionStatus.DISCONNECTED)

    fun getChatRecentMessages(chatId: ChatId, messageCount: Long): Flow<List<Message>> {
        return messageDao.getChatRecentMessages(chatId, messageCount)
    }

    fun getChatMessagesNewerThan(chatId: ChatId, messageId: MessageId): Flow<List<Message>> {
        return messageDao.getChatMessagesNewerThan(chatId, messageId)
    }

    fun getChatAllMessages(chatId: ChatId): Flow<List<Message>> {
        return messageDao.getChatAllMessages(chatId)
    }

    fun submitMessage(vararg messages: Message) {
        messageDao.insertMessage(*messages)
    }

    fun submitConnectionStatus(connectionStatus: MessageService.ConnectionStatus) {
        this.connectionStatus.postValue(connectionStatus)
    }
}