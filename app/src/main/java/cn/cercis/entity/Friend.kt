package cn.cercis.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import cn.cercis.util.ApplyId
import cn.cercis.util.CommonId
import cn.cercis.util.UserId
import com.facebook.stetho.json.annotation.JsonValue
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonQualifier

@Entity
data class FriendEntry(
    @PrimaryKey val id: CommonId,
    @ColumnInfo(index = true) val friendUserId: UserId,
    val remarks: String,
    val alias: String,
)

@Entity
@JsonClass(generateAdapter = true)
data class FriendRequest(
    @PrimaryKey @Json(name = "apply_id") val applyId: ApplyId = 0L,
    val fromId: UserId,
    val toId: UserId,
    val state: Int,
) {
    companion object {
        const val STATE_PENDING = 0
        const val STATE_ACCEPTED = 1
        const val STATE_REJECTED = -1
    }
}
