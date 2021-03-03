package cn.edu.tsinghua.thss.cercis.dao

import androidx.room.*
import cn.edu.tsinghua.thss.cercis.entity.Message
import cn.edu.tsinghua.thss.cercis.util.ChatId
import cn.edu.tsinghua.thss.cercis.util.MessageId
import kotlinx.coroutines.flow.Flow

@Database(entities = [Message::class], version = 1, exportSchema = false)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun MessageDao(): MessageDao
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(vararg messages: Message)

    @Delete
    fun deleteMessage(vararg messages: Message)

    @Query("SELECT * FROM message WHERE chatId = :chatId ORDER BY id DESC LIMIT :messageCount")
    fun getChatRecentMessages(chatId: ChatId, messageCount: Long): Flow<List<Message>>

    @Query("SELECT * FROM message WHERE chatId = :chatId AND id >= :messageId")
    fun getChatMessagesNewerThan(chatId: ChatId, messageId: MessageId): Flow<List<Message>>

    @Query("SELECT * FROM message WHERE chatId = :chatId")
    fun getChatAllMessages(chatId: ChatId): Flow<List<Message>>
}
