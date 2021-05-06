package cn.cercis.util.resource

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

/**
 * A generic class that can provide a Response backed by both the sqlite database and the network.
 *
 * You can read more about it in the [Architecture Guide](https://developer.android.com/arch).
 * @param <T>
 */
@FlowPreview
@ExperimentalCoroutinesApi
abstract class DataSource<T> {

    fun flow(): Flow<Resource<T>> = flow {
        emit(Resource.Loading(null))
        val dbValue = loadFromDb().first()
        if (shouldFetch(dbValue)) {
            emit(Resource.Loading(dbValue))
            emitNetworkResources()
        } else {
            emitAll(dbResourceFlow())
        }
    }

    fun dbFlow(): Flow<T> = loadFromDb().filterNotNull()

    fun networkFlow(): Flow<Resource<T>> = flow(emitNetworkResources)

    private fun dbResourceFlow(): Flow<Resource<T>> {
        return dbFlow().map { Resource.Success(it) }
    }

    private val emitNetworkResources: suspend FlowCollector<Resource<T>>.() -> Unit = {
        val response = fetch()
        when (response) {
            is NetworkResponse.Success -> {
                saveToDb(response.data)
            }
            is NetworkResponse.Reject,
            is NetworkResponse.NetworkError -> {
                onFetchFailed()
            }
        }
        emitAll(response.asResourceFlow())
    }

    private val asResourceFlow: NetworkResponse<T>.() -> Flow<Resource<T>> = {
        when (this) {
            is NetworkResponse.Success -> {
                dbResourceFlow()
            }
            is NetworkResponse.Reject -> {
                loadFromDb().map { Resource.Error(code, message, it) }
            }
            is NetworkResponse.NetworkError -> {
                loadFromDb().map { Resource.Error(-1, message, it) }
            }
        }
    }

    // Implement in sub-classes to handle errors
    protected open fun onFetchFailed() {}

    protected open fun shouldFetch(data: T?): Boolean = true

    protected abstract suspend fun fetch(): NetworkResponse<T>

    protected abstract suspend fun saveToDb(data: T)

    protected abstract fun loadFromDb(): Flow<T?>
}
