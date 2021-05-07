package cn.cercis.repository

import androidx.lifecycle.MutableLiveData
import cn.cercis.service.NotificationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(){
    val connectionStatus = MutableLiveData(NotificationService.ConnectionStatus.DISCONNECTED)

    /**
     * Submits connection status.
     *
     * Called from [NotificationService].
     */
    fun submitConnectionStatus(connectionStatus: NotificationService.ConnectionStatus) {
        this.connectionStatus.postValue(connectionStatus)
    }
}