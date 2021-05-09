package cn.cercis.http

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PayloadResponseBody<P>(
    val code: Int,
    val msg: String,
    val payload: P?,
) {
    val successful: Boolean
        get() = code == 0

    val authorized: Boolean
        get() = code != HttpStatusCode.StatusUnauthorized
}
