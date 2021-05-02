package cn.edu.tsinghua.thss.cercis.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Transformations
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cn.edu.tsinghua.thss.cercis.dao.ChatDao
import cn.edu.tsinghua.thss.cercis.entity.Chat
import cn.edu.tsinghua.thss.cercis.entity.ChatType.CHAT_SINGLE
import cn.edu.tsinghua.thss.cercis.repository.AuthRepository
import cn.edu.tsinghua.thss.cercis.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
        application: Application,
        private val messageRepository: MessageRepository,
        private val chatDao: ChatDao,
        private val authRepository: AuthRepository,
) : AndroidViewModel(application) {
    val sessions = chatDao.loadAllChats().asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)
    val chatListItemList = Transformations.map(sessions) {
        it?.map {
            ChatListItemData(
                sessionId = it.id,
                avatar = "",
                sessionName = it.name,
                latestMessage = generateTimeString(),
                lastUpdate = "18:54",
                unreadCount = 20,
            )
        }
    }

    fun generateTimeString(): String {
        val pattern = "yyyy-MM-dd HH:mm:ss"
        val simpleDateFormat = SimpleDateFormat(pattern)
        val date: String = simpleDateFormat.format(Date())
        return date
    }

    fun onRefreshListener() {
        // TODO replace fake data with real ones
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.insertChat(*(1..20).map { it.toLong() }.map {
                ChatListItemData(
                    sessionId = it,
                    avatar = "",
                    sessionName = "${authRepository.userId} #$it",
                    latestMessage = generateTimeString(),
                    lastUpdate = "18:54",
                    unreadCount = 20,
                )
            }.map {
                Chat(
                    id = it.sessionId,
                    type = CHAT_SINGLE,
                    name = it.sessionName,
                    lastMessage = it.latestMessage
                )
            }.toTypedArray())
        }
        Log.d(null, "Some junk data generated!")
    }
}
