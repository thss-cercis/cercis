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
import cn.cercis.viewmodel.RemoveGroupMemberViewModel
import cn.cercis.viewmodel.toCommonListItemData
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.*

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class RemoveGroupMemberFragment : Fragment(){
    private lateinit var binding: FragmentSelectUserBinding
    private val viewModel: RemoveGroupMemberViewModel by viewModels()

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
            dataSource = viewModel.groupMemberListSource,
            viewLifecycleOwnerSupplier = { viewLifecycleOwner },
            itemIndex = { first.first.userId },
            contentsSameCallback = Objects::equals,
            inflater = { itemInflater, parent, _ ->
                CompactUserListItemCheckableBinding.inflate(itemInflater, parent, false)
            },
            onBindViewHolderWithExecution = { holder, position ->
                val (member, friendSelected, canRemove) = currentList[position]
                holder.binding.apply {
                    data = member.toCommonListItemData()
                    if (canRemove) {
                        selected = friendSelected
                        checkable = true
                    } else {
                        selected = false
                        checkable = false
                    }
                    root.setOnClickListener {
                        viewModel.toggleUserSelected(member.first.userId)
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
                getString(R.string.remove_group_member_button_caption).format(it ?: 0)
        }
        binding.selectUserButton.setOnClickListener {
            viewModel.removeFromGroup()
        }
        binding.selectUserToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        viewModel.removeMemberResponse.observe(viewLifecycleOwner) {
            it?.let {
                when (it) {
                    is NetworkResponse.NetworkError, is NetworkResponse.Reject ->
                        snackbarMakeError(
                            binding.root,
                            getString(R.string.remove_group_member_failed_message),
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