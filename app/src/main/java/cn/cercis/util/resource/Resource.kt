package cn.cercis.util.resource

/**
 * A generic class that represents response with status.
 */
sealed class Resource<out T>(open val data: T?) {
    data class Success<out T>(override val data: T) : Resource<T>(data)
    data class Error<out T>(val code: Int, val message: String, override val data: T?) : Resource<T>(data)
    data class Loading<out T>(override val data: T?) : Resource<T>(data)
}
