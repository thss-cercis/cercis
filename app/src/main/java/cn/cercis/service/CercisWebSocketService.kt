package cn.cercis.service

import cn.cercis.common.*
import cn.cercis.entity.Chat
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import kotlinx.coroutines.channels.ReceiveChannel

/**
 * TODO May change due to protocol change.
 */
interface CercisWebSocketService {
    @Receive
    fun observeWebSocketEvent(): ReceiveChannel<WebSocket.Event>

    @Receive
    fun observeWebSocketMessage(): ReceiveChannel<NotificationMessage>
}

@JsonClass(generateAdapter = true)
data class DebugMessage(
    @Json(name = "Msg") val msg: String,
)

/**
 * This is a joint message class for all types.
 */
@JsonClass(generateAdapter = true)
data class NotificationMessage(
    val type: WSMessageTypeId,
    val apply: WSMessage.FriendRequestReceived?,
    val msg: WSMessage.NewMessageReceived?,
) {
    fun get(): WSMessage? {
        when (type) {
            100L -> apply
            101L -> WSMessage.FriendListUpdated
            200L -> msg
        }
        return null
    }
}

sealed interface WSMessageWithId {
    val typeId: WSMessageTypeId

    /**
     * Implementing this interface indicates that messages of this same type id can be combined.
     */
    interface Combinable : WSMessageWithId
}

sealed class WSMessage(override val typeId: WSMessageTypeId) : WSMessageWithId {
    // internal messages
    object WebSocketConnected : WSMessage(-1L)

    // 100
    @JsonClass(generateAdapter = true)
    data class FriendRequestReceived(
        @Json(name = "apply_id") val applyId: ApplyId,
        val nickname: String,
    ) : WSMessage(100L), WSMessageWithId.Combinable

    // 101
    object FriendListUpdated : WSMessage(101L), WSMessageWithId.Combinable

    // 200
    @JsonClass(generateAdapter = true)
    data class NewMessageReceived(
        @Json(name = "chat_id") val chatId: ChatId,
        @Json(name = "msg_id") val messageId: MessageId,
        @Json(name = "type") val type: WSMessageTypeId,
        @Json(name = "sum") val sum: String,
    ) : WSMessage(200L)

    // 300
    @JsonClass(generateAdapter = true)
    data class NewActivity(
        @Json(name = "activity_id") val activityId: ActivityId,
        @Json(name = "user_id") val userId: UserId,
    ) : WSMessage(300L)
}
