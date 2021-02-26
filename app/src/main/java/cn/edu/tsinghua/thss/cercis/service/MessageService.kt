package cn.edu.tsinghua.thss.cercis.service

import android.app.Service
import android.content.ContentValues.TAG
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import cn.edu.tsinghua.thss.cercis.Constants.WSS_MESSAGES
import cn.edu.tsinghua.thss.cercis.entity.Chat
import cn.edu.tsinghua.thss.cercis.sqlite.ChatDbHelper
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.jackson.JacksonMessageAdapter
import com.tinder.scarlet.retry.ExponentialWithJitterBackoffStrategy
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import io.reactivex.disposables.Disposable
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MessageService : Service() {
    private val binder: ClientBinder = ClientBinder()
    private val disposables = ArrayList<Disposable>()
    private var listener: MessageServiceListener? = null
    private lateinit var db: SQLiteDatabase

    inner class ClientBinder : Binder() {
        @Throws(RemoteException::class)
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            // TODO finish this
            return super.onTransact(code, data, reply, flags)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onCreate() {
        // initializes a WebSocket connection
        val lifecycle = AndroidLifecycle.ofApplicationForeground(application)
        val backoffStrategy = ExponentialWithJitterBackoffStrategy(5000, 5000)

        val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
        val scarlet = Scarlet.Builder()
                .webSocketFactory(okHttpClient.newWebSocketFactory(WSS_MESSAGES))
                .addMessageAdapterFactory(JacksonMessageAdapter.Factory())
                .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
                .backoffStrategy(backoffStrategy)
                .lifecycle(lifecycle)
                .build()

        val socketService = scarlet.create<CercisWebSocketService>()

        disposables += socketService.observeWebSocketEvent().subscribe({ event ->
            if (event is WebSocket.Event.OnConnectionOpened<*>) {
                socketService.sendInit(InitMessage(List(0) { ChatLatestStatus(0, 0) }))
            }
        }, { error ->
            Log.e(TAG, "Error while observing socket ${error.cause}")
        })

        disposables += socketService.receiveUpdate().subscribe({ update ->

        }, { error ->
            Log.e(TAG, "Error while receiving update ${error.cause}")
        })

        val dbHelper = ChatDbHelper(this, null);
        this.db = dbHelper.writableDatabase
        // TODO finish this
    }

    override fun onDestroy() {
        db.close()
        disposables.forEach { it.dispose() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY;
    }

    // listener functions
    /**
     * A listener on upcoming messages.
     * Only one such listener can be bounded to MessageService at once.
     *
     * Do not throw any exception in the listener handler method! Such exceptions would be
     * considered a listener fault leading to the listener being abandoned.
     */
    interface MessageServiceListener {
        /**
         * Called when new messages arrived at the client from UI Thread.
         */
        fun onChatUpdate(/* TODO: finish event implementation */)

        /**
         * Called when client status changed from UI Thread.
         */
        fun onStatusChanged(/* TODO: finish event implementation */)
    }

    /**
     * Bind a service listener. Only one listener would be valid.
     */
    fun bindMessageServiceListener(listener: MessageServiceListener) {
        this.listener = listener
    }

    // local message functions
    /**
     * Get all chats managed by the service.
     */
    fun getAllChats(): List<Chat> {
        // TODO finish this
//        val cursor = db.rawQuery("select * from chats", null)
//        val columnId = cursor.getColumnIndex("id")
//        val columnType = cursor.getColumnIndex("type")
//        val columnName = cursor.getColumnIndex("name")
//        val list = ArrayList<Chat>()
//        while (cursor.moveToNext()) {
//            list.add(Chat(
//                    id = cursor.getLong(columnId),
//                    type = cursor.getInt(columnType),
//                    name = cursor.getString(columnName)))
//        }
//        cursor.close()
//        return list
        return ArrayList()
    }

    /**
     * Apply updates.
     */
    private fun insertMessages(update: ChatUpdate): List<Chat> {
        // TODO finish this
        return ArrayList()
    }

}