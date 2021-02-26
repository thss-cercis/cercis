package cn.edu.tsinghua.thss.cercis.service

import com.fasterxml.jackson.annotation.JsonProperty
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

data class ChatLatestStatus(
        @JsonProperty("id") val id: Int,
        @JsonProperty("latest") val latest: Int
)

data class InitMessage(
        @JsonProperty("chats", required = true) val chats: List<ChatLatestStatus>
)

data class ChatUpdateMessage(
        @JsonProperty("id", required = true) val id: String,
        @JsonProperty("type", required = true) val type: String,
        @JsonProperty("content", required = true) val content: String
)

data class ChatUpdate(
        @JsonProperty("id") val id: Int,
        @JsonProperty("messages", required = true) val messages: List<ChatUpdateMessage>
)

data class Update(
        @JsonProperty("type", required = true) val type: String,
        @JsonProperty("chats", required = false) val chats: List<ChatUpdate>?
)