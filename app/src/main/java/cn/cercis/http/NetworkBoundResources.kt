/*
 * https://stackoverflow.com/a/58845665
 * CC BY-SA 4.0
 */

package cn.cercis.http

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import cn.cercis.common.LOG_TAG
import cn.cercis.util.NetworkResponse
import cn.cercis.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

/**
 * A generic class that can provide a Response backed by both the sqlite database and the network.
 *
 * You can read more about it in the [Architecture Guide](https://developer.android.com/arch).
 * @param <T>
 */
@FlowPreview
@ExperimentalCoroutinesApi
abstract class NetworkBoundResource<T> {

    fun asFlow() = flow {
        emit(Resource.Loading(null))
        val dbValue = loadFromDb().first()
        if (shouldFetch(dbValue)) {
            emit(Resource.Loading(dbValue))
            emitAll(networkFlow())
        } else {
            emitAll(dbFlow())
        }
    }

    suspend fun dbFlow(): Flow<Resource<T>> {
        return loadFromDb().filterNotNull().map { Resource.Success(it) }
    }

    suspend fun networkFlow(): Flow<Resource<T>> {
        return fetchFromNetwork().run {
            Log.d(LOG_TAG, "$this")
            when (this) {
                is NetworkResponse.Success -> {
                    saveNetworkResult(data)
                    dbFlow()
                }
                is NetworkResponse.Reject -> {
                    onFetchFailed()
                    loadFromDb().map { Resource.Error(code, message, it) }
                }
                is NetworkResponse.NetworkError -> {
                    onFetchFailed()
                    loadFromDb().map { Resource.Error(-1, message, it) }
                }
            }
        }
    }

    protected open fun onFetchFailed() {
        // Implement in sub-classes to handle errors
    }

    protected open fun shouldFetch(data: T?): Boolean = true

    protected abstract suspend fun fetchFromNetwork(): NetworkResponse<T>

    protected abstract suspend fun saveNetworkResult(data: T)

    protected abstract suspend fun loadFromDb(): Flow<T?>
}
