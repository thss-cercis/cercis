package cn.edu.tsinghua.thss.cercis.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import cn.edu.tsinghua.thss.cercis.util.CommonId
import cn.edu.tsinghua.thss.cercis.util.UserId
import com.squareup.moshi.JsonClass

@Entity(
//    foreignKeys = [ForeignKey(
//        entity = User::class,
//        parentColumns = ["id"],
//        childColumns = ["userId"],
//        onDelete = ForeignKey.CASCADE,
//    )]
)
data class LoginHistory(
    @PrimaryKey(autoGenerate = true) val id: CommonId = 0,
    val userId: UserId,
    val mobile: String,
)
