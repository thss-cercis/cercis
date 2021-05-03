package cn.cercis.service

import android.app.Service
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.IBinder
import android.util.Log
import cn.cercis.Constants.WSS_MESSAGES
import cn.cercis.entity.Chat
import cn.cercis.repository.MessageRepository
import cn.cercis.util.LOG_TAG
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.retry.ExponentialWithJitterBackoffStrategy
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import io.reactivex.disposables.Disposable
import okhttp3.OkHttpClient
import javax.inject.Inject

//@AndroidEntryPoint
class MessageService : Service() {
    private val disposables = ArrayList<Disposable>()
    private lateinit var socketService: CercisWebSocketService
    private lateinit var db: SQLiteDatabase

    @Inject
    lateinit var messageRepository: MessageRepository
    @Inject
    lateinit var okHttpClient: OkHttpClient

    enum class ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        UPDATING,
        CONNECTED,
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        // initializes a WebSocket connection
        val lifecycle = AndroidLifecycle.ofApplicationForeground(application)
        val backoffStrategy = ExponentialWithJitterBackoffStrategy(5000, 5000)

        val scarlet = Scarlet.Builder()
                .webSocketFactory(okHttpClient.newWebSocketFactory(WSS_MESSAGES))
                .addMessageAdapterFactory(MoshiMessageAdapter.Factory())
                .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
                .backoffStrategy(backoffStrategy)
                .lifecycle(lifecycle)
                .build()

        socketService = scarlet.create()
        messageRepository.submitConnectionStatus(ConnectionStatus.CONNECTING)

        disposables += socketService.observeWebSocketEvent().subscribe({ event ->
            if (event is WebSocket.Event.OnConnectionOpened<*>) {
                messageRepository.submitConnectionStatus(ConnectionStatus.CONNECTED)
            }
        }, { error ->
            Log.e(LOG_TAG, "Error while observing socket ${error.cause}")
        })

        disposables += socketService.receiveChatsUpdate().subscribe({ update ->
            insertMessages(update)
        }, { error ->
            Log.e(LOG_TAG, "Error while receiving update ${error.cause}")
        })

        disposables += socketService.receiveSendMessageResponseMessage().subscribe({ resp ->

        }, { error ->
            Log.e(LOG_TAG, "Error while receiving response ${error.cause}")
        })

        // val dbHelper = ChatDbHelper(this, null);
        // this.db = dbHelper.writableDatabase
    }

    private fun sendInitMessage() {
        // TODO finish this
        socketService.sendInitMessage(InitMessageFromClient(0))
    }

    private fun insertMessages(update: ChatsUpdateMessage) {
        // TODO finish this
    }

    override fun onDestroy() {
        db.close()
        disposables.forEach { it.dispose() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    /**
     * Get all chats managed by the service.
     */
    fun getAllChats(): List<Chat> {
        // TODO finish this
        return ArrayList()
    }
}
