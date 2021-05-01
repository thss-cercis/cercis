package cn.edu.tsinghua.thss.cercis.ui.chatList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.databinding.ChatListItemBinding
import cn.edu.tsinghua.thss.cercis.databinding.FragmentChatListBinding
import cn.edu.tsinghua.thss.cercis.ui.chat.ChatFragment
import cn.edu.tsinghua.thss.cercis.util.ChatId
import cn.edu.tsinghua.thss.cercis.util.doDetailNavigation
import cn.edu.tsinghua.thss.cercis.viewmodel.ChatListItemData
import cn.edu.tsinghua.thss.cercis.viewmodel.ChatListViewModel
import com.bumptech.glide.Glide

class ChatListFragment : Fragment() {
    private val chatListViewModel: ChatListViewModel by activityViewModels()
    inner class ChatListAdapter : Adapter<ChatListAdapter.ChatChatViewHolder>() {
        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatChatViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ChatListItemBinding.inflate(inflater, parent, false)
            return ChatChatViewHolder(binding = binding)
        }

        override fun getItemCount(): Int {
            return sessions.size
        }

        override fun onBindViewHolder(holder: ChatChatViewHolder, position: Int) {
            sessions[position].let {
                holder.binding.run {
                    viewModel = it
                    Glide.with(avatar.context)
                        .load(it.avatar)
                        .fallback(R.drawable.outline_perm_identity_24)
                        .placeholder(R.drawable.outline_perm_identity_24)
                        .into(avatar)
                    executePendingBindings()
                }
            }
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
        fun replaceDataWithoutNotify(list: List<ChatListItemData>) {
            sessions.clear()
            sessions.addAll(list)
        }

        private val sessions = ArrayList<ChatListItemData>()

        inner class ChatChatViewHolder(
            val binding: ChatListItemBinding,
        ) : RecyclerView.ViewHolder(binding.root) {
            init {
                itemView.setOnClickListener {
                    doDetailNavigation(ChatFragment.navDirection(getChatId(absoluteAdapterPosition)))
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentChatListBinding.inflate(inflater, container, false)
        binding.viewModel = chatListViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        // bind refresh listener
        binding.scrollRefreshLayout.setOnRefreshListener {
            chatListViewModel.onRefreshListener()
            binding.scrollRefreshLayout.isRefreshing = false
        }
        // bind item click listener
        // todo: use real data and somehow refactor this
        val adapter = ChatListAdapter()
        binding.sessionListView.adapter = adapter
        chatListViewModel.sessions.observe(viewLifecycleOwner) {
            it?.let {
                adapter.replaceDataWithoutNotify(it)
                adapter.notifyDataSetChanged()
            }
        }
        return binding.root
    }
}
