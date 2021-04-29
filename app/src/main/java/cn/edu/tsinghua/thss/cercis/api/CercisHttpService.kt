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
    suspend fun login(@Body request: LoginRequest): EmptyResponse

    @POST("auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): SignUpResponse

    @POST("auth/signup")
    suspend fun logout(): EmptyResponse

    @POST("mobile/signup")
    suspend fun mobileSignUp(@Body request: MobileSignUpRequest): EmptyResponse

//    @POST("mobile/signup/check")
//    suspend fun mobileSignUpCheck(@Body request: MobileSignUpCheckRequest): MobileSignUpCheckResponse

    @POST("user/current")
    suspend fun userCurrent(): UserCurrentResponse
}

@JsonClass(generateAdapter = true)
data class LoginRequest(
        val id: String,
        val password: String
)

@JsonClass(generateAdapter = true)
data class SignUpRequest(
        val nickname: String,
        val mobile: String,
        @Json(name = "code") val verificationCode: String,
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

@JsonClass(generateAdapter = true)
data class EmailSignUpRequest(
        val email: String
)

@JsonClass(generateAdapter = true)
data class MobileSignUpCheckRequest(
        val code: String
)

@JsonClass(generateAdapter = true)
data class EmailSignUpCheckRequest(
        val code: String
)

@JsonClass(generateAdapter = true)
data class MobileSignUpCheckResponsePayload(
        val ok: Boolean
)

typealias MobileSignUpCheckResponse = PayloadResponse<MobileSignUpCheckResponsePayload>

@JsonClass(generateAdapter = true)
data class EmailSignUpCheckResponsePayload(
        val ok: Boolean
)

typealias EmailSignUpCheckResponse = PayloadResponse<EmailSignUpCheckResponsePayload>

typealias UserCurrentResponse = PayloadResponse<CurrentUser>
