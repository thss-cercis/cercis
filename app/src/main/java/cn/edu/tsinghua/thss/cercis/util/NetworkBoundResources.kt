/*
 * https://stackoverflow.com/a/58845665
 * CC BY-SA 4.0
 */
package cn.edu.tsinghua.thss.cercis.util


import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import cn.edu.tsinghua.thss.cercis.api.PayloadResponseBody
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

/**
 * A generic class that can provide a Response backed by both the sqlite database and the network.
 *
 * You can read more about it in the [Architecture
 * Guide](https://developer.android.com/arch).
 * @param <ResultType>
 * @param <RequestType>
</RequestType></ResultType> */
@FlowPreview
@ExperimentalCoroutinesApi
abstract class NetworkBoundResource<ResultType, RequestType> {

    fun asFlow() = flow<Resource<ResultType>> {
        emit(Resource.Loading(null))

        val dbValue = loadFromDb().first()
        if (shouldFetch(dbValue)) {
            emit(Resource.Loading(dbValue))
            when (val apiResponse = fetchFromNetwork()) {
                is NetworkResponse.Success -> {
                    saveNetworkResult(processResponse(apiResponse))
                    emitAll(loadFromDb().filterNotNull().map { Resource.Success(it) })
                }
                is NetworkResponse.Reject -> {
                    onFetchFailed()
                    emitAll(loadFromDb().map { Resource.Error(apiResponse.code, apiResponse.message, it) })
                }
                is NetworkResponse.NetworkError -> {
                    onFetchFailed()
                    emitAll(loadFromDb().map { Resource.Error(-1, apiResponse.message, it) })
                }
            }
        } else {
            emitAll(loadFromDb().filterNotNull().map { Resource.Success(it) })
        }
    }

    protected open fun onFetchFailed() {
        // Implement in sub-classes to handle errors
    }

    @WorkerThread
    protected open fun processResponse(networkResponse: NetworkResponse<RequestType>) = (networkResponse as NetworkResponse.Success).data

    @WorkerThread
    protected abstract suspend fun saveNetworkResult(item: RequestType)

    @MainThread
    protected abstract fun shouldFetch(data: ResultType?): Boolean

    @MainThread
    protected abstract fun loadFromDb(): Flow<ResultType?>

    @MainThread
    protected abstract suspend fun fetchFromNetwork(): NetworkResponse<RequestType>
}
