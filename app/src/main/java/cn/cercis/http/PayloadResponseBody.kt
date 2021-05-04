package cn.cercis.http

import cn.cercis.util.HttpStatusCode
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PayloadResponseBody<P>(
        val code: Int,
        val msg: String,
        val payload: P?,
) {
    val successful: Boolean
        get() {
            return code == 0
        }

    val authorized: Boolean
        get() {
            return code != HttpStatusCode.StatusUnauthorized
        }
}