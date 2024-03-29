package cn.cercis.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.cercis.common.ApplyId
import cn.cercis.common.Timestamp
import cn.cercis.common.UserId
import cn.cercis.viewmodel.CommonListItemData
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity
data class FriendEntry(
    @PrimaryKey val friendUserId: UserId,
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

data class FriendUser(
    // friend
    val friendUserId: UserId,
    val displayName: String,
    // user
    val nickname: String?,
    val avatar: String?,
    val bio: String?,
) {
    fun toCommonListItemData(): CommonListItemData {
        return CommonListItemData(
            displayName = displayName.takeIf { it.isNotEmpty() } ?: nickname ?: "",
            description = bio ?: "",
            avatar = avatar ?: ""
        )
    }
}
