package cn.cercis.viewmodel

import android.app.Application
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import cn.cercis.R
import cn.cercis.common.CommonId
import cn.cercis.common.LOG_TAG
import cn.cercis.common.Timestamp
import cn.cercis.common.UserId
import cn.cercis.entity.Activity
import cn.cercis.entity.User
import cn.cercis.repository.ActivityRepository
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.helper.getString
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
    application: Application,
    private val authRepository: AuthRepository,
    private val activityRepository: ActivityRepository,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {
    class ActivityListItem(
        val activityId: CommonId,
        val user: LiveData<User>,
        val text: String,
        private val imageList: List<String>,
        val publishedAt: Timestamp,
        val isLoading: LiveData<Boolean>,
    ) {
        val columnCount: Int
            get() {
                Log.d(LOG_TAG, imageList.size.toString())
                return when (imageList.size) {
                    4 -> 2
                    else -> 3
                }
            }

        fun imageVisible(pos: Int): Int {
            return when {
                pos < imageList.size -> View.VISIBLE
                else -> View.GONE
            }
        }

        fun getImageUrl(pos: Int): String {
            return when {
                pos < imageList.size -> imageList[pos]
                else -> ""
            }
        }
    }

    // this works as a flag indicating users need to be re-fetched
    private val atomicInteger = AtomicInteger(0)

    private val users = HashMap<UserId, LiveData<User>>()

    private val activitySource by lazy {
        object : MappingLiveData<Resource<List<Activity>>>() {
            fun refresh() {
//                val fakeData: Resource<List<Activity>> = Resource.Success((1L..10L).map {
//                    Activity(id = it)
//                })
//                setSource(MutableLiveData(fakeData))
                setSource(activityRepository.getActivityList(1L..10L).asLiveData(coroutineContext))
            }
        }.apply {
            refresh()
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
                    user = getUserLiveData(authRepository.currentUserId),
                    text = getString(R.string.lorem_ipsum),
                    imageList = List(Random.nextInt(1, 9)) { "" },
                    publishedAt = 0,
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
