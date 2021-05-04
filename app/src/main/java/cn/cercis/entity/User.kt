package cn.cercis.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.cercis.util.ChatId
import cn.cercis.util.UserId
import com.squareup.moshi.JsonClass

@Entity
@JsonClass(generateAdapter = true)
data class User(
        @PrimaryKey val id: UserId,
        val nickname: String,
        val mobile: String,
        val chatId: ChatId,
        val avatar: String,
        val bio: String,
        val updated: Long,
)

@Entity
@JsonClass(generateAdapter = true)
data class UserDetail(
        @PrimaryKey val id: UserId,
        val nickname: String,
        val mobile: String,
        val avatar: String,
        val bio: String,
)