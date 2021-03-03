package cn.edu.tsinghua.thss.cercis.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Chat(
        @Json(name = "id") var id: Long,
        @Json(name = "type") var type: Int,
        @Json(name = "name") var name: String
)
