package cn.cercis.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cn.cercis.common.ChatId
import cn.cercis.common.MessageId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(
        foreignKeys = [ForeignKey(
                entity = Chat::class,
                parentColumns = ["id"],
                childColumns = ["chatId"],
                onDelete = ForeignKey.CASCADE,
        )],
        indices = [Index("chatId")]
)
@JsonClass(generateAdapter = true)
data class Message(
    @PrimaryKey val id: MessageId,
    @Json(name = "chat_id") val chatId: ChatId,
    @Json(name = "type") val type: String,
    @Json(name = "content") val content: String,
    @Json(name = "sender_id") var senderId: Long,
)
