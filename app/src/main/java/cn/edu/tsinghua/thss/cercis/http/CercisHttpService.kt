package cn.edu.tsinghua.thss.cercis.http

import cn.edu.tsinghua.thss.cercis.entity.UserDetail
import cn.edu.tsinghua.thss.cercis.util.NetworkResponse
import cn.edu.tsinghua.thss.cercis.util.UserId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CercisHttpService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): SignUpResponse

    @POST("mobile/signup")
    suspend fun mobileSignUp(@Body request: MobileSignUpRequest): EmptyNetworkResponse

    @POST("auth/logout")
    suspend fun logout(): EmptyNetworkResponse

    @GET("user/current")
    suspend fun userDetail(): UserDetailResponse
}

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val id: UserId?,
    var mobile: String?,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponsePayload(
    @Json(name = "id") val userId: UserId
)

typealias LoginResponse = NetworkResponse<LoginResponsePayload>

@JsonClass(generateAdapter = true)
data class SignUpRequest(
        val nickname: String,
        val mobile: String,
        @Json(name = "code") val verificationCode: String,
        val password: String
)

@JsonClass(generateAdapter = true)
data class SignUpResponsePayload(
        @Json(name = "user_id") val userId: UserId
)

typealias SignUpResponse = NetworkResponse<SignUpResponsePayload>

@JsonClass(generateAdapter = true)
data class MobileSignUpRequest(
        val mobile: String
)

@JsonClass(generateAdapter = true)
data class EmailSignUpRequest(
        val email: String
)

@JsonClass(generateAdapter = true)
data class MobileSignUpCheckRequest(
        val code: String
)

@JsonClass(generateAdapter = true)
data class EmailSignUpCheckResponsePayload(
        val ok: Boolean
)

typealias UserDetailResponse = NetworkResponse<UserDetail>
