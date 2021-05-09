package cn.cercis.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import cn.cercis.R
import cn.cercis.common.ChatId
import cn.cercis.common.MESSAGE_DIGEST_LENGTH
import cn.cercis.entity.Chat
import cn.cercis.entity.ChatType.CHAT_PRIVATE
import cn.cercis.entity.Message
import cn.cercis.entity.MessageType
import cn.cercis.entity.asMessageType
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.NotificationRepository
import cn.cercis.repository.UserRepository
import cn.cercis.util.getString
import cn.cercis.util.helper.coroutineContext
import cn.cercis.util.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val hashMap = HashMap<ChatId, LiveData<CommonListItemData?>>()
    private val chatRefreshTime = MutableStateFlow<Long>(System.currentTimeMillis())
    private val chatListFlow: Flow<Resource<List<Chat>>> = chatRefreshTime.flatMapLatest {
        messageRepository.getAllChats().flow()
    }
    private val chatListLiveData = chatListFlow.asLiveData(coroutineContext)
    val chatListData = chatListLiveData.map { it.data ?: listOf() }

    private fun generateTimeString(): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        return dateFormat.format(Date())
    }

    fun onRefreshListener() {
        refresh()
    }

    /**
     * Gets the display info for a chat.
     *
     * A null value indicates loading.
     */
    fun getChatDisplay(chat: Chat): LiveData<CommonListItemData?> {
        return hashMap.computeIfAbsent(chat.id) { chatId ->
            messageRepository.getLatestMessage(chatId).flatMapLatest { msg ->
                when (chat.type) {
                    CHAT_PRIVATE -> {
                        messageRepository.getOtherUserId(
                            authRepository.currentUserId,
                            chatId
                        ).flatMapLatest {
                            it.data?.let { userId ->
                                userRepository.getUserWithFriendDisplay(userId, true)
                            } ?: MutableStateFlow(null)
                        }
                    }
                    else -> MutableStateFlow(
                        CommonListItemData(
                            avatar = chat.avatar,
                            displayName = chat.name,
                            description = digest(msg),
                        )
                    )
                }
            }.asLiveData(coroutineContext)
        }
    }

    private fun refresh() {
        chatRefreshTime.value = System.currentTimeMillis()
    }

    private fun digest(message: Message?): String {
        return when (message?.type?.asMessageType()) {
            MessageType.TEXT -> message.content.substring(0, MESSAGE_DIGEST_LENGTH)
            MessageType.IMAGE -> "[${getString(R.string.message_type_image)}]"
            MessageType.AUDIO -> "[${getString(R.string.message_type_audio)}]"
            MessageType.VIDEO -> "[${getString(R.string.message_type_video)}]"
            MessageType.LOCATION -> "[${getString(R.string.message_type_location)}]"
            MessageType.UNKNOWN -> "[${getString(R.string.message_type_unknown)}]"
            null -> "[${getString(R.string.message_type_unknown)}]"
        }
    }
}