package cn.cercis.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import cn.cercis.R
import cn.cercis.databinding.FragmentFriendRequestListBinding
import cn.cercis.databinding.FriendRequestListDelimiterBinding
import cn.cercis.databinding.FriendRequestListItemBinding
import cn.cercis.util.helper.DiffRecyclerViewAdapter
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.viewmodel.FriendRequestListViewModel
import cn.cercis.viewmodel.FriendRequestListViewModel.RecyclerData.Companion.DELIMITER_0
import cn.cercis.viewmodel.FriendRequestListViewModel.RecyclerData.Companion.DELIMITER_1
import cn.cercis.viewmodel.FriendRequestListViewModel.RecyclerData.Companion.TYPE_DATA
import cn.cercis.viewmodel.FriendRequestListViewModel.RecyclerData.Companion.TYPE_DELIMITER
import cn.cercis.viewmodel.FriendRequestListViewModel.RecyclerData.Delimiter
import cn.cercis.viewmodel.FriendRequestListViewModel.RecyclerData.FriendRequestWithUpdateMark
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.*

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

        binding.friendRequestListRecyclerView.adapter =
            DiffRecyclerViewAdapter.getInstance(
                friendRequestListViewModel.requests,
                { viewLifecycleOwner },
                itemIndex = { id },
                contentsSameCallback = Objects::equals,
                inflater = { subInflater, parent, viewType ->
                    // * always remember to bind a lifecycleOwner to data binding objects
                    when (viewType) {
                        TYPE_DATA -> FriendRequestListItemBinding
                            .inflate(subInflater, parent, false)
                        TYPE_DELIMITER -> FriendRequestListDelimiterBinding
                            .inflate(subInflater, parent, false)
                        else -> throw IllegalStateException("should not reach here")
                    }
                },
                onBindViewHolderWithExecution = { holder, position ->
                    when (getItemViewType(position)) {
                        TYPE_DATA -> {
                            (holder.binding as FriendRequestListItemBinding).apply {
                                (currentList[position] as FriendRequestWithUpdateMark).let {
                                    request = it
                                    user = friendRequestListViewModel.getUserLiveData(it.fromId)
                                    onAcceptClicked = View.OnClickListener { _ ->
                                        if (it.loading.value != true) {
                                            friendRequestListViewModel.acceptRequest(it)
                                        }
                                    }
                                }
                            }
                        }
                        TYPE_DELIMITER -> {
                            (holder.binding as FriendRequestListDelimiterBinding).apply {
                                (currentList[position] as Delimiter).let {
                                    text = getString(when (it.delimiterId) {
                                        DELIMITER_0 -> R.string.friend_request_pending
                                        DELIMITER_1 -> R.string.friend_request_finished
                                        else -> throw IllegalStateException("should not reach here")
                                    })
                                }
                            }
                        }
                    }
                },
                getViewType = { currentList[it].type },
            )

        friendRequestListViewModel.operationMessage.observe(viewLifecycleOwner) {
            it ?: return@observe
            val (req, resp) = it
            when (resp) {
                is NetworkResponse.NetworkError,
                is NetworkResponse.Reject -> {
                    Snackbar.make(binding.root, resp.message!!, Snackbar.LENGTH_SHORT)
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

        binding.friendRequestListSwipe.setOnRefreshListener {
            friendRequestListViewModel.refreshRequestList()
            friendRequestListViewModel.requestListLoading.let {
                val observer = object : Observer<Boolean> {
                    override fun onChanged(value: Boolean?) {
                        if (value == false) {
                            binding.friendRequestListSwipe.isRefreshing = false
                        }
                        it.removeObserver(this)
                    }
                }
                it.observe(viewLifecycleOwner, observer)
            }
        }
        return binding.root
    }
}
