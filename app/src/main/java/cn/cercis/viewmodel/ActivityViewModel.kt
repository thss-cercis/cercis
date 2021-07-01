package cn.cercis.viewmodel

import androidx.lifecycle.*
import cn.cercis.Constants.STATIC_BASE
import cn.cercis.common.ActivityId
import cn.cercis.common.UserId
import cn.cercis.common.mapRun
import cn.cercis.entity.Comment
import cn.cercis.entity.User
import cn.cercis.http.EmptyNetworkResponse
import cn.cercis.http.EmptyPayload
import cn.cercis.repository.ActivityRepository
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.helper.FileUploadUtils
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ActivityViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val activityRepository: ActivityRepository,
    private val userRepository: UserRepository,
    private val fileUploadUtils: FileUploadUtils,
) : ViewModel() {
    val currentUserId = authRepository.currentUserId

    private val users = HashMap<UserId, LiveData<User>>()
    private val usersDisplay = HashMap<UserId, LiveData<CommonListItemData>>()
    private val comments = HashMap<ActivityId, LiveData<List<Comment>>>()

//    private val activitySource by lazy {
//        object : MappingLiveData<Resource<List<EntireActivity>>>() {
//            init {
//                refresh()
//            }
//
//            fun refresh() {
//                setSource(activityRepository.getActivityList().asLiveData(coroutineContext))
//            }
//        }
//    }

    private val refreshTag = MutableStateFlow(0)
    private val activitySourceResFlow = refreshTag
        .flatMapLatest { activityRepository.getActivityList().flow() }
        .flowOn(Dispatchers.IO)
        .shareIn(viewModelScope, started = SharingStarted.Eagerly, replay = 1)
    private val activitySource = activitySourceResFlow.map { it.data }
        .filterNotNull().asLiveData(coroutineContext)

//    init {
//        activitySource.observeForever {
//            Log.d(LOG_TAG, "null: ${it == null} length: ${it?.size}")
//        }
//    }

    val isLoading by lazy {
        activitySourceResFlow.map { it is Resource.Loading }.asLiveData(coroutineContext)
    }

    val activities: LiveData<List<ActivityListItem>> by lazy {
        activitySource.map { resource ->
            resource.map {
                it.activity.run {
                    ActivityListItem(
                        activityId = id,
                        userId = userId,
                        text = text,
                        mediaType = mediaType,
                        mediaUrlList = it.media.mapRun { url },
                        publishedAt = publishedAt,
                        commentList = it.comments,
                        thumbUpUserIdList = it.thumbUps.mapRun { userId },
                    )
                }
            }.sortedByDescending {
                it.publishedAt
            }
        }
    }

//    fun getUserLiveData(userId: UserId): LiveData<User> {
//        return users.computeIfAbsent(userId) {
//            userRepository.getUser(it).asLiveData(coroutineContext).unwrapResource()
//        }
//    }

    suspend fun sendComment(activityId: ActivityId, content: String): EmptyNetworkResponse {
        return activityRepository.sendComment(activityId, content).thenUse {
            activityRepository.getActivityList().fetchAndSave()
        }.use { EmptyPayload() }
    }

    fun loadUser(userId: UserId): LiveData<CommonListItemData> {
        return usersDisplay.computeIfAbsent(userId) { _ ->
            userRepository.getUserWithFriendDisplay(userId, true)
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
        // clear users to enforce re-fetch
        users.clear()
        // trigger activity list reload
        refreshTag.value += 1
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

    fun thumbUp(activityId: ActivityId, value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = activityRepository.thumbUp(activityId, value)
            if (response is NetworkResponse.Success) {
                launch(Dispatchers.Main) {
                    refresh()
                }
            }
        }
    }

    fun deleteActivity(activityId: ActivityId, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = activityRepository.deleteActivity(activityId)
            if (response is NetworkResponse.Success) {
                launch(Dispatchers.Main) {
                    onSuccess()
                    refresh()
                }
            }
        }
    }
}
