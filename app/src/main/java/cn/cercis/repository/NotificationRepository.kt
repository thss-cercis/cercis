package cn.cercis.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import cn.cercis.common.LOG_TAG
import cn.cercis.common.UserId
import cn.cercis.service.NotificationService
import cn.cercis.service.WSMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor() {
    val connectionStatus = MutableLiveData(NotificationService.ConnectionStatus.DISCONNECTED)
    val messageIndex = AtomicLong(0)
    var messageChannel = createChannel()

    suspend fun submitWSMessage(message: WSMessage) {
        messageChannel.send(message to messageIndex.addAndGet(1))
        Log.d(LOG_TAG, "submitted message $message")
    }

    private var currentUserId = AtomicLong(-1L)

    /**
     * Clears previous user's message channel.
     */
    fun setCurrentUserId(currentUserId: UserId) {
        if (this.currentUserId.getAndSet(currentUserId) != -1L) {
            messageChannel = createChannel()
        }
    }

    /**
     * Submits connection status.
     *
     * Called from [NotificationService].
     */
    fun submitConnectionStatus(connectionStatus: NotificationService.ConnectionStatus) {
        this.connectionStatus.postValue(connectionStatus)
    }

    companion object {
        private fun createChannel(): Channel<Pair<WSMessage, Long>> {
            return Channel(
                capacity = 50,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        }
    }
}