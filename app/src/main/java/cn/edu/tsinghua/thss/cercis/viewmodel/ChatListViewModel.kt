package cn.edu.tsinghua.thss.cercis.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
        application: Application,
) : AndroidViewModel(application) {
    val sessions = MutableLiveData(ArrayList<ChatListItemData>())

    fun onRefreshListener() {
        // TODO replace fake data with real ones
        val sessionList = sessions.value ?: ArrayList()
        val newList = ArrayList(LongArray(20) { sessionList.size + it + 0L }.map {
            ChatListItemData(
                    sessionId = it,
                    avatar = "",
                    sessionName = "test session #$it",
                    latestMessage = "test message #${it * 20}",
                    lastUpdate = "18:54",
                    unreadCount = 20,
            )
        })
        Log.d(null, "Some junk data generated!")
        newList.reverse()
        sessions.postValue(newList)
    }
}
