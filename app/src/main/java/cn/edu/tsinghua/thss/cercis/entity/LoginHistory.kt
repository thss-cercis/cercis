package cn.edu.tsinghua.thss.cercis.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import cn.edu.tsinghua.thss.cercis.util.CommonId
import cn.edu.tsinghua.thss.cercis.util.UserId
import com.squareup.moshi.JsonClass

@Entity
data class LoginHistory(
    @PrimaryKey val userId: UserId,
    val mobile: String,
)
