package cn.edu.tsinghua.thss.cercis.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PayloadResponse<P>(
        var code: Int,
        var msg: String,
        var payload: P?
)
