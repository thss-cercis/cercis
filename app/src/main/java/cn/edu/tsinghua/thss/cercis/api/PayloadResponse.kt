package cn.edu.tsinghua.thss.cercis.api

import cn.edu.tsinghua.thss.cercis.util.HttpStatusCode
import com.squareup.moshi.JsonClass
import retrofit2.Response

@JsonClass(generateAdapter = true)
data class PayloadResponse<P>(
        var code: Int,
        var msg: String,
        var payload: P?
) {
    fun isSuccessful(): Boolean {
        return code == 0
    }
}

val <P> Response<PayloadResponse<P>>.payload: P?
    get() {
        val body = this.body()
        return body?.payload
    }

/**
 * Returns if a cercis request succeeded
 */
val <P> Response<PayloadResponse<P>>.ok: Boolean
    get() {
        val body = this.body()
        return this.isSuccessful && body != null && body.code == 0
    }

val <P> Response<PayloadResponse<P>>.authorized: Boolean
    get() {
        return this.code() != HttpStatusCode.StatusUnauthorized
    }
