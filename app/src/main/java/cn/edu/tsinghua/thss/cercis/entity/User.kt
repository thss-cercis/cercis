package cn.edu.tsinghua.thss.cercis.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import cn.edu.tsinghua.thss.cercis.util.ChatId
import cn.edu.tsinghua.thss.cercis.util.CommonId
import cn.edu.tsinghua.thss.cercis.util.UserId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity
data class User(
        @PrimaryKey val id: UserId,
        val nickname: String,
        val mobile: String,
        val email: String,
        val chatId: ChatId,
        val avatar: String,
        val bio: String,
        val updated: Long,
)

@Entity(
        foreignKeys = [ForeignKey(
                entity = User::class,
                parentColumns = ["id"],
                childColumns = ["friendUserId"],
                onDelete = ForeignKey.CASCADE,
        )]
)
data class FriendEntry(
        @PrimaryKey val id: CommonId,
        @ColumnInfo(index = true) val friendUserId: UserId,
        val remarks: String,
        val alias: String,
)

@Entity
@JsonClass(generateAdapter = true)
data class CurrentUser(
        @PrimaryKey val id: UserId,
        val nickname: String,
        val mobile: String,
        val email: String,
        val avatar: String,
        val bio: String,
)