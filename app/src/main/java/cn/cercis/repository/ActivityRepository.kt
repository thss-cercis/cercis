package cn.cercis.repository

import cn.cercis.entity.Activity
import cn.cercis.http.CercisHttpService
import cn.cercis.util.FakeData
import cn.cercis.util.resource.DataSource
import cn.cercis.util.resource.NetworkResponse
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@ActivityRetainedScoped
@FlowPreview
@ExperimentalCoroutinesApi
class ActivityRepository @Inject constructor(
    private val fakeData: FakeData,
    private val httpService: CercisHttpService,
) {
    fun getActivityList(range: LongRange) = object : DataSource<List<Activity>>() {
        override suspend fun fetch(): NetworkResponse<List<Activity>> {
//            return httpService.getActivityList().use { activities }
            return NetworkResponse.Success(fakeData.getFakeActivityList(range))
        }

        override suspend fun saveToDb(data: List<Activity>) {
        }

        override fun loadFromDb(): Flow<List<Activity>?> {
            return flowOf(fakeData.getFakeActivityList(range))
        }
    }
}
