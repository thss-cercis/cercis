package cn.cercis.viewmodel

import android.text.format.DateUtils
import android.view.View
import cn.cercis.common.CommonId
import cn.cercis.common.MediaType
import cn.cercis.common.Timestamp
import cn.cercis.common.UserId
import cn.cercis.entity.Comment

data class ActivityListItem(
    val activityId: CommonId,
    val userId: UserId,
    val text: String,
    val mediaType: MediaType,
    val mediaUrlList: List<String>,
    val publishedAt: Timestamp,
    val commentList: List<Comment>,
    val thumbUpUserIdList: List<UserId>,
) {
    private val imageList = when (mediaType) {
        MediaType.IMAGE -> mediaUrlList
        MediaType.VIDEO -> listOf()
    }

    val videoUrl = when(mediaType) {
        MediaType.VIDEO -> mediaUrlList.firstOrNull()
        MediaType.IMAGE -> null
    }

    init {
//        Log.d(LOG_TAG, "Init with imageCount=$imageCount")
//        Log.d(LOG_TAG, "$imageList")
    }

    val viewType: Int
        get() = when (mediaType) {
            MediaType.IMAGE -> columnCount
            MediaType.VIDEO -> VIEW_TYPE_VIDEO
        }

    private val imageCount get() = imageList.size

    val columnCount: Int
        get() = when (imageCount) {
            0 -> 1
            1 -> 1
            2 -> 2
            3 -> 3
            4 -> 2
            else -> 3
        }

    val rowCount: Int
        get() = when (imageCount) {
            0 -> 1
            else -> (imageCount - 1) / columnCount + 1
        }

    val dimensionRatio
        get() = "${columnCount}:${rowCount}"

    val publishedTimeText = DateUtils.getRelativeTimeSpanString(publishedAt)!!

    fun getImageUrl(pos: Int): String {
        return when {
            pos < imageCount -> imageList[pos]
            else -> {
//                    Log.d(LOG_TAG, "invisible image index ${pos + 1}/$imageCount")
                ""
            }
        }
    }

    fun isImageVisible(pos: Int): Int {
        return when {
            pos < imageCount -> View.VISIBLE
            else -> View.GONE
        }
    }

    fun isViewVisible(id: Int): Int {
        val isVisible = when (id) {
            ID_TEXT -> text.isNotEmpty()
            ID_IMAGE_GRID -> mediaType == MediaType.IMAGE && imageCount != 0
            ID_VIDEO -> mediaType == MediaType.VIDEO
            else -> false
        }
        return when {
            isVisible -> View.VISIBLE
            else -> View.GONE
        }
    }

    companion object {
        const val ID_TEXT = 0
        const val ID_IMAGE_GRID = 1
        const val ID_VIDEO = 2

        const val VIEW_TYPE_VIDEO = 100
    }
}
