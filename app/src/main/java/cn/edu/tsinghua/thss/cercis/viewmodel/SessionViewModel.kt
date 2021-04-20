package cn.edu.tsinghua.thss.cercis.viewmodel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import cn.edu.tsinghua.thss.cercis.Constants
import cn.edu.tsinghua.thss.cercis.databinding.ChatItemBinding
import cn.edu.tsinghua.thss.cercis.entity.Message
import cn.edu.tsinghua.thss.cercis.repository.MessageRepository
import cn.edu.tsinghua.thss.cercis.repository.UserRepository
import cn.edu.tsinghua.thss.cercis.util.ChatId
import cn.edu.tsinghua.thss.cercis.util.MessageId
import dagger.assisted.Assisted
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
        savedStateHandle: SavedStateHandle,
        private val messageRepository: MessageRepository,
        private val userRepository: UserRepository,
) : ViewModel(), LifecycleObserver {
    private val chatId = savedStateHandle.get<Long>("chatId") ?: -1
    private val chatParticipants = messageRepository.getChatParticipants(chatId)
    val chat = messageRepository.getChat(chatId)
    fun side(senderId: ChatId) : MessageViewModel.Side = when {
        senderId != userRepository.currentUserId.value -> {
            MessageViewModel.Side.OTHER
        }
        else -> MessageViewModel.Side.THIS
    }

    /**
     * Messages to be displayed.
     */
    private val messageList: ArrayList<Message> = ArrayList()
    private var messageListDataSource0: LiveData<List<Message>>? = null
    val messageListDataSource: MediatorLiveData<List<Message>> = MediatorLiveData()


    /** used by [loadMorePreviousMessage] */
    private var messageLoading = false

    // range of messages being displayed
    private val currentIdStart: MessageId = 0
    private val currentIdEnd: MessageId = 0


    @MainThread
    private fun loadCurrentMessages(count: Long) {
        preventDoubleSubmitRun {
            messageRepository.getChatRecentMessages(count, 15)
        }
    }

    @MainThread
    private fun reloadMessageList(messages: List<Message>) {
        messageList.clear()
        messageList.addAll(messages)
    }

    @MainThread
    private fun replaceDataSource(messageDataSource: LiveData<List<Message>>) {
        messageListDataSource0?.let { messageListDataSource.removeSource(it) }
        messageListDataSource0 = messageDataSource
        messageListDataSource.addSource(messageDataSource) {
            if (it != null) {
                reloadMessageList(it)
            }
        }
    }

    @MainThread
    private fun preventDoubleSubmitRun(runnable: suspend () -> Unit) {
        if (messageLoading) {
            return;
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                runnable()
            } finally {
                messageLoading = false
            }
        }
    }

    /**
     * Loads more previous messages when for example, swiped to the top.
     */
    @MainThread
    private fun loadMorePreviousMessage(count: Long) {
        preventDoubleSubmitRun {
            val newData = messageRepository.getChatMessagesNewerThanWithPreviousMessages(
                    chatId,
                    currentIdStart,
                    Constants.REFRESH_COUNT,
            )
            messageLoading = false
        }
    }

    /**
     * Exits view
     */
    fun navigationOnClickListener(view: View) {
        TODO("finish this")
    }
}