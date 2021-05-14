package cn.cercis.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.cercis.common.LOG_TAG
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.FriendRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.NotificationRepository
import cn.cercis.service.WSMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository,
    private val messageRepository: MessageRepository,
    private val friendRepository: FriendRepository,
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            notificationRepository.messageFlow().collect {
                Log.d(LOG_TAG, "processing message: $it")
                when (val message = it.first) {
                    WSMessage.WebSocketConnected -> {
                        launch {
                            // load friends
                            friendRepository.getFriendList().fetchAndSave().apply { Log.d(LOG_TAG, "$this") }
                            // load chat list
                            messageRepository.getChatList().fetchAndSave().apply { Log.d(LOG_TAG, "$this") }
                            // load latest message list
                            messageRepository.fetchAndSaveLatestMessages().apply { Log.d(LOG_TAG, "$this") }
                        }
                    }
                    WSMessage.FriendListUpdated -> {
                        launch {
                            // prevent blocking message looper
                            friendRepository.getFriendList().fetchAndSave()
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
                                .fetchAndSave()
                        }
                    }
                    is WSMessage.NewActivity -> {
                        // TODO
                    }
                }
            }
        }
    }
}
