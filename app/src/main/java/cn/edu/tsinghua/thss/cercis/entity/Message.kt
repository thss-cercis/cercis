package cn.edu.tsinghua.thss.cercis.entity

import com.fasterxml.jackson.annotation.JsonProperty

data class Message(
        @JsonProperty("id") var id: Long,
        @JsonProperty("type") var type: String,
        @JsonProperty("content") var content: String,
        @JsonProperty("chat_id") var chatId: Long,
        @JsonProperty("sender_id") var senderId: Long
)
