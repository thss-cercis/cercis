package cn.cercis.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import cn.cercis.R
import cn.cercis.databinding.ChatListItemBinding
import cn.cercis.databinding.CommonListItemBinding
import cn.cercis.databinding.FragmentChatListBinding
import cn.cercis.entity.ChatType
import cn.cercis.util.helper.DiffRecyclerViewAdapter
import cn.cercis.util.helper.doDetailNavigation
import cn.cercis.util.helper.requireMainActivity
import cn.cercis.viewmodel.ChatListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
class ChatListFragment : Fragment() {
    private val chatListViewModel: ChatListViewModel by activityViewModels()

    override fun onPrepareOptionsMenu(menu: Menu) {
        val menuItem = menu.findItem(R.id.action_search)
        // TODO inline search
//        menuItem?.let {
//            val actionView = it.actionView as SearchView
//        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
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
                R.id.action_create_group -> doDetailNavigation(R.id.action_global_createGroupFragment)
            }
            true
        }
        // bind item click listener
        val adapter = DiffRecyclerViewAdapter.getInstance(
            dataSource = chatListViewModel.chatListData,
            viewLifecycleOwnerSupplier = { viewLifecycleOwner },
            itemIndex = { chatId },
            contentsSameCallback = { a, b -> a.chatId == b.chatId && a.chatType == ChatType.CHAT_GROUP },
            inflater = { inflater1, parent, _ ->
                ChatListItemBinding.inflate(
                    inflater1,
                    parent,
                    false
                )
            },
            onBindViewHolderWithExecution = { holder, position ->
                val chat = currentList[position]
                holder.binding.data = chatListViewModel.getChatDisplay(chat)
                holder.binding.root.setOnClickListener { requireMainActivity().openChat(chat.toChat()) }
            },
            itemViewType = { chatType }
        )
        binding.chatListView.adapter = adapter
        // remove update animation
        (binding.chatListView.itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        return binding.root
    }
}
