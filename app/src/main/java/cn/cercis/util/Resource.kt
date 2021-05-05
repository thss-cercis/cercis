package cn.cercis.util

/**
 * A generic class that represents response with status.
 */
sealed class NetworkResponse<out T>(open val message: String?) {
    /**
     * Server responded with queried resource.
     */
    data class Success<out T>(val data: T) : NetworkResponse<T>(null)

    /**
     * Server responded with error message.
     */
    data class Reject<out T>(val code: Int, override val message: String) : NetworkResponse<T>(message)

    /**
     * Failed to connect to server due to possible network issues.
     */
    data class NetworkError<out T>(override val message: String) : NetworkResponse<T>(message)

    fun <ToType> use(block: T.() -> ToType) : NetworkResponse<ToType> {
        return when (this) {
            is Success -> Success(data.block())
            is Reject -> Reject(code, message)
            is NetworkError -> NetworkError(message)
        }
    }

    fun <ToType> convert(transform: (T) -> ToType) = use(transform)
}

/**
 * A generic class that represents response with status.
 */
sealed class Resource<out T>(open val data: T?) {
    data class Success<out T>(override val data: T) : Resource<T>(data)
    data class Error<out T>(val code: Int, val message: String, override val data: T?) : Resource<T>(data)
    data class Loading<out T>(override val data: T?) : Resource<T>(data)
}
