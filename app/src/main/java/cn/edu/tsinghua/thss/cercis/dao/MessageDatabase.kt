package cn.edu.tsinghua.thss.cercis.dao

import androidx.room.*
import cn.edu.tsinghua.thss.cercis.entity.Chat
import cn.edu.tsinghua.thss.cercis.entity.Message
import cn.edu.tsinghua.thss.cercis.util.ChatId
import cn.edu.tsinghua.thss.cercis.util.MessageId
import kotlinx.coroutines.flow.Flow

@Database(entities = [Message::class, Chat::class], version = 1, exportSchema = false)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun MessageDao(): MessageDao
    abstract fun ChatDao(): ChatDao
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(vararg messages: Message)

    @Delete
    fun deleteMessage(vararg messages: Message)

    @Query("SELECT * FROM message WHERE chatId = :chatId AND id >= :messageId")
    fun getChatMessagesNewerThan(chatId: ChatId, messageId: MessageId): Flow<List<Message>>

    @Query("SELECT * FROM message WHERE chatId = :chatId")
    fun getChatAllMessages(chatId: ChatId): Flow<List<Message>>

    @Query("SELECT COUNT(*) FROM message WHERE chatId = :chatId AND id >= :start AND id <= :end")
    suspend fun countMessagesBetween(chatId: ChatId, start: MessageId, end: MessageId): Long
}

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChat(vararg chats: Chat)

    @Delete
    fun deleteChat(vararg chats: Chat)

    @Query("SELECT * FROM chat WHERE id = :chatId")
    fun getChat(chatId: ChatId): Flow<Chat?>

    @Query("SELECT * FROM chat ORDER BY id DESC")
    fun loadAllChats(): Flow<List<Chat>>
}
