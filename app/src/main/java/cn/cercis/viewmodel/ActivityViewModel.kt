package cn.cercis.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.common.MediaType
import cn.cercis.common.UserId
import cn.cercis.entity.Activity
import cn.cercis.entity.User
import cn.cercis.repository.ActivityRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.getFakeMediaUrlList
import cn.cercis.util.getString
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.livedata.MappingLiveData
import cn.cercis.util.livedata.unwrapResource
import cn.cercis.util.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.random.Random

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val activityRepository: ActivityRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    // this works as a flag indicating users need to be re-fetched
    private val atomicInteger = AtomicInteger(0)

    private val users = HashMap<UserId, LiveData<User>>()

    private val activitySource by lazy {
        object : MappingLiveData<Resource<List<Activity>>>() {
            init { refresh() }
            fun refresh() {
                setSource(activityRepository.getActivityList(1L..10L).asLiveData(coroutineContext))
            }
        }
    }

    val isLoading by lazy { Transformations.map(activitySource) { it is Resource.Loading } }

    val activities: LiveData<List<ActivityListItem>> by lazy {
        Transformations.map(activitySource) { resource ->
            val updateMark = atomicInteger.get()
            Log.d(LOG_TAG, "updated with mark $updateMark")
            (resource?.data ?: listOf()).map {
                ActivityListItem(
                    activityId = it.id,
                    user = getUserLiveData(it.userId),
                    mediaType = it.mediaType,
                    text = it.text,
                    mediaUrlList = getFakeMediaUrlList(it.mediaType),
                    publishedAt = it.publishedAt,
                    isLoading = isLoading,
                )
            }.sortedByDescending {
                it.publishedAt
            }
        }
    }

    private fun getUserLiveData(userId: UserId): LiveData<User> {
        return users.computeIfAbsent(userId) {
            userRepository.getUser(it).asLiveData(coroutineContext).unwrapResource()
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
}
