package cn.edu.tsinghua.thss.cercis.ui.messages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.databinding.MessageSessionListItemBinding

class MessageSessionAdapter : Adapter<ChatSessionViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatSessionViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<MessageSessionListItemBinding>(inflater, R.layout.message_session_list_item, parent, false)
        return ChatSessionViewHolder(binding = binding)
    }

    override fun getItemCount(): Int {
        return sessions.size
    }

    override fun onBindViewHolder(holder: ChatSessionViewHolder, position: Int) {
        holder.binding.viewModel = sessions[position]
        holder.binding.executePendingBindings()
    }

    private val sessions = ArrayList<MessageSessionViewModel>()
}

class ChatSessionViewHolder(
        val binding: MessageSessionListItemBinding
) : ViewHolder(binding.root)
