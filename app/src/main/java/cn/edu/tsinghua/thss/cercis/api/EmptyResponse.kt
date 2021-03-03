package cn.edu.tsinghua.thss.cercis.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class EmptyPayload

typealias EmptyResponse = PayloadResponse<EmptyPayload>
