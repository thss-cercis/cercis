package cn.cercis.viewmodel

import android.util.Log
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cn.cercis.dao.ChatDao
import cn.cercis.entity.Chat
import cn.cercis.entity.ChatType.CHAT_SINGLE
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.NotificationRepository
import cn.cercis.util.getCurrentTimeString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
        private val messageRepository: MessageRepository,
        private val notificationRepository: NotificationRepository,
        private val chatDao: ChatDao,
        private val authRepository: AuthRepository,
) : ViewModel() {
    private val chats = chatDao.loadAllChats()
        .asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)

    val chatListItems = Transformations.map(chats) {
        it?.map { item ->
            ChatListItemData(
                chatId = item.id,
                avatar = "",
                chatName = item.name,
                latestMessage = item.lastMessage,
                lastUpdate = "00:00",
                unreadCount = 20,
            )
        }
    }

    fun onRefreshListener() {
        // TODO replace fake data with real ones
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.insertChat(*(1L..20L).map {
                Chat(
                    id = it,
                    type = CHAT_SINGLE,
                    name = "Test $it",
                    lastMessage = "${authRepository.currentUserId} @ ${getCurrentTimeString()}"
                )
            }.toTypedArray())
            Log.d(null, "Some junk data generated!")
        }
    }
}
