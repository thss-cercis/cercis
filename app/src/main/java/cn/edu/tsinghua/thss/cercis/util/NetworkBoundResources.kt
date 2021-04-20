/*
 * https://stackoverflow.com/a/58845665
 * CC BY-SA 4.0
 */
package cn.edu.tsinghua.thss.cercis.util


import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import cn.edu.tsinghua.thss.cercis.api.PayloadResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

/**
 * A generic class that can provide a resource backed by both the sqlite database and the network.
 *
 *
 * You can read more about it in the [Architecture
 * Guide](https://developer.android.com/arch).
 * @param <ResultType>
 * @param <RequestType>
</RequestType></ResultType> */
@FlowPreview
@ExperimentalCoroutinesApi
abstract class NetworkBoundResource<ResultType, RequestType> {

    fun asFlow() = flow {
        emit(Resource.loading(null))

        val dbValue = loadFromDb().first()
        if (shouldFetch(dbValue)) {
            emit(Resource.loading(dbValue))
            val apiResponse = fetchFromNetwork()
            when (apiResponse.successful) {
                true -> {
                    saveNetworkResult(processResponse(apiResponse))
                    emitAll(loadFromDb().map { Resource.success(it) })
                }
                false -> {
                    onFetchFailed()
                    emitAll(loadFromDb().map { Resource.error(apiResponse.msg, it) })
                }
            }
        } else {
            emitAll(loadFromDb().map { Resource.success(it) })
        }
    }

    protected open fun onFetchFailed() {
        // Implement in sub-classes to handle errors
    }

    @WorkerThread
    protected open fun processResponse(response: PayloadResponse<RequestType>) = response.payload!!

    @WorkerThread
    protected abstract suspend fun saveNetworkResult(item: RequestType)

    @MainThread
    protected abstract fun shouldFetch(data: ResultType?): Boolean

    @MainThread
    protected abstract fun loadFromDb(): Flow<ResultType>

    @MainThread
    protected abstract suspend fun fetchFromNetwork(): PayloadResponse<RequestType>
}
