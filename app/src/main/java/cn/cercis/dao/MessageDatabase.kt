package cn.cercis.dao

import androidx.paging.PagingSource
import androidx.room.*
import cn.cercis.common.ChatId
import cn.cercis.common.MessageId
import cn.cercis.common.UserId
import cn.cercis.entity.*
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [Message::class, Chat::class, ChatMember::class, ChatLastRead::class],
    version = 1,
    exportSchema = false
)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun chatDao(): ChatDao
    abstract fun chatMemberDao(): ChatMemberDao
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertMessage(vararg messages: Message)

    /**
     * Inserts some messages and replace on conflict. This method is used to mark a message to be
     * deleted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAndReplaceMessage(vararg messages: Message)

    @Transaction
    fun insertIgnoreAndInsertReplace(
        insertIgnoreMessages: List<Message>,
        insertReplaceMessages: List<Message>,
    ) {
        insertMessage(*insertIgnoreMessages.toTypedArray())
        insertAndReplaceMessage(*insertReplaceMessages.toTypedArray())
    }

    @Delete
    fun deleteMessage(vararg messages: Message)

    @Query("SELECT * FROM message WHERE chatId = :chatId AND messageId = :messageId")
    fun loadSingleMessage(chatId: ChatId, messageId: MessageId): Flow<Message?>

    @Query("SELECT * FROM message WHERE chatId = :chatId AND messageId >= :messageId")
    fun loadChatMessagesNewerThan(chatId: ChatId, messageId: MessageId): Flow<List<Message>>

    @Query("SELECT * FROM message WHERE chatId = :chatId ORDER BY messageId")
    fun loadChatAllMessages(chatId: ChatId): PagingSource<Int, Message>

    @Query("SELECT COUNT(*) FROM message WHERE chatId = :chatId AND messageId >= :start AND messageId <= :end")
    suspend fun countMessagesBetween(chatId: ChatId, start: MessageId, end: MessageId): Long

    @Query("SELECT * FROM message WHERE chatId = :chatId AND messageId >= :start AND messageId <= :end ORDER BY messageId")
    suspend fun loadMessagesBetweenOnce(
        chatId: ChatId,
        start: MessageId,
        end: MessageId,
    ): List<Message>

    @Query("SELECT COUNT(*) FROM message WHERE chatId = :chatId AND messageId >= :start AND messageId <= :end")
    suspend fun countMessagesBetweenOnce(
        chatId: ChatId,
        start: MessageId,
        end: MessageId,
    ): Long

    @Query("SELECT * FROM message WHERE chatId = :chatId AND messageId >= :start AND messageId <= :end ORDER BY messageId")
    fun loadMessagesBetween(chatId: ChatId, start: MessageId, end: MessageId): Flow<List<Message>>

    @Query("SELECT * FROM message WHERE chatId = :chatId ORDER BY messageId DESC LIMIT 1")
    fun loadLatestMessage(chatId: ChatId): Flow<Message?>

    @Query("SELECT chatId, MAX(messageId) AS messageId FROM message GROUP BY chatId")
    suspend fun loadAllChatLatestMessagesOnce(): List<ChatIdMessageId>

    @Query("SELECT * FROM message NATURAL JOIN (SELECT chatId, MAX(messageId) AS messageId FROM message GROUP BY chatId)")
    fun loadAllChatLatestMessages(): Flow<List<Message>>

    @Query("""SELECT 
            chat.id AS chatId, 
            chat.type AS chatType,
            chat.name AS name,
            chat.avatar AS avatar,
            chat.createdAt AS chatCreatedAt,
            chat.updatedAt AS chatUpdatedAt,
            latestMsg.messageId AS messageId,
            latestMsg.message AS message,
            latestMsg.type AS messageType,
            latestMsg.senderId AS senderId,
            latestMsg.createdAt AS messageCreatedAt,
            latestMsg.updatedAt AS messageUpdatedAt,
            IFNULL(latestMsg.updatedAt, chat.createdAt) AS lastUpdate FROM chat
        LEFT OUTER JOIN (
            SELECT chatId AS maxChatId, MAX(messageId) AS maxMsgId FROM message GROUP BY chatId
        ) ON chat.id = maxChatId
        LEFT OUTER JOIN message AS latestMsg ON messageId = maxMsgId AND chatId = chat.id
        ORDER BY lastUpdate DESC
    """)
    fun loadAllChatsOrderedByUpdateOrLatestMessage(): Flow<List<ChatWithLatestMessage>>
}

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChat(vararg chats: Chat)

    @Delete
    fun deleteChat(vararg chats: Chat)

    @Query("SELECT * FROM chat WHERE id = :chatId")
    fun getChat(chatId: ChatId): Flow<Chat?>

    @Query("SELECT * FROM chat")
    fun loadAllChats(): Flow<List<Chat>>

    @Query("DELETE FROM chat")
    fun deleteAllChats()

    @Transaction
    fun updateAllChats(chats: List<Chat>) {
        deleteAllChats()
        insertChat(*chats.toTypedArray())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun uncheckedInsertChatLastRead(chatLastRead: ChatLastRead)

    @Transaction
    suspend fun updateChatLastRead(chatId: ChatId, messageId: MessageId) {
        val lastRead = loadChatLastReadOnce(chatId)
        if (lastRead == null || lastRead < messageId) {
            uncheckedInsertChatLastRead(ChatLastRead(chatId, messageId))
        }
    }

    @Query("SELECT lastReadMessageId FROM chatLastRead WHERE chatId = :chatId")
    suspend fun loadChatLastReadOnce(chatId: ChatId): MessageId?

    @Query("SELECT lastReadMessageId FROM chatLastRead WHERE chatId = :chatId")
    fun loadChatLastRead(chatId: ChatId): Flow<MessageId?>
}

@Dao
interface ChatMemberDao {
    @Query("DELETE FROM chatMember WHERE chatId = :chatId")
    fun deleteAllMembersFrom(chatId: ChatId)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMembers(vararg chatMembers: ChatMember)

    @Query("SELECT * FROM chatMember WHERE chatId = :chatId")
    fun loadChatMembers(chatId: ChatId): Flow<List<ChatMember>>

    @Query("SELECT id, type, name, avatar, createdAt, updatedAt FROM chatMember AS c1 JOIN chatMember AS c2 JOIN chat ON c1.chatId = c2.chatId AND c1.chatId = chat.id WHERE c1.userId = :userId1 AND c2.userId = :userId2")
    fun loadSharedChats(userId1: UserId, userId2: UserId): Flow<Chat?>

    @Transaction
    fun updateChatMemberList(chatId: ChatId, chatMembers: List<ChatMember>) {
        deleteAllMembersFrom(chatId)
        insertMembers(*chatMembers.toTypedArray())
    }
}
