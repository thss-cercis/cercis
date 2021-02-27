package cn.edu.tsinghua.thss.cercis.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface HttpService {

    @POST("/auth/login")
    fun login(@Body request: LoginRequest): Call<EmptyResponse>

    @JsonClass(generateAdapter = true)
    data class LoginRequest(
            var id: String,
            var password: String
    )

    @POST("/auth/signup")
    fun signUp(@Body request: SignUpRequest): Call<PayloadResponse<SignUpResponsePayload>>

    @JsonClass(generateAdapter = true)
    data class SignUpRequest(
            var nickname: String,
            var email: String,
            var mobile: String,
            var password: String
    )

    @JsonClass(generateAdapter = true)
    data class SignUpResponsePayload(
            @Json(name = "user_id") var userId: Long
    )

    @POST("/auth/signup")
    fun logout(): Call<EmptyResponse>

    @POST("/mobile/signup")
    fun mobileSignUp(@Body request: MobileSignUpRequest): Call<EmptyResponse>

    @JsonClass(generateAdapter = true)
    data class MobileSignUpRequest(
            var mobile: String
    )
}
