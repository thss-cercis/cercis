package cn.cercis.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import cn.cercis.R
import cn.cercis.common.ChatId
import cn.cercis.databinding.ChatListItemBinding
import cn.cercis.databinding.CommonListItemBinding
import cn.cercis.databinding.FragmentChatListBinding
import cn.cercis.entity.ChatType
import cn.cercis.util.helper.DiffRecyclerViewAdapter
import cn.cercis.util.helper.doDetailNavigation
import cn.cercis.util.helper.requireMainActivity
import cn.cercis.viewmodel.ChatListItemData
import cn.cercis.viewmodel.ChatListViewModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

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
        savedInstanceState: Bundle?
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
            }
            true
        }
        // bind item click listener
        // TODO: use real data and somehow refactor this
        val adapter = DiffRecyclerViewAdapter.getInstance(
            dataSource = chatListViewModel.chatListData,
            viewLifecycleOwnerSupplier = { viewLifecycleOwner },
            itemIndex = { id },
            contentsSameCallback = { a, b -> a.id == b.id && a.type == ChatType.CHAT_GROUP },
            inflater = { inflater1, parent, _ ->
                CommonListItemBinding.inflate(
                    inflater1,
                    parent,
                    false
                )
            },
            onBindViewHolderWithExecution = { holder, position ->
                holder.binding.data = chatListViewModel.getChatDisplay(currentList[position])
                holder.binding.root.setOnClickListener { requireMainActivity().openChat(currentList[position]) }
            },
            itemViewType = { type }
        )
        binding.chatListView.adapter = adapter
        return binding.root
    }
}
