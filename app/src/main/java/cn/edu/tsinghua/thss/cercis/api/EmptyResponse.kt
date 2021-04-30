package cn.edu.tsinghua.thss.cercis.api

import cn.edu.tsinghua.thss.cercis.util.NetworkResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class EmptyPayload

typealias EmptyResponse = PayloadResponseBody<EmptyPayload>
typealias EmptyNetworkResponse = NetworkResponse<EmptyPayload>
