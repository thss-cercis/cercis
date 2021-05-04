package cn.cercis.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.cercis.common.UserId

@Entity
data class LoginHistory(
    @PrimaryKey val userId: UserId,
    val mobile: String,
)
