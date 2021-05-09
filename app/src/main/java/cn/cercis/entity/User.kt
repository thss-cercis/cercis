package cn.cercis.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.cercis.common.ChatId
import cn.cercis.common.UserId
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class User(
    @PrimaryKey val id: UserId,
    val nickname: String,
    val mobile: String,
    val avatar: String,
    val bio: String,
    val updated: Long,
) : Parcelable

@Entity
@Parcelize
@JsonClass(generateAdapter = true)
data class UserDetail(
    @PrimaryKey val id: UserId,
    val nickname: String,
    val email: String,
    val mobile: String,
    val avatar: String,
    val bio: String,
) : Parcelable
