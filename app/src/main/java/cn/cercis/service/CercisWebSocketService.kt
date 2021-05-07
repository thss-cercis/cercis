package cn.cercis.service

import cn.cercis.common.ApplyId
import cn.cercis.common.WSMessageTypeId
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
) {
    fun get(): WSMessage? {
        when (type) {
            100L -> apply
            101L -> WSMessage.FriendListUpdated
        }
        return null
    }
}

sealed class WSMessage {
    // 100
    @JsonClass(generateAdapter = true)
    data class FriendRequestReceived(
        @Json(name = "apply_id") val applyId: ApplyId,
        val nickname: String,
    ) : WSMessage()

    // 101
    object FriendListUpdated : WSMessage()

    // TODO future types
}
