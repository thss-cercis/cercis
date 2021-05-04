package cn.cercis.http

import cn.cercis.common.ApplyId
import cn.cercis.common.UserId
import cn.cercis.entity.FriendRequest
import cn.cercis.entity.UserDetail
import cn.cercis.util.NetworkResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface CercisHttpService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): NetworkResponse<LoginPayload>

    @POST("auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): NetworkResponse<SignUpPayload>

    @POST("mobile/signup")
    suspend fun mobileSignUp(@Body request: MobileSignUpRequest): EmptyNetworkResponse

    @POST("auth/logout")
    suspend fun logout(): EmptyNetworkResponse

    @GET("user/current")
    suspend fun getUserDetail(): NetworkResponse<UserDetail>

    @PUT("user/modify")
    suspend fun updateUserDetail(@Body request: UpdateUserDetailRequest): EmptyNetworkResponse

    @PUT("user/password")
    suspend fun updateUserPassword(@Body request: UpdateUserPasswordRequest): EmptyNetworkResponse

    @GET("user/info")
    suspend fun getUser(@Query("id") userId: UserId): NetworkResponse<WrappedUserPayload>

    @GET("friend/")
    suspend fun getFriendList(): NetworkResponse<WrappedFriendListPayload>

    @GET("friend/send")
    suspend fun getFriendRequestSentList(): NetworkResponse<WrappedFriendRequestReceivedListPayload>

    @GET("friend/receive")
    suspend fun getFriendRequestReceivedList(): NetworkResponse<WrappedFriendRequestReceivedListPayload>

    @POST("friend/accept")
    suspend fun acceptAddingFriend(@Body request: AcceptAddingFriendRequest): EmptyNetworkResponse

    @POST("friend/reject")
    suspend fun rejectAddingFriend(@Body request: RejectAddingFriendRequest): EmptyNetworkResponse
}

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val id: UserId?,
    var mobile: String?,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class LoginPayload(
    @Json(name = "id") val userId: UserId,
)

@JsonClass(generateAdapter = true)
data class SignUpRequest(
    val nickname: String,
    val mobile: String,
    @Json(name = "code") val verificationCode: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class SignUpPayload(
    @Json(name = "user_id") val userId: UserId,
)

@JsonClass(generateAdapter = true)
data class MobileSignUpRequest(
    val mobile: String,
)

@JsonClass(generateAdapter = true)
data class UpdateUserDetailRequest(
    val nickname: String?,
    val email: String?,
    val avatar: String?,
    val bio: String?,
)

@JsonClass(generateAdapter = true)
data class UpdateUserPasswordRequest(
    @Json(name = "old_pwd") val oldPassword: String,
    @Json(name = "new_pwd") val newPassword: String,
)

@JsonClass(generateAdapter = true)
data class WrappedUserPayload(
    val user: UserPayload,
) {
    @JsonClass(generateAdapter = true)
    data class UserPayload(
        val nickname: String,
        val email: String,
        val mobile: String,
        val avatar: String,
        val bio: String,
    )
}

@JsonClass(generateAdapter = true)
data class WrappedFriendListPayload(
    val friends: List<FriendPayload>,
) {
    @JsonClass(generateAdapter = true)
    data class FriendPayload(
        @Json(name = "friend_id") val id: UserId,
        @Json(name = "alias") val displayName: String,
    )
}

@JsonClass(generateAdapter = true)
data class WrappedFriendRequestSentListPayload(
    @Json(name = "applies") val requests: List<FriendRequest>,
)

@JsonClass(generateAdapter = true)
data class WrappedFriendRequestReceivedListPayload(
    @Json(name = "applies") val requests: List<FriendRequest>,
)

@JsonClass(generateAdapter = true)
data class AcceptAddingFriendRequest(
    @Json(name = "apply_id") val applyId: ApplyId,
    @Json(name = "alias") val displayName: String?,
)

@JsonClass(generateAdapter = true)
data class RejectAddingFriendRequest(
    @Json(name = "apply_id") val applyId: ApplyId,
)
