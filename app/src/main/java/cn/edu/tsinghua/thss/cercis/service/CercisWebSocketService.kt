package cn.edu.tsinghua.thss.cercis.service

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
    fun sendInit(initMessage: InitMessage)

    @Receive
    fun receiveUpdate(): Flowable<Update>

    @Receive
    fun observeWebSocketEvent(): Flowable<WebSocket.Event>
}

@JsonClass(generateAdapter = true)
data class ChatLatestStatus(
        @Json(name = "id") val id: Int,
        @Json(name = "latest") val latest: Int
)

@JsonClass(generateAdapter = true)
data class InitMessage(
        @Json(name = "chats") val chats: List<ChatLatestStatus>
)

@JsonClass(generateAdapter = true)
data class ChatUpdateMessage(
        @Json(name = "id") val id: String,
        @Json(name = "type") val type: String,
        @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class ChatUpdate(
        @Json(name = "id") val id: Int,
        @Json(name = "messages") val messages: List<ChatUpdateMessage>
)

@JsonClass(generateAdapter = true)
data class Update(
        @Json(name = "type") val type: String,
        @Json(name = "chats") val chats: List<ChatUpdate>?
)