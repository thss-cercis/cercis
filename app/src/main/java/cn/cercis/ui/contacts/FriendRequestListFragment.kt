package cn.cercis.ui.contacts;

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.R
import cn.cercis.databinding.FragmentFriendRequestListBinding
import cn.cercis.databinding.FriendRequestListDelimiterBinding
import cn.cercis.databinding.FriendRequestListItemBinding
import cn.cercis.util.DataBindingViewHolder
import cn.cercis.util.DiffRecyclerViewAdapter
import cn.cercis.util.NetworkResponse
import cn.cercis.viewmodel.FriendRequestListViewModel
import cn.cercis.viewmodel.FriendRequestListViewModel.RecyclerData.Companion.DELIMITER_0
import cn.cercis.viewmodel.FriendRequestListViewModel.RecyclerData.Companion.DELIMITER_1
import cn.cercis.viewmodel.FriendRequestListViewModel.RecyclerData.Companion.TYPE_DATA
import cn.cercis.viewmodel.FriendRequestListViewModel.RecyclerData.Companion.TYPE_DELIMITER
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint;
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class FriendRequestListFragment : Fragment() {
    private val friendRequestListViewModel: FriendRequestListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentFriendRequestListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = friendRequestListViewModel
        binding.fragmentFriendRequestToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.friendRequestListRecyclerView.adapter = object :
            DiffRecyclerViewAdapter<FriendRequestListViewModel.RecyclerData,
                    DataBindingViewHolder<ViewDataBinding>>(
                itemSameCallback = { a, b -> a.id == b.id },
            ) {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int,
            ): DataBindingViewHolder<ViewDataBinding> {
                val inflater1 = LayoutInflater.from(parent.context)
                // * always remember to bind a lifecycleOwner to data binding objects
                val binding1 = when (viewType) {
                    TYPE_DATA -> FriendRequestListItemBinding
                        .inflate(inflater1, parent, false)
                    TYPE_DELIMITER -> FriendRequestListDelimiterBinding
                        .inflate(inflater1, parent, false)
                    else -> throw IllegalStateException("should not reach here")
                }.apply { lifecycleOwner = this@FriendRequestListFragment.viewLifecycleOwner }
                return DataBindingViewHolder(binding1)
            }

            override fun onBindViewHolder(
                holder: DataBindingViewHolder<ViewDataBinding>,
                position: Int,
            ) {
                when (getItemViewType(position)) {
                    TYPE_DATA -> {
                        val requestListItemBinding = holder.binding as FriendRequestListItemBinding
                        (currentList[position] as FriendRequestListViewModel.RecyclerData.FriendRequestWithUpdateMark).let {
                            requestListItemBinding.apply {
                                request = it
                                user = friendRequestListViewModel.getUserInfo(it.fromId)
                                onAcceptClicked = View.OnClickListener { _ ->
                                    if (it.loading.value != true) {
                                        friendRequestListViewModel.acceptRequest(it)
                                    }
                                }
                                executePendingBindings()
                            }
                        }
                    }
                    TYPE_DELIMITER -> {
                        val requestListDelimiterBinding =
                            holder.binding as FriendRequestListDelimiterBinding
                        (currentList[position] as FriendRequestListViewModel.RecyclerData.Delimiter).let {
                            requestListDelimiterBinding.text = getString(when (it.delimiterId) {
                                DELIMITER_0 -> R.string.friend_request_pending
                                DELIMITER_1 -> R.string.friend_request_finished
                                else -> throw IllegalStateException("should not reach here")
                            })
                            requestListDelimiterBinding.executePendingBindings()
                        }
                    }
                }
            }

            override fun getItemViewType(position: Int): Int {
                return currentList[position].type
            }
        }.apply {
            submitList(friendRequestListViewModel.requests.value ?: listOf())
            friendRequestListViewModel.requests.observe(viewLifecycleOwner) { list ->
                list?.let {
                    submitList(it)
                }
            }
        }

        friendRequestListViewModel.operationMessage.observe(viewLifecycleOwner) {
            it?.let {
                val (req, resp) = it
                when (resp) {
                    is NetworkResponse.NetworkError -> {
                        Snackbar.make(binding.root, resp.message, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.friend_request_accept_retry) {
                                friendRequestListViewModel.acceptRequest(req)
                            }
                            .setBackgroundTint(requireContext().getColor(R.color.snackbar_error_background))
                            .setTextColor(requireContext().getColor(R.color.snackbar_error_text))
                            .show()
                    }
                    is NetworkResponse.Reject -> {
                        Snackbar.make(binding.root, resp.message, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.friend_request_accept_retry) {
                                friendRequestListViewModel.acceptRequest(req)
                            }
                            .setBackgroundTint(requireContext().getColor(R.color.snackbar_error_background))
                            .setTextColor(requireContext().getColor(R.color.snackbar_error_text))
                            .show()
                    }
                    is NetworkResponse.Success -> {
                        Snackbar.make(binding.root, R.string.friend_request_accept_success, Snackbar.LENGTH_SHORT)
                            .setBackgroundTint(requireContext().getColor(R.color.snackbar_success_background))
                            .setTextColor(requireContext().getColor(R.color.snackbar_success_text))
                            .show()
                        friendRequestListViewModel.refreshRequestList()
                    }
                }
                friendRequestListViewModel.operationMessage.value = null
            }
        }

        binding.friendRequestListSwipe.setOnRefreshListener {
            friendRequestListViewModel.refreshRequestList()
            val observer = object : androidx.lifecycle.Observer<Boolean> {
                override fun onChanged(t: Boolean?) {
                    if (t == false) {
                        binding.friendRequestListSwipe.isRefreshing = false
                    }
                    friendRequestListViewModel.requestListLoading.removeObserver(this)
                }
            }
            friendRequestListViewModel.requestListLoading.observe(viewLifecycleOwner, observer)
        }
        return binding.root
    }
}
