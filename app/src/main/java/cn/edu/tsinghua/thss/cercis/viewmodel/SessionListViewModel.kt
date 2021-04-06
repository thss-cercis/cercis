package cn.edu.tsinghua.thss.cercis.viewmodel

import android.app.Application
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import cn.edu.tsinghua.thss.cercis.databinding.MessageSessionListItemBinding
import cn.edu.tsinghua.thss.cercis.util.ChatId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionListViewModel @Inject constructor(
        application: Application,
) : AndroidViewModel(application) {
    private val sessionList = ArrayList<SessionListItemData>()
    val sessionListAdapter = SessionListAdapter()
    val openedSession = MutableLiveData<ChatId>(null)

    fun onRefreshListener() {
        // TODO replace fake data with real ones
        val newList = ArrayList(LongArray(20) { sessionList.size + it + 0L }.map {
            SessionListItemData(
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
        sessionList.addAll(0, newList)
        sessionListAdapter.replaceDataWithoutNotify(sessionList);
        sessionListAdapter.notifyDataSetChanged();
    }

    private fun triggerOpenSession(sessionId: ChatId) {
        openedSession.postValue(sessionId)
    }

    inner class SessionListAdapter : Adapter<SessionListAdapter.ChatSessionViewHolder>() {
        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatSessionViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = MessageSessionListItemBinding.inflate(inflater, parent, false)
            return ChatSessionViewHolder(binding = binding)
        }

        override fun getItemCount(): Int {
            return sessions.size
        }

        override fun onBindViewHolder(holder: ChatSessionViewHolder, position: Int) {
            holder.binding.viewModel = sessions[position]
            holder.binding.executePendingBindings()
        }

        override fun getItemId(position: Int): Long {
            return sessions[position].sessionId
        }

        fun getChatId(position: Int): ChatId {
            return sessions[position].sessionId
        }

        /**
         * Replaces all data within the message session adapter, but does not notify the changes.
         *
         * To update the view, call [Adapter.notifyItemChanged] and other similar functions to inform
         * the data changes.
         */
        fun replaceDataWithoutNotify(list: List<SessionListItemData>) {
            sessions.clear()
            sessions.addAll(list)
        }

        private val sessions = ArrayList<SessionListItemData>()

        inner class ChatSessionViewHolder(
                val binding: MessageSessionListItemBinding,
        ) : RecyclerView.ViewHolder(binding.root) {
            init {
                itemView.setOnClickListener {
                    triggerOpenSession(getChatId(adapterPosition))
                }
            }
        }
    }

}