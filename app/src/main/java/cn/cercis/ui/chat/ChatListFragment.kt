package cn.cercis.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import cn.cercis.R
import cn.cercis.common.ChatId
import cn.cercis.databinding.ChatListItemBinding
import cn.cercis.databinding.FragmentChatListBinding
import cn.cercis.util.helper.doDetailNavigation
import cn.cercis.viewmodel.ChatListItemData
import cn.cercis.viewmodel.ChatListViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
class ChatListFragment : Fragment() {
    private val chatListViewModel: ChatListViewModel by activityViewModels()
    inner class ChatListAdapter : Adapter<ChatListAdapter.ChatViewHolder>() {
        init {
            setHasStableIds(true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ChatListItemBinding.inflate(inflater, parent, false)
            return ChatViewHolder(binding = binding)
        }

        override fun getItemCount(): Int {
            return chats.size
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            holder.binding.run {
                chats[position].let {
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
            return chats[position].chatId
        }

        fun getChatId(position: Int): ChatId {
            return chats[position].chatId
        }

        /**
         * Replaces all data within the message chat adapter, but does not notify the changes.
         *
         * To update the view, call [Adapter.notifyItemChanged] and other similar functions to inform
         * the data changes.
         */
        fun replaceDataWithoutNotify(list: List<ChatListItemData>) {
            chats.clear()
            chats.addAll(list)
        }

        private val chats = ArrayList<ChatListItemData>()

        inner class ChatViewHolder(
            val binding: ChatListItemBinding,
        ) : RecyclerView.ViewHolder(binding.root) {
            init {
                itemView.setOnClickListener {
                    doDetailNavigation(ChatFragment.navDirection(getChatId(absoluteAdapterPosition)))
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val menuItem = menu.findItem(R.id.action_search)
        // TODO inline search
//        menuItem?.let {
//            val actionView = it.actionView as SearchView
//        }
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
        binding.topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_search -> doDetailNavigation(R.id.action_global_searchFragment)
            }
            true
        }
        // bind item click listener
        // TODO: use real data and somehow refactor this
        val adapter = ChatListAdapter()
        binding.chatListView.adapter = adapter
        chatListViewModel.chatListItems.observe(viewLifecycleOwner) {
            it?.let {
                adapter.replaceDataWithoutNotify(it)
                adapter.notifyDataSetChanged()
            }
        }
        return binding.root
    }
}
