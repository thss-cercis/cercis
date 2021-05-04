package cn.cercis.ui.contacts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.ContactListFriendItemBinding
import cn.cercis.databinding.ContactListRecyclerViewBinding
import cn.cercis.databinding.FragmentContactListBinding
import cn.cercis.util.DataBindingViewHolder
import cn.cercis.util.DiffRecyclerViewAdapter
import cn.cercis.util.doDetailNavigation
import cn.cercis.viewmodel.ContactListViewModel
import cn.cercis.viewmodel.ContactListViewModel.FriendEntryWithUpdateMark
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
                            holder.binding.user = currentList[position].let {
                                contactListViewModel.getUserInfo(it.friendUserId, it)
                            }
                        }
                    )
                }

                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int,
                ): DataBindingViewHolder<ContactListRecyclerViewBinding> {
                    val recyclerViewBinding = ContactListRecyclerViewBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                    recyclerViewBinding.contactListSwipe.setOnRefreshListener {
                        contactListViewModel.refreshFriendList()
                        val observer = object : androidx.lifecycle.Observer<Boolean> {
                            override fun onChanged(t: Boolean?) {
                                if (t == false) {
                                    recyclerViewBinding.contactListSwipe.isRefreshing = false
                                }
                                contactListViewModel.friendListLoading.removeObserver(this)
                            }
                        }
                        contactListViewModel.friendListLoading.observe(viewLifecycleOwner, observer)
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
                            // TODO supports for groups
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
        binding.buttonShowGroupNotifications.root.setOnClickListener { /* TODO */ }
        return binding.root
    }

    companion object {
        const val TAB_COUNT = 2
        const val TAB_FRIENDS = 0
        const val TAB_GROUPS = 1
    }
}
