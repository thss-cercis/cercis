package cn.edu.tsinghua.thss.cercis.entity

import com.fasterxml.jackson.annotation.JsonProperty

data class Chat(
        @JsonProperty("id") var id: Long,
        @JsonProperty("type") var type: Int,
        @JsonProperty("name") var name: String
)
