/*
 * https://stackoverflow.com/a/58845665
 * CC BY-SA 4.0
 */

package cn.cercis.http

import android.util.Log
import cn.cercis.common.LOG_TAG
import cn.cercis.util.NetworkResponse
import cn.cercis.util.Resource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

/**
 * A generic class that can provide a Response backed by both the sqlite database and the network.
 *
 * You can read more about it in the [Architecture Guide](https://developer.android.com/arch).
 * @param <T>
 * @param <RequestType>
 */
@FlowPreview
@ExperimentalCoroutinesApi
abstract class NetworkBoundResource<T> {

    fun asFlow() = flow<Resource<T>> {
        emit(Resource.Loading(null))
        val dbValue = loadFromDb().first()
        if (shouldFetch(dbValue)) {
            emit(Resource.Loading(dbValue))
            fetchFromNetwork().run {
                Log.d(LOG_TAG, "$this")
                when (this) {
                    is NetworkResponse.Success -> {
                        saveNetworkResult(data)
                        emitAll(loadFromDb().filterNotNull().map { Resource.Success(it) })
                    }
                    is NetworkResponse.Reject -> {
                        onFetchFailed()
                        emitAll(loadFromDb().map { Resource.Error(code, message, it) })
                    }
                    is NetworkResponse.NetworkError -> {
                        onFetchFailed()
                        emitAll(loadFromDb().map { Resource.Error(-1, message, it) })
                    }
                }
            }
        } else {
            emitAll(loadFromDb().filterNotNull().map { Resource.Success(it) })
        }
    }

    protected open fun onFetchFailed() {
        // Implement in sub-classes to handle errors
    }

    protected open fun shouldFetch(data: T?): Boolean = true

    protected abstract suspend fun fetchFromNetwork(): NetworkResponse<T>

    protected abstract suspend fun saveNetworkResult(data: T)

    protected abstract fun loadFromDb(): Flow<T?>
}
