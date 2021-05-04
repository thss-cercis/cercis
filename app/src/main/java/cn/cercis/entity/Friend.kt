package cn.cercis.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import cn.cercis.util.ApplyId
import cn.cercis.util.CommonId
import cn.cercis.util.Timestamp
import cn.cercis.util.UserId
import com.facebook.stetho.json.annotation.JsonValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonQualifier

@Entity
data class FriendEntry(
    @PrimaryKey val id: CommonId,
    @ColumnInfo(index = true) val friendUserId: UserId,
    val remark: String,
    @Json(name = "alias") val displayName: String,
)

@Entity
@JsonClass(generateAdapter = true)
data class FriendRequest(
    @PrimaryKey @Json(name = "apply_id") val applyId: ApplyId,
    val fromId: UserId,
    val toId: UserId,
    val state: Int,
    @Json(name = "alias") val requestedDisplayName: String,
    val remark: String,
    @Json(name = "created_at") val createdAt: Timestamp,
) {
    companion object {
        const val STATE_PENDING = 0
        const val STATE_ACCEPTED = 1
        const val STATE_REJECTED = -1
    }
}
