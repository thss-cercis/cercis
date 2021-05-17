package cn.cercis.service

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.Transformations
import androidx.lifecycle.lifecycleScope
import cn.cercis.Constants.WSS_BASE
import cn.cercis.common.LOG_TAG
import cn.cercis.repository.AuthRepository
import cn.cercis.repository.MessageRepository
import cn.cercis.repository.NotificationRepository
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.retry.ExponentialWithJitterBackoffStrategy
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.scarlet.websocket.okhttp.request.RequestFactory
import com.tinder.streamadapter.coroutines.CoroutinesStreamAdapterFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : LifecycleService() {
    private lateinit var socketService: CercisWebSocketService

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var cookieJar: PersistentCookieJar

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        UPDATING,
        CONNECTED,
    }

    inner class LoggedInLifecycle constructor(
        lifecycleRegistry: LifecycleRegistry = LifecycleRegistry()
    ) : Lifecycle by lifecycleRegistry {
        init {
            LiveDataReactiveStreams.toPublisher(
                { lifecycle },
                Transformations.map(authRepository.loggedIn) {
                    if (it == false) {
                        Lifecycle.State.Stopped.WithReason()
                    } else {
                        Lifecycle.State.Started
                    }
                })
                .subscribe(lifecycleRegistry)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "Service starting...")

        // initializes a WebSocket connection
        val lifecycle = AndroidLifecycle.ofServiceStarted(application, this, 1000)
            .combineWith(LoggedInLifecycle())
        val backoffStrategy = ExponentialWithJitterBackoffStrategy(5000, 5000)

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .pingInterval(5, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .build()
        val scarlet = Scarlet.Builder()
            .webSocketFactory(okHttpClient.newWebSocketFactory(object : RequestFactory {
                override fun createRequest(): Request {
                    val sessionId =
                        cookieJar.loadForRequest(WSS_BASE.replaceFirst("ws", "http").toHttpUrl())
                            .find {
                                it.name == "session_id"
                            }?.value ?: ""
                    Log.d(LOG_TAG, "session_id: $sessionId")
                    return Request.Builder().url("$WSS_BASE?session_id=${sessionId}").build()
                }
            }))
            .addMessageAdapterFactory(MoshiMessageAdapter.Factory())
            .addStreamAdapterFactory(CoroutinesStreamAdapterFactory())
            .backoffStrategy(backoffStrategy)
            .lifecycle(lifecycle)
            .build()

        socketService = scarlet.create()
        notificationRepository.submitConnectionStatus(ConnectionStatus.CONNECTING)

        lifecycleScope.launch(Dispatchers.IO) {
            socketService.observeWebSocketEvent().consumeEach { event ->
                when (event) {
                    is WebSocket.Event.OnConnectionOpened<*> -> {
                        notificationRepository.submitConnectionStatus(ConnectionStatus.CONNECTED)
                        notificationRepository.submitWSMessage(WSMessage.WebSocketConnected)
                    }
                    is WebSocket.Event.OnConnectionClosed -> {
                        notificationRepository.submitConnectionStatus(ConnectionStatus.DISCONNECTED)
                    }
                    is WebSocket.Event.OnConnectionFailed -> {
                        notificationRepository.submitConnectionStatus(ConnectionStatus.DISCONNECTED)
                    }
                    else -> {
                    }
                }
                Log.d(LOG_TAG, "$event")
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            socketService.observeWebSocketMessage().consumeEach {
                Log.d(LOG_TAG, "trying to submit $it as ${it.get()}")
                it.get()?.let { msg ->
                    Log.d(LOG_TAG, "sending")
                    notificationRepository.submitWSMessage(msg)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }
}
