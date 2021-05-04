package cn.cercis.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity
@JsonClass(generateAdapter = true)
data class Chat(
        @PrimaryKey @Json(name = "id") val id: Long,
        @Json(name = "type") val type: Int,
        @Json(name = "name") val name: String,
        @Json(name = "lastMessage") val lastMessage: String,
)

object ChatType {
    const val CHAT_SINGLE: Int = 1
    const val CHAT_MULTIPLE: Int = 2
}