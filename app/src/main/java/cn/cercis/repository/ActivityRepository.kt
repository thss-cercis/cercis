package cn.cercis.repository

import cn.cercis.common.ActivityId
import cn.cercis.common.MediaType
import cn.cercis.common.mapRun
import cn.cercis.dao.ActivityDao
import cn.cercis.dao.EntireActivity
import cn.cercis.entity.Activity
import cn.cercis.entity.Comment
import cn.cercis.entity.Medium
import cn.cercis.http.*
import cn.cercis.util.resource.DataSourceBase
import cn.cercis.util.resource.NetworkResponse
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import org.joda.time.DateTime
import javax.inject.Inject

@ActivityRetainedScoped
@FlowPreview
@ExperimentalCoroutinesApi
class ActivityRepository @Inject constructor(
    private val activityDao: ActivityDao,
    private val httpService: CercisHttpService,
) {
    fun getActivityList(range: LongRange? = null) =
        object : DataSourceBase<List<EntireActivity>, List<ActivityPayload>>() {
            override suspend fun fetch(): NetworkResponse<List<ActivityPayload>> {
                return httpService.getActivityList()
            }

            override suspend fun saveToDb(data: List<ActivityPayload>) {
                activityDao.deleteAllActivities()
                activityDao.saveEntireActivityList(data.mapRun {
                    Activity(
                        id = id,
                        userId = userId,
                        text = text,
                        mediaTypeCode = when {
                            media.isEmpty() -> 0
                            else -> media.first().type
                        },
                        publishedAt = DateTime.parse(createdAt).millis,
                    )
                }, data.flatMap {
                    it.media.mapRun {
                        Medium(
                            id = id,
                            activityId = activityId,
                            url = content
                        )
                    }
                }, data.flatMap {
                    it.comments
                }, data.flatMap {
                    it.thumbUps
                })
            }

            override fun loadFromDb(): Flow<List<EntireActivity>?> {
                return activityDao.loadEntireActivityList()
            }
        }

    fun getCommentList(activityId: ActivityId): Flow<List<Comment>> {
        return activityDao.loadCommentList(activityId)
    }

    suspend fun publishNormalActivity(text: String, imageUrls: List<String>) =
        httpService.publishActivity(
            PublishActivityRequest(
                text = text,
                type = MediaType.IMAGE.code,
                contents = imageUrls,
            )
        )

    suspend fun publishVideoActivity(videoUrl: String) =
        httpService.publishActivity(
            PublishActivityRequest(
                text = "",
                type = MediaType.VIDEO.code,
                contents = listOf(videoUrl),
            )
        )

    suspend fun thumbUp(activityId: ActivityId, value: Boolean) =
        if (value) {
            httpService.thumbUpActivity(ActivityRequest(activityId))
        } else {
            httpService.undoThumbUpActivity(ActivityRequest(activityId))
        }

    suspend fun sendComment(activityId: ActivityId, content: String) =
        httpService.addActivityComment(ActivityCommentRequest(activityId, content))

    suspend fun deleteActivity(activityId: ActivityId) =
        httpService.deleteActivity(ActivityRequest(activityId))
}
