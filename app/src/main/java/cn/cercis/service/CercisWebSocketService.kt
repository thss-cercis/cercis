package cn.cercis.service

import cn.cercis.common.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

/**
 * TODO May change due to protocol change.
 */
interface CercisWebSocketService {
    @Receive
    fun observeWebSocketEvent(): ReceiveChannel<WebSocket.Event>

    @Receive
    fun observeWebSocketMessage(): ReceiveChannel<NotificationMessage>

    @Receive
    fun observeDebugMessage(): ReceiveChannel<DebugMessage>
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
    val apply: FriendRequestReceivedMessage?,
)

@JsonClass(generateAdapter = true)
data class FriendRequestReceivedMessage(
    @Json(name = "apply_id") val applyId: ApplyId,
    val nickname: String,
)

enum class MessageType(
    val typeId: WSMessageTypeId,
    val property: KProperty1<NotificationMessage, *>?
) {
    // a new friend request arrived
    FRIEND_REQUEST_RECEIVED(100, null),

    // friend list changed (friend deleted / friend added)
    FRIEND_LIST_UPDATED(101, NotificationMessage::apply),

    ;

    companion object {
        /**
         * Gets the message type from id.
         */
        fun of(type: WSMessageTypeId): MessageType? {
            return values().find { it.typeId == type }
        }
    }
}
