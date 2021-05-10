package cn.cercis.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.cercis.common.CommonId
import cn.cercis.common.Timestamp
import cn.cercis.common.UserId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity
@JsonClass(generateAdapter = true)
data class Activity(
    @PrimaryKey val id: CommonId,
    @Json(name = "user_id") val userId: UserId,
    @Json(name = "published_at") val publishedAt: Timestamp,
    val type: Int,
    val text: String?,
    @Json(name = "image_urls") val imageUrls: List<String>,
    @Json(name = "video_url") val videoUrl: String?,
    @Json(name = "liked_user_names") val likedUserNames: List<String>,
)