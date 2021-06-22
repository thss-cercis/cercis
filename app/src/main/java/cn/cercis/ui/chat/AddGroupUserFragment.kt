package cn.cercis.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.R
import cn.cercis.databinding.CompactUserListItemCheckableBinding
import cn.cercis.databinding.FragmentSelectUserBinding
import cn.cercis.util.helper.DiffRecyclerViewAdapter
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.snackbarMakeError
import cn.cercis.viewmodel.AddGroupUserViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.*

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class AddGroupUserFragment : Fragment() {
    private lateinit var binding: FragmentSelectUserBinding
    private val viewModel: AddGroupUserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.selectUserFriendList.adapter = DiffRecyclerViewAdapter.getInstance(
            dataSource = viewModel.friendList,
            viewLifecycleOwnerSupplier = { viewLifecycleOwner },
            itemIndex = { first.friendUserId },
            contentsSameCallback = Objects::equals,
            inflater = { itemInflater, parent, _ ->
                CompactUserListItemCheckableBinding.inflate(itemInflater, parent, false)
            },
            onBindViewHolderWithExecution = { holder, position ->
                val (friend, friendSelected, isMember) = currentList[position]
                holder.binding.apply {
                    data = friend.toCommonListItemData()
                    if (isMember) {
                        selected = true
                        checkable = false
                    } else {
                        selected = friendSelected
                        checkable = true
                    }
                    root.setOnClickListener {
                        viewModel.toggleUserSelected(friend.friendUserId)
                    }
                }
                holder.binding.root.isSelected = friendSelected
            },
            itemViewType = { 0 },
        )
        viewModel.buttonClickable.observe(viewLifecycleOwner) {
            binding.selectUserButton.apply {
                (it ?: true).let {
                    isFocusable = it
                    isClickable = it
                    isEnabled = it
                }
            }
        }
        viewModel.selectedUserCount.observe(viewLifecycleOwner) {
            binding.selectUserButton.text =
                getString(R.string.invite_group_user_button_caption).format(it ?: 0)
        }
        binding.selectUserButton.setOnClickListener {
            viewModel.addToGroup()
        }
        binding.selectUserToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        viewModel.selectUserResponse.observe(viewLifecycleOwner) {
            it?.let {
                when (it) {
                    is NetworkResponse.NetworkError, is NetworkResponse.Reject ->
                        snackbarMakeError(
                            binding.root,
                            getString(R.string.invite_group_user_failed_message),
                            Snackbar.LENGTH_SHORT
                        )
                    is NetworkResponse.Success -> {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }
}