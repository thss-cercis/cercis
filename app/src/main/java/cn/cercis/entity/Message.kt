package cn.cercis.entity

import androidx.room.Entity
import androidx.room.Index
import cn.cercis.R
import cn.cercis.SelectedLocation
import cn.cercis.common.ChatId
import cn.cercis.common.MessageId
import cn.cercis.common.Timestamp
import cn.cercis.common.WSMessageTypeId
import cn.cercis.util.getString
import cn.cercis.util.helper.TimeString
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(
    primaryKeys = ["messageId", "chatId"],
    indices = [Index("chatId")]
)
@JsonClass(generateAdapter = true)
data class Message(
    @Json(name = "message_id") val messageId: MessageId,
    @Json(name = "chat_id") val chatId: ChatId,
    @Json(name = "type") val type: Int,
    @Json(name = "message") val message: String,
    @Json(name = "sender_id") val senderId: Long,
    @Json(name = "created_at") @TimeString val createdAt: Timestamp,
    @Json(name = "updated_at") @TimeString val updatedAt: Timestamp,
)

data class ChatIdMessageId(
    val chatId: ChatId,
    val messageId: MessageId,
)

/**
 * Message type.
 *
 * ! type constant should be representable with 28-bit signed integer
 */
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
