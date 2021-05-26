package cn.cercis.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.cercis.common.LOG_TAG
import cn.cercis.repository.*
import cn.cercis.service.WSMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val messageRepository: MessageRepository,
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
) : ViewModel() {
    val loggedIn = authRepository.loggedIn

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

    override fun onCleared() {
        super.onCleared()
        // TODO inform service to end itself
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
                            messageRepository.getSingleMessage(message.chatId, message.messageId)
                                .fetchAndSave().apply { Log.d(LOG_TAG, "$this") }
                            messageRepository.getChatList().fetchAndSave()
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
}
