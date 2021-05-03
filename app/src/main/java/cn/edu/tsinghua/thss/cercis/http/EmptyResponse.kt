package cn.edu.tsinghua.thss.cercis.http

import cn.edu.tsinghua.thss.cercis.util.NetworkResponse
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class EmptyPayload

typealias EmptyNetworkResponse = NetworkResponse<EmptyPayload>
