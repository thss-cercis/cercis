package cn.cercis.service

import cn.cercis.common.ChatId
import cn.cercis.common.MessageId
import cn.cercis.common.SerialId
import cn.cercis.common.UserId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable

/**
 * TODO May change due to protocol change.
 */
interface CercisWebSocketService {
    @Send
    fun sendInitMessage(initMessageFromClient: InitMessageFromClient)

    @Send
    fun sendSendMessageRequestMessage(sendMessageRequestMessage: SendMessageRequestMessage)

    @Receive
    fun receiveInitMessage(): Flowable<InitMessageFromServer>

    @Receive
    fun observeWebSocketEvent(): Flowable<WebSocket.Event>

    @Receive
    fun receiveChatsUpdate(): Flowable<ChatsUpdateMessage>

    @Receive
    fun receiveSendMessageResponseMessage(): Flowable<SendMessageResponseMessage>
}

@JsonClass(generateAdapter = true)
data class InitMessageFromServer(
    @Json(name = "friends") val friends: List<InitMessageFromServerFriend>,
    @Json(name = "groups") val groups: List<InitMessageFromServerGroup>,
)

@JsonClass(generateAdapter = true)
data class InitMessageFromServerFriend(
    @Json(name = "user_id") val userId: UserId,
    @Json(name = "chat_id") val chatId: ChatId,
    @Json(name = "alias") val alias: String,
)

@JsonClass(generateAdapter = true)
data class InitMessageFromServerGroup(
    @Json(name = "id") val chatId: ChatId,
)

@JsonClass(generateAdapter = true)
data class InitMessageFromClient(
    @Json(name = "latest") val latestMessageId: MessageId,
)

@JsonClass(generateAdapter = true)
data class ChatsUpdateMessage(
    @Json(name = "type") val type: String,
    @Json(name = "chats") val chats: List<ChatsUpdateMessageChat>,
)

@JsonClass(generateAdapter = true)
data class ChatsUpdateMessageChat(
    @Json(name = "id") val chatId: ChatId,
    @Json(name = "messages") val messages: List<ChatUpdateMessage>,
)

@JsonClass(generateAdapter = true)
data class ChatUpdateMessage(
    @Json(name = "id") val messageId: MessageId,
    @Json(name = "type") val type: String,
    @Json(name = "content") val content: String,
)

@JsonClass(generateAdapter = true)
data class SendMessageRequestMessage(
    /**
     * Client generated serial number.
     * Used to receive corresponding response.
     */
    @Json(name = "serial") val serial: SerialId,
    @Json(name = "chat_id") val chatId: ChatId,
    @Json(name = "type") val type: String,
    @Json(name = "content") val content: String,
)

@JsonClass(generateAdapter = true)
data class SendMessageResponseMessage(
    @Json(name = "serial") val serial: SerialId,
    @Json(name = "message_id") val messageId: MessageId,
)
