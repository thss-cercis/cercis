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
    @Json(name = "type") val type: Int,
    @Json(name = "content") val content: String,
    @Json(name = "sender_id") var senderId: Long,
)

enum class MessageType(val type: Int) {
    TEXT(0),
    IMAGE(1),
    AUDIO(2),
    VIDEO(3),
    LOCATION(4),
    WITHDRAW(100),
    UNKNOWN(-1),
    DELETED(-2),
    ;

    companion object {
        fun of(type: Int): MessageType {
            return values().firstOrNull { it.type == type } ?: UNKNOWN
        }
    }
}

fun Int.asMessageType(): MessageType = MessageType.of(this)
