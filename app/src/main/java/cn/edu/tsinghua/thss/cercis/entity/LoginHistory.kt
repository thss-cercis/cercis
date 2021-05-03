package cn.edu.tsinghua.thss.cercis.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.edu.tsinghua.thss.cercis.util.UserId

@Entity
data class LoginHistory(
    @PrimaryKey val userId: UserId,
    val mobile: String,
)
