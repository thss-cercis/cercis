package cn.edu.tsinghua.thss.cercis.util

/**
 * A generic class that represents response with status.
 */
sealed class NetworkResponse<out T> {
    /**
     * Server responded with queried resource.
     */
    data class Success<out T>(val data: T) : NetworkResponse<T>()

    /**
     * Server responded with error message.
     */
    data class Reject<out T>(val code: Int, val message: String) : NetworkResponse<T>()

    /**
     * Failed to connect to server due to possible network issues.
     */
    data class NetworkError<out T>(val message: String) : NetworkResponse<T>()
}

/**
 * A generic class that represents response with status.
 */
sealed class Resource<out T>(open val data: T?) {
    data class Success<out T>(override val data: T) : Resource<T>(data)
    data class Error<out T>(val code: Int, val message: String, override val data: T?) : Resource<T>(data)
    data class Loading<out T>(override val data: T?) : Resource<T>(data)
}