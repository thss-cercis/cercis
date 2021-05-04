package cn.cercis.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.cercis.common.ApplyId
import cn.cercis.common.CommonId
import cn.cercis.common.Timestamp
import cn.cercis.common.UserId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity
data class FriendEntry(
    @PrimaryKey(autoGenerate = true) val id: CommonId = 0,
    @ColumnInfo(index = true) val friendUserId: UserId,
    val remark: String,
    val displayName: String,
)

@Entity
@JsonClass(generateAdapter = true)
data class FriendRequest(
    @PrimaryKey @Json(name = "apply_id") val applyId: ApplyId,
    @Json(name = "from_id") val fromId: UserId,
    @Json(name = "to_id") val toId: UserId,
    @Json(name = "alias") val displayName: String?,
    val state: Int,
    val remark: String,
    @Json(name = "created_at") val createdAt: Timestamp,
) {
    companion object {
        const val STATE_PENDING = 0
        const val STATE_ACCEPTED = 1
        const val STATE_REJECTED = -1
    }
}
