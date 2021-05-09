package cn.cercis.repository

import cn.cercis.entity.Activity
import cn.cercis.util.resource.DataSource
import cn.cercis.util.resource.NetworkResponse
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@ActivityRetainedScoped
@FlowPreview
@ExperimentalCoroutinesApi
class ActivityRepository @Inject constructor(
) {
    fun getActivityList(range: LongRange) = object : DataSource<List<Activity>>() {
        override suspend fun fetch(): NetworkResponse<List<Activity>> {
            return NetworkResponse.Success(range.map { Activity(id = it) })
        }

        override suspend fun saveToDb(data: List<Activity>) {
        }

        override fun loadFromDb(): Flow<List<Activity>?> {
            return flow {
                emit(range.map { Activity(id = it) })
            }
        }
    }
}
