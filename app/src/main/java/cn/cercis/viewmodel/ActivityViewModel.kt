package cn.cercis.viewmodel

import android.util.Log
import androidx.lifecycle.*
import cn.cercis.Constants.STATIC_BASE
import cn.cercis.common.ActivityId
import cn.cercis.common.LOG_TAG
import cn.cercis.common.UserId
import cn.cercis.common.mapRun
import cn.cercis.dao.EntireActivity
import cn.cercis.entity.Comment
import cn.cercis.entity.User
import cn.cercis.http.EmptyNetworkResponse
import cn.cercis.http.EmptyPayload
import cn.cercis.repository.ActivityRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.helper.FileUploadUtils
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.MappingLiveData
import cn.cercis.util.livedata.unwrapResource
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val userRepository: UserRepository,
    private val fileUploadUtils: FileUploadUtils,
) : ViewModel() {
    // this works as a flag indicating users need to be re-fetched
    private val atomicInteger = AtomicInteger(0)

    private val users = HashMap<UserId, LiveData<User>>()
    private val usersDisplay = HashMap<UserId, LiveData<CommonListItemData>>()
    private val comments = HashMap<ActivityId, LiveData<List<Comment>>>()

    private val activitySource by lazy {
        object : MappingLiveData<Resource<List<EntireActivity>>>() {
            init {
                refresh()
            }

            fun refresh() {
                setSource(activityRepository.getActivityList().asLiveData(coroutineContext))
            }
        }
    }

    val isLoading by lazy { Transformations.map(activitySource) { it is Resource.Loading } }

    val activities: LiveData<List<ActivityListItem>> by lazy {
        activitySource.map { resource ->
            val updateMark = atomicInteger.get()
            Log.d(LOG_TAG, "updated with mark $updateMark")
            (resource?.data ?: listOf()).map {
                it.activity.run {
                    ActivityListItem(
                        activityId = id,
                        user = getUserLiveData(userId),
                        mediaType = mediaType,
                        text = text,
                        mediaUrlList = it.media.mapRun { url },
                        commentList = it.comments,
                        thumbUpList = it.thumbUps.mapRun { userId },
                        publishedAt = publishedAt,
                        isLoading = isLoading,
                    )
                }
            }.sortedByDescending {
                it.publishedAt
            }
        }
    }

    fun getUserLiveData(userId: UserId): LiveData<User> {
        return users.computeIfAbsent(userId) {
            userRepository.getUser(it).asLiveData(coroutineContext).unwrapResource()
        }
    }

    suspend fun sendComment(activityId: ActivityId, content: String): EmptyNetworkResponse {
        return activityRepository.sendComment(activityId, content).thenUse {
            activityRepository.getActivityList().fetchAndSave()
        }.use { EmptyPayload() }
    }

    fun loadUser(userId: UserId): LiveData<CommonListItemData> {
        return usersDisplay.computeIfAbsent(userId) { uid ->
            userRepository.getUserWithFriendDisplay(uid, true)
                .mapLatest { it }
                .asLiveData(coroutineContext)
        }
    }

    fun getCommentLiveData(activityId: ActivityId): LiveData<List<Comment>> {
        return comments.computeIfAbsent(activityId) {
            activityRepository.getCommentList(activityId).asLiveData(coroutineContext)
        }
    }

    /**
     * Refreshes users in the user list.
     */
    fun refresh() {
        // the following steps should not be reordered
        // add updateMark by 1 to fail all caches
        atomicInteger.incrementAndGet()
        // clear users to enforce re-fetch
        users.clear()
        // trigger activity list reload
        activitySource.refresh()
    }

    fun publishVideoActivity(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = fileUploadUtils.uploadFile(file)
            if (response is NetworkResponse.Success) {
                val url = STATIC_BASE + response.data
                activityRepository.publishVideoActivity(url)
                launch(Dispatchers.Main) {
                    refresh()
                }
            }
        }
    }
}
