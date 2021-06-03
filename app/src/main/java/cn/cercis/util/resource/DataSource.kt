package cn.cercis.util.resource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import cn.cercis.util.helper.coroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

/**
 * A generic class that can provide a Response backed by both the sqlite database and the network.
 *
 * You can read more about it in the [Architecture Guide](https://developer.android.com/arch).
 * @param <DataType>
 */
@FlowPreview
@ExperimentalCoroutinesApi
abstract class DataSourceBase<DataType, ResponseType> {
    fun asLiveData(context: CoroutineContext) = flow().asLiveData(context)

    fun asLiveData(viewModel: ViewModel) = asLiveData(viewModel.coroutineContext)

    fun flow(): Flow<Resource<DataType>> = flow {
        emit(Resource.Loading(null))
        val dbValue = loadFromDb().first()
        if (shouldFetch(dbValue)) {
            emit(Resource.Loading(dbValue))
            emitNetworkResources()
        } else {
            emitAll(dbResourceFlow())
        }
    }

    fun fallbackFlow(): Flow<Resource<DataType>> = flow().filter { it !is Resource.Loading }

    fun dbFlow(): Flow<DataType> = loadFromDb().filterNotNull()

//    fun networkFlow(): Flow<Resource<DataType>> = flow(emitNetworkResources)

    suspend fun fetchAndSave(): NetworkResponse<ResponseType> {
        val response = fetch()
        when (response) {
            is NetworkResponse.Success -> {
                saveToDb(response.data)
            }
            is NetworkResponse.Reject, is NetworkResponse.NetworkError -> {
                onFetchFailed()
            }
        }
        return response
    }

    private fun dbResourceFlow(): Flow<Resource<DataType>> {
        return dbFlow().map { Resource.Success(it) }
    }

    private val emitNetworkResources: suspend FlowCollector<Resource<DataType>>.() -> Unit = {
        emitAll(fetchAndSave().asResourceFlow())
    }

    private val asResourceFlow: NetworkResponse<ResponseType>.() -> Flow<Resource<DataType>> = {
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

    protected open fun shouldFetch(data: DataType?): Boolean = true

    protected abstract suspend fun fetch(): NetworkResponse<ResponseType>

    protected abstract suspend fun saveToDb(data: ResponseType)

    protected abstract fun loadFromDb(): Flow<DataType?>
}

@FlowPreview
@ExperimentalCoroutinesApi
abstract class DataSource<DataType> : DataSourceBase<DataType, DataType>()
