package cn.cercis.ui.contacts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.ContactListFriendItemBinding
import cn.cercis.databinding.ContactListRecyclerViewBinding
import cn.cercis.databinding.FragmentContactListBinding
import cn.cercis.databinding.GroupChatListItemBinding
import cn.cercis.entity.User
import cn.cercis.util.helper.DataBindingViewHolder
import cn.cercis.util.helper.DiffRecyclerViewAdapter
import cn.cercis.util.helper.doDetailNavigation
import cn.cercis.util.helper.requireMainActivity
import cn.cercis.viewmodel.CommonListItemData
import cn.cercis.viewmodel.ContactListViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.*


@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ContactListFragment : Fragment() {
    private val contactListViewModel: ContactListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentContactListBinding.inflate(inflater, container, false)
        binding.viewModel = contactListViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.contactListPager.adapter =
            object : RecyclerView.Adapter<DataBindingViewHolder<ContactListRecyclerViewBinding>>() {
                val friendAdapter by lazy {
                    DiffRecyclerViewAdapter.getInstance(
                        contactListViewModel.friendList,
                        { viewLifecycleOwner },
                        itemIndex = { friendUserId },
                        contentsSameCallback = Objects::equals,
                        inflater = { inflater, parent, _ ->
                            ContactListFriendItemBinding.inflate(inflater, parent, false)
                        },
                        onBindViewHolderWithExecution = { holder, position ->
                            holder.binding.apply {
                                user = currentList[position].let {
                                    contactListViewModel.getUserInfo(it.friendUserId, it)
                                }.apply {
                                    root.setOnClickListener {
                                        value?.let {
                                            requireMainActivity().openUserInfo(
                                                // TODO replace this stupid workaround
                                                User(
                                                    id = it.id,
                                                    nickname = it.nickname,
                                                    mobile = it.mobile,
                                                    avatar = it.avatar,
                                                    bio = it.bio,
                                                    updated = 0L,
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    )
                }

                val groupAdapter = DiffRecyclerViewAdapter.getInstance(
                    contactListViewModel.groupChatList,
                    { viewLifecycleOwner },
                    itemIndex = { id },
                    contentsSameCallback = Objects::equals,
                    inflater = { inflater, parent, _ ->
                        GroupChatListItemBinding.inflate(inflater, parent, false)
                    },
                    onBindViewHolderWithExecution = { holder, position ->
                        currentList[position].let { chat ->
                            holder.binding.data = CommonListItemData(
                                avatar = chat.avatar,
                                displayName = chat.name,
                                description = "",
                            )
                            holder.binding.root.setOnClickListener {
                                requireMainActivity().openChat(chat)
                            }
                        }
                    },
                )

                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int,
                ): DataBindingViewHolder<ContactListRecyclerViewBinding> {
                    val recyclerViewBinding = ContactListRecyclerViewBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                    when(viewType) {
                        TAB_FRIENDS -> recyclerViewBinding.contactListSwipe.setOnRefreshListener {
                            contactListViewModel.refreshFriendList()
                            val observer = object : Observer<Boolean> {
                                override fun onChanged(t: Boolean?) {
                                    if (t == false) {
                                        recyclerViewBinding.contactListSwipe.isRefreshing = false
                                    }
                                    contactListViewModel.friendListLoading.removeObserver(this)
                                }
                            }
                            contactListViewModel.friendListLoading.observe(viewLifecycleOwner,
                                observer)
                        }
                        TAB_GROUPS -> recyclerViewBinding.contactListSwipe.setOnRefreshListener {
                            contactListViewModel.refreshGroupChatList()
                        }
                    }
                    return DataBindingViewHolder(recyclerViewBinding)
                }

                override fun onBindViewHolder(
                    holder: DataBindingViewHolder<ContactListRecyclerViewBinding>,
                    position: Int,
                ) {
                    when (position) {
                        TAB_FRIENDS -> {
                            holder.binding.contactListRecyclerView.apply {
                                if (adapter != friendAdapter) {
                                    adapter = friendAdapter
                                }
                            }
                        }
                        TAB_GROUPS -> {
                            holder.binding.contactListRecyclerView.apply {
                                if (adapter != groupAdapter) {
                                    adapter = groupAdapter
                                }
                            }
                        }
                    }
                }

                override fun getItemCount(): Int {
                    return TAB_COUNT
                }

                override fun getItemViewType(position: Int): Int {
                    // prevent view reuse
                    return position
                }

            }

        // reduce swipe sensitivity
        // TODO this code uses reflect and depends on undocumented APIs
        ViewPager2::class.java.getDeclaredField("mRecyclerView").apply {
            isAccessible = true
        }.get(binding.contactListPager).apply {
            this as RecyclerView
            RecyclerView::class.java.getDeclaredField("mTouchSlop").apply {
                isAccessible = true
            }.let {
                val touchSlop = it.getInt(this)
                it.set(this, touchSlop * 6)
            }
        }

        // initial TabLayout and ViewPager2
        TabLayoutMediator(binding.tabs, binding.contactListPager) { tab, position ->
            when (position) {
                TAB_FRIENDS -> {
                    Log.d(LOG_TAG, "selected tab friends")
                    tab.text = getString(R.string.contact_list_tab_friends)
                    binding.contactListPager.currentItem = position
                }
                TAB_GROUPS -> {
                    Log.d(LOG_TAG, "selected tab groups")
                    tab.text = getString(R.string.contact_list_tab_groups)
                    binding.contactListPager.currentItem = position
                }
            }
        }.attach()
        binding.contactListPager.currentItem = TAB_FRIENDS

        // initiate ListView with actions
        binding.buttonShowFriendRequests.root.setOnClickListener {
            doDetailNavigation(R.id.action_global_friendRequestListFragment)
        }
        return binding.root
    }

    companion object {
        const val TAB_COUNT = 2
        const val TAB_FRIENDS = 0
        const val TAB_GROUPS = 1
    }
}
