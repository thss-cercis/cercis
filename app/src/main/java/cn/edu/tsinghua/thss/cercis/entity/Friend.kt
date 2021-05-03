package cn.edu.tsinghua.thss.cercis.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import cn.edu.tsinghua.thss.cercis.util.CommonId
import cn.edu.tsinghua.thss.cercis.util.UserId

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
