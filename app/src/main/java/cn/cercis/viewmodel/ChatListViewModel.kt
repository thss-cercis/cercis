package cn.cercis.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cn.cercis.common.ChatId
import cn.cercis.common.LOG_TAG
import cn.cercis.entity.Chat
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.NotificationRepository
import cn.cercis.repository.UserRepository
import cn.cercis.service.WSMessage
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
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val hashMap = HashMap<ChatId, LiveData<CommonListItemData?>>()
    private val chatRefreshTime = MutableStateFlow(System.currentTimeMillis())
    private val chatListFlow: Flow<Resource<List<Chat>>> = chatRefreshTime.flatMapLatest {
        messageRepository.getAllChats().flow()
    }
    val chatListData = chatListFlow.map { it.data }.filterNotNull().asLiveData(coroutineContext)

    private fun generateTimeString(): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        return dateFormat.format(Date())
    }

    fun onRefreshListener() {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.submitWSMessage(WSMessage.ForceUpdate)
        }
        refresh()
    }

    fun getChatDisplay(chat: Chat): LiveData<CommonListItemData?> {
        return hashMap.computeIfAbsent(chat.id) {
            Log.d(LOG_TAG, "recreated live data (chat: ${chat.id})")
            messageRepository.getChatDisplay(authRepository.currentUserId, chat)
                .asLiveData(coroutineContext)
        }
    }

    private fun refresh() {
        chatRefreshTime.value = System.currentTimeMillis()
    }
}
