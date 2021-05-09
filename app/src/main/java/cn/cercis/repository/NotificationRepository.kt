package cn.cercis.repository

import androidx.lifecycle.MutableLiveData
import cn.cercis.service.NotificationService
import cn.cercis.service.WSMessage
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor() {
    val connectionStatus = MutableLiveData(NotificationService.ConnectionStatus.DISCONNECTED)
    val messageIndex = AtomicLong(0)
    private val messageChannel = Channel<Pair<WSMessage, Long>>(
        capacity = 50,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    suspend fun submitWSMessage(message: WSMessage) {
        messageChannel.send(message to messageIndex.addAndGet(1))
    }

    fun messageFlow() = messageChannel.receiveAsFlow()

    /**
     * Submits connection status.
     *
     * Called from [NotificationService].
     */
    fun submitConnectionStatus(connectionStatus: NotificationService.ConnectionStatus) {
        this.connectionStatus.postValue(connectionStatus)
    }
}