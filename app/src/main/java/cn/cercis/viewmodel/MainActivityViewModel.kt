package cn.cercis.viewmodel

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.cercis.CercisApplication
import cn.cercis.MainActivity
import cn.cercis.R
import cn.cercis.common.ChatId
import cn.cercis.common.LOG_TAG
import cn.cercis.common.NOTIFICATION_CHANNEL_ID
import cn.cercis.entity.Chat
import cn.cercis.entity.asMessageType
import cn.cercis.repository.*
import cn.cercis.service.WSMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val messageRepository: MessageRepository,
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    val loggedIn = authRepository.loggedIn
    val atomicInteger = AtomicInteger(0)

    // this initial value is related to the one in [master_nav_graph]
    val detailHasNavigationDestination = MutableLiveData(true)
    val masterVisible = Transformations.map(detailHasNavigationDestination) {
        when (it) {
            null, false -> View.VISIBLE
            else -> View.GONE
        }
    }
    val detailVisible = Transformations.map(detailHasNavigationDestination) {
        when (it) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

    fun createOpenChatIntent(chatId: ChatId): Intent {
        return Intent(CercisApplication.application, MainActivity::class.java).apply {
            putExtra(ACTION_KEY, ACTION_OPEN_CHAT)
            putExtra(CHAT_ID_KEY, chatId)
        }
    }

    suspend fun getChat(chatId: ChatId): Chat? {
        return messageRepository.getChat(chatId).firstOrNull()
    }

    override fun onCleared() {
        super.onCleared()
        // TODO inform service to end itself
    }

    suspend fun createNewMessageNotification(
        chatId: ChatId,
        type: Int,
        content: String,
    ): Notification {
        return NotificationCompat.Builder(
            CercisApplication.application,
            NOTIFICATION_CHANNEL_ID,
        )
            .setContentIntent(PendingIntent.getActivity(
                CercisApplication.application,
                atomicInteger.getAndIncrement(),
                createOpenChatIntent(chatId),
                PendingIntent.FLAG_UPDATE_CURRENT
            ))
            /* TODO set icon .setSmallIcon(R.drawable.notification_icon) */
            // TODO get chat name
            .setSmallIcon(R.drawable.ic_cercis)
            .setContentTitle(messageRepository.getChat(chatId).first()?.name ?: "$chatId")
            .setContentText(messageRepository.digest(type.asMessageType(), content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
            .apply {
                flags = flags or Notification.FLAG_AUTO_CANCEL
            }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // set to current user to clear previous unprocessed messages
            notificationRepository.setCurrentUserId(authRepository.currentUserId)
            for (it in notificationRepository.messageChannel) {
                Log.d(LOG_TAG, "processing message: $it")
                when (val message = it.first) {
                    WSMessage.WebSocketConnected, WSMessage.ForceUpdate -> {
                        launch {
                            // load friends
                            friendRepository.getFriendListAndSave()
                                .apply { Log.d(LOG_TAG, "$this") }
                            // load chat list
                            messageRepository.getChatList().fetchAndSave()
                                .apply { Log.d(LOG_TAG, "$this") }
                            // load latest message list
                            messageRepository.fetchAndSaveLatestMessages()
                                .apply { Log.d(LOG_TAG, "$this") }
                            // saves self
                            userRepository.getUser(authRepository.currentUserId).fetchAndSave()
                        }
                    }
                    WSMessage.FriendListUpdated -> {
                        launch {
                            // prevent blocking message looper
                            friendRepository.getFriendListAndSave()
                        }
                    }
                    is WSMessage.FriendRequestReceived -> {
                        launch {
                            friendRepository.getFriendRequestReceivedList().fetchAndSave()
                        }
                    }
                    is WSMessage.NewMessageReceived -> {
                        launch {
                            messageRepository.saveSingleMessage(message.chatId, message.messageId)
                                .apply { Log.d(LOG_TAG, "$this") }
                                .apply {
                                    this.use {
                                        if (this.senderId != authRepository.currentUserId) {
                                            val notificationManager =
                                                NotificationManagerCompat.from(
                                                    CercisApplication.application
                                                )
                                            notificationManager.notify(atomicInteger.getAndIncrement(),
                                                createNewMessageNotification(message.chatId,
                                                    message.type,
                                                    message.sum))
                                        }
                                    }
                                }
                        }
                    }
                    is WSMessage.NewActivity -> {
                        // TODO
                    }
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            // re-fetch latest messages every 5000 ms
            while (true) {
                delay(20000)
                messageRepository.fetchAndSaveLatestMessages()
            }
        }
        messageRepository.processMessageSending(viewModelScope)
    }

    companion object {
        const val ACTION_KEY = "action"
        const val ACTION_OPEN_CHAT = "open_chat"
        const val CHAT_ID_KEY = "chat_id"
    }
}
