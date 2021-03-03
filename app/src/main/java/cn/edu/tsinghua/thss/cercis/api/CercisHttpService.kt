package cn.edu.tsinghua.thss.cercis.api

import androidx.room.PrimaryKey
import cn.edu.tsinghua.thss.cercis.dao.CurrentUser
import cn.edu.tsinghua.thss.cercis.util.ChatId
import cn.edu.tsinghua.thss.cercis.util.UserId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface CercisHttpService {

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<EmptyResponse>

    @POST("auth/signup")
    fun signUp(@Body request: SignUpRequest): Call<SignUpResponse>

    @POST("auth/signup")
    fun logout(): Call<EmptyResponse>

    @POST("mobile/signup")
    fun mobileSignUp(@Body request: MobileSignUpRequest): Call<EmptyResponse>

    @POST("user/current")
    fun userCurrent(): Call<UserCurrentResponse>
}

@JsonClass(generateAdapter = true)
data class LoginRequest(
        val id: String,
        val password: String
)

@JsonClass(generateAdapter = true)
data class SignUpRequest(
        val nickname: String,
        val email: String,
        val mobile: String,
        val password: String
)

@JsonClass(generateAdapter = true)
data class SignUpResponsePayload(
        @Json(name = "user_id") val userId: Long
)

typealias SignUpResponse = PayloadResponse<SignUpResponsePayload>

@JsonClass(generateAdapter = true)
data class MobileSignUpRequest(
        val mobile: String
)

typealias UserCurrentResponse = PayloadResponse<CurrentUser>