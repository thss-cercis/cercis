package cn.cercis.http

import cn.cercis.common.*
import cn.cercis.entity.*
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
    suspend fun updateFriendDisplayName(@Body request: UpdateFriendDisplayNameRequest): EmptyNetworkResponse

    @HTTP(method = "DELETE", path = "friend/", hasBody = true)
    suspend fun deleteFriend(@Body request: DeleteFriendRequest): EmptyNetworkResponse

    @POST("chat/private")
    suspend fun createPrivateChat(@Body request: CreatePrivateChatRequest): NetworkResponse<Chat>

    @POST("chat/group")
    suspend fun createGroupChat(@Body request: CreateGroupChatRequest): NetworkResponse<Chat>

    @GET("chat/all")
    suspend fun getChatList(): NetworkResponse<List<Chat>>

    @GET("chat/private")
    suspend fun getPrivateChatWith(@Query("user_id") userId: UserId): NetworkResponse<Chat>

    @PUT("chat/group")
    suspend fun editGroupChatInfo(@Body request: EditGroupChatInfoRequest): NetworkResponse<Chat>

    @GET("chat/")
    suspend fun getChatMemberList(@Query("chat_id") chatId: ChatId): NetworkResponse<List<ChatMember>>

    @POST("chat/group/member")
    suspend fun inviteGroupMember(@Body request: InviteGroupMemberRequest): EmptyNetworkResponse

    @PUT("group/member/alias")
    suspend fun editInGroupDisplayName(@Body request: EditInGroupDisplayNameRequest): EmptyNetworkResponse

    @PUT("chat/group/member/perm")
    suspend fun editGroupMemberPermission(@Body request: EditGroupMemberPermissionRequest): EmptyNetworkResponse

    @PUT("chat/group/member/owner")
    suspend fun giveawayGroupOwner(@Body request: GiveAwayGroupOwnerRequest): EmptyNetworkResponse

    @HTTP(method = "DELETE", path = "chat/group/member", hasBody = true)
    suspend fun deleteGroupMember(@Body request: DeleteGroupMemberRequest): EmptyNetworkResponse

    @POST("chat/message")
    suspend fun sendMessage(@Body request: SendMessageRequest): NetworkResponse<Message>

    @GET("chat/message")
    suspend fun getSingleMessage(
        @Query("chat_id") chatId: ChatId,
        @Query("message_id") messageId: MessageId,
    ): NetworkResponse<Message>

    @GET("chat/messages")
    suspend fun getRangeMessages(
        @Query("chat_id") chatId: ChatId,
        @Query("from_id") fromId: MessageId,
        @Query("to_id") toId: MessageId,
    ): NetworkResponse<List<Message>>

    @GET("chat/messages/all-latest")
    suspend fun getAllChatsLatestMessageId(): NetworkResponse<List<ChatLatestMessageId>>

    @POST("chat/messages/latest")
    suspend fun getChatsLatestMessages(@Body request: GetChatsLatestMessagesRequest): NetworkResponse<List<Message>>

    @POST("chat/message/withdraw")
    suspend fun withdrawMessage(@Body request: WithdrawMessageRequest): EmptyNetworkResponse

    @POST("mobile/recover")
    suspend fun sendPasswordResetSms(@Body request: SendSmsRequest): EmptyNetworkResponse

    @POST("auth/recover")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): EmptyNetworkResponse

    @GET("upload")
    suspend fun getUploadToken(): NetworkResponse<UploadTokenResponse>

    @GET("activity/after")
    suspend fun getActivityList(
        @Query("activity_id") activityId: ActivityId = 1
    ): NetworkResponse<List<ActivityPayload>>

    @POST("activity")
    suspend fun publishActivity(@Body request: PublishActivityRequest): EmptyNetworkResponse

    @POST("activity/thumbup")
    suspend fun thumbUpActivity(@Body request: ActivityRequest): EmptyNetworkResponse

    @HTTP(method = "DELETE", path = "activity/thumbup", hasBody = true)
    suspend fun undoThumbUpActivity(@Body request: ActivityRequest): EmptyNetworkResponse

    @POST("activity/comment")
    suspend fun addActivityComment(@Body request: ActivityCommentRequest): EmptyNetworkResponse

    @HTTP(method = "DELETE", path = "activity", hasBody = true)
    suspend fun deleteActivity(@Body request: ActivityRequest): EmptyNetworkResponse
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
        val avatar: String,
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
data class UpdateFriendDisplayNameRequest(
    @Json(name = "friend_id") val id: UserId,
    @Json(name = "alias") val displayName: String?,
)

@JsonClass(generateAdapter = true)
data class DeleteFriendRequest(
    @Json(name = "friend_id") val id: UserId,
)

@JsonClass(generateAdapter = true)
data class CreatePrivateChatRequest(
    val id: UserId,
)

@JsonClass(generateAdapter = true)
data class EditGroupChatInfoRequest(
    @Json(name = "chat_id") val chatId: ChatId,
    val name: String?,
    val avatar: String?,
)

@JsonClass(generateAdapter = true)
data class CreateGroupChatRequest(
    val name: String,
    @Json(name = "member_ids") val memberIds: List<UserId>?,
)

@JsonClass(generateAdapter = true)
data class InviteGroupMemberRequest(
    @Json(name = "chat_id") val chatId: ChatId,
    @Json(name = "user_id") val userId: UserId,
)

@JsonClass(generateAdapter = true)
data class EditInGroupDisplayNameRequest(
    @Json(name = "chat_id") val chatId: ChatId,
    @Json(name = "user_id") val userId: UserId,
    @Json(name = "alias") val displayName: String,
)

@JsonClass(generateAdapter = true)
data class EditGroupMemberPermissionRequest(
    @Json(name = "chat_id") val chatId: ChatId,
    @Json(name = "user_id") val userId: UserId,
    // GroupChatPermission
    val permission: Int,
)

@JsonClass(generateAdapter = true)
data class GiveAwayGroupOwnerRequest(
    @Json(name = "chat_id") val chatId: ChatId,
    @Json(name = "user_id") val userId: UserId,
)

@JsonClass(generateAdapter = true)
data class DeleteGroupMemberRequest(
    @Json(name = "chat_id") val chatId: ChatId,
    @Json(name = "user_id") val userId: UserId,
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @Json(name = "chat_id") val chatId: ChatId,
    val type: Int,
    val message: String,
)

@JsonClass(generateAdapter = true)
data class ChatLatestMessageId(
    @Json(name = "chat_id") val chatId: ChatId,
    @Json(name = "max_message_id") val latestMessageId: MessageId,
)

@JsonClass(generateAdapter = true)
data class WithdrawMessageRequest(
    @Json(name = "chat_id") val chatId: ChatId,
    @Json(name = "message_id") val messageId: MessageId,
)

@JsonClass(generateAdapter = true)
data class GetChatsLatestMessagesRequest(
    @Json(name = "chat_ids") val chatIds: List<ChatId>,
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
    val mobile: String,
    @Json(name = "new_pwd") val newPassword: String,
    @Json(name = "code") val verificationCode: String,
)

@JsonClass(generateAdapter = true)
data class ActivityPayload(
    val id: ActivityId,
    val text: String,
    @Json(name = "sender_id") val userId: UserId,
    val media: List<MediaPayload>,
    val comments: List<Comment>,
    @Json(name = "thumb_ups") val thumbUps: List<ThumbUp>,
    @Json(name = "created_at") val createdAt: String,
) {
    @JsonClass(generateAdapter = true)
    data class MediaPayload(
        val id: MediumId,
        @Json(name = "activity_id") val activityId: ActivityId,
        val type: Int,
        val content: String,
        @Json(name = "created_at") val createdAt: String,
    )
}

@JsonClass(generateAdapter = true)
data class ActivityRequest(
    @Json(name = "activity_id") val activityId: ActivityId,
)

@JsonClass(generateAdapter = true)
data class ActivityCommentRequest(
    @Json(name = "activity_id") val activityId: ActivityId,
    val content: String,
)

@JsonClass(generateAdapter = true)
data class PublishActivityRequest(
    val text: String,
    val media: List<MediaRequest>?,
) {
    @JsonClass(generateAdapter = true)
    data class MediaRequest(
        val type: Int,
        val content: String,
    )

    constructor(text: String, type: Int, contents: List<String>) : this(text, when {
        contents.isEmpty() -> null
        else -> contents.map { MediaRequest(type, it) }
    })
}

@JsonClass(generateAdapter = true)
data class UploadTokenResponse(
    @Json(name = "upload_token") val uploadToken: String,
)
