package cn.cercis.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.cercis.common.ChatId
import cn.cercis.common.MessageId
import cn.cercis.common.UserId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
@JsonClass(generateAdapter = true)
data class Chat(
    @PrimaryKey @Json(name = "id") val id: Long,
    @Json(name = "type") val type: Int,
    @Json(name = "name") val name: String,
    @Json(name = "avatar") val avatar: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String,
) : Parcelable

@Entity(primaryKeys = ["userId", "chatId"])
@JsonClass(generateAdapter = true)
data class ChatMember(
    @Json(name = "chat_id") val chatId: ChatId,
    @Json(name = "user_id") val userId: UserId,
    @Json(name = "alias") val displayName: String?,
    val permission: Int,
)

@Entity
data class ChatLastRead(
    @PrimaryKey val chatId: ChatId,
    val lastReadMessageId: MessageId,
)

object ChatType {
    const val CHAT_PRIVATE: Int = 0
    const val CHAT_GROUP: Int = 1
}

object GroupChatPermission {
    const val GROUP_MEMBER: Int = 0
    const val GROUP_MANAGER: Int = 1
    const val GROUP_OWNER: Int = 2
}
