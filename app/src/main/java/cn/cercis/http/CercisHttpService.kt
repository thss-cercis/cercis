package cn.cercis.http

import cn.cercis.common.ApplyId
import cn.cercis.common.UserId
import cn.cercis.entity.Activity
import cn.cercis.entity.FriendRequest
import cn.cercis.entity.UserDetail
import cn.cercis.util.resource.NetworkResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface CercisHttpService {
    @POST("mobile/signup")
    suspend fun sendSignUpSms(@Body request: SendSmsRequest): EmptyNetworkResponse

    @POST("auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): NetworkResponse<SignUpPayload>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): NetworkResponse<LoginPayload>

    @POST("auth/logout")
    suspend fun logout(): EmptyNetworkResponse

    @GET("user/current")
    suspend fun getUserDetail(): NetworkResponse<UserDetail>

    @PUT("user/modify")
    suspend fun updateUserDetail(@Body request: UpdateUserDetailRequest): EmptyNetworkResponse

    @PUT("user/password")
    suspend fun updatePassword(@Body request: UpdatePasswordRequest): EmptyNetworkResponse

    @GET("user/info")
    suspend fun getUser(@Query("id") userId: UserId): NetworkResponse<WrappedUserPayload>

    @GET("search/users")
    suspend fun searchUser(
        @Query("id") userId: UserId?,
        @Query("mobile") mobile: String?,
        @Query("nickname") nickname: String?,
        @Query("offset") offset: Long?,
        @Query("limit") limit: Long?,
    ): NetworkResponse<WrappedSearchUserPayload>

    @GET("friend/")
    suspend fun getFriendList(): NetworkResponse<WrappedFriendListPayload>

    @POST("friend/send")
    suspend fun addFriend(@Body request: AddFriendRequest): EmptyNetworkResponse

    @GET("friend/send")
    suspend fun getFriendRequestSentList(): NetworkResponse<WrappedFriendRequestListPayload>

    @GET("friend/receive")
    suspend fun getFriendRequestReceivedList(): NetworkResponse<WrappedFriendRequestListPayload>

    @POST("friend/accept")
    suspend fun acceptAddingFriend(@Body request: AcceptAddingFriendRequest): EmptyNetworkResponse

    @POST("friend/reject")
    suspend fun rejectAddingFriend(@Body request: RejectAddingFriendRequest): EmptyNetworkResponse

    @PUT("friend/")
    suspend fun updateFriendRemark(@Body request: UpdateFriendRemarkRequest): EmptyNetworkResponse

    @DELETE("friend/")
    suspend fun deleteFriend(@Body request: DeleteFriendRequest): EmptyNetworkResponse

    @POST("mobile/recover")
    suspend fun sendPasswordResetSms(@Body request: SendSmsRequest): EmptyNetworkResponse

    @POST("auth/recover")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): EmptyNetworkResponse

    @GET("activity/")
    suspend fun getActivityList(): NetworkResponse<WrappedActivityListPayload>

    @POST("activity/like")
    suspend fun likeActivity(@Body request: LikeActivityRequest): EmptyNetworkResponse
}

@JsonClass(generateAdapter = true)
class EmptyPayload

typealias EmptyNetworkResponse = NetworkResponse<EmptyPayload>

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val id: UserId?,
    val mobile: String?,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class LoginPayload(
    val id: UserId,
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
    @Json(name = "user_id") val id: UserId,
)

@JsonClass(generateAdapter = true)
data class SendSmsRequest(
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
data class UpdatePasswordRequest(
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
data class WrappedSearchUserPayload(
    val users: List<UserSearchResult>,
) {
    @JsonClass(generateAdapter = true)
    data class UserSearchResult(
        val id: UserId,
        val mobile: String,
        // TODO wait for ayajike to add this
        // val avatar: String,
        val nickname: String,
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
data class WrappedFriendRequestListPayload(
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

@JsonClass(generateAdapter = true)
data class AddFriendRequest(
    @Json(name = "to_id") val id: UserId,
    val remark: String?,
    @Json(name = "alias") val displayName: String?,
)

@JsonClass(generateAdapter = true)
data class UpdateFriendRemarkRequest(
    @Json(name = "friend_id") val id: UserId,
    val remark: String?,
    @Json(name = "alias") val displayName: String?,
)

@JsonClass(generateAdapter = true)
data class DeleteFriendRequest(
    @Json(name = "friend_id") val id: UserId,
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    val mobile: String,
    @Json(name = "new_pwd") val newPassword: String,
    @Json(name = "code") val verificationCode: String,
)

@JsonClass(generateAdapter = true)
data class WrappedActivityListPayload(
    val activities: List<Activity>,
)

@JsonClass(generateAdapter = true)
data class LikeActivityRequest(
    val id: String,
)
