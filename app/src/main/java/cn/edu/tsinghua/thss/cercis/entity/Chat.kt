package cn.edu.tsinghua.thss.cercis.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity
@JsonClass(generateAdapter = true)
data class Chat(
        @PrimaryKey @Json(name = "id") var id: Long,
        @Json(name = "type") var type: Int,
        @Json(name = "name") var name: String,
)

object ChatType {
    const val CHAT_SINGLE: Int = 1
    const val CHAT_MULTIPLE: Int = 2
}
