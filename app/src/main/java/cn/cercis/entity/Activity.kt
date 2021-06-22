package cn.cercis.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cn.cercis.common.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity
data class Activity(
    @PrimaryKey val id: CommonId,
    val userId: UserId,
    val text: String,
    val mediaTypeCode: Int,
    val publishedAt: Timestamp,
) {
    val mediaType: MediaType
        get() = when (mediaTypeCode) {
            0 -> MediaType.IMAGE
            1 -> MediaType.VIDEO
            else -> MediaType.NONE
        }
}

@Entity(
    foreignKeys = [ForeignKey(
        entity = Activity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("activityId")]
)
data class Medium(
    @PrimaryKey val id: MediumId,
    val activityId: ActivityId,
    val url: String,
)

@Entity(
    foreignKeys = [ForeignKey(
        entity = Activity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("activityId")]
)
@JsonClass(generateAdapter = true)
data class Comment(
    @PrimaryKey val id: CommentId,
    @Json(name = "activity_id") val activityId: ActivityId,
    @Json(name = "commenter_id") val commenterId: UserId,
    val content: String,
    @Json(name = "created_at") val createdAt: String,
)

@Entity(
    foreignKeys = [ForeignKey(
        entity = Activity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("activityId")]
)
@JsonClass(generateAdapter = true)
data class ThumbUp(
    @PrimaryKey @Json(name = "activity_id") val activityId: ActivityId,
    @PrimaryKey @Json(name = "user_id") val userId: UserId,
)
