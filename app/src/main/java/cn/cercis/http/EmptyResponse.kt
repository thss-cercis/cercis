package cn.cercis.http

import cn.cercis.util.NetworkResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class EmptyPayload

typealias EmptyNetworkResponse = NetworkResponse<EmptyPayload>
