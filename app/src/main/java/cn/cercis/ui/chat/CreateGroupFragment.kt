package cn.cercis.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.R
import cn.cercis.databinding.FragmentCreateGroupBinding
import cn.cercis.databinding.SelectFriendListItemBinding
import cn.cercis.util.helper.DiffRecyclerViewAdapter
import cn.cercis.util.helper.requireMainActivity
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.snackbarMakeError
import cn.cercis.viewmodel.CreateGroupViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.*

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class CreateGroupFragment : Fragment() {
    val createGroupViewModel: CreateGroupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
        binding.viewModel = createGroupViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.createGroupFriendList.adapter = DiffRecyclerViewAdapter.getInstance(
            dataSource = createGroupViewModel.friendList,
            viewLifecycleOwnerSupplier = { viewLifecycleOwner },
            itemIndex = { first.friendUserId },
            contentsSameCallback = Objects::equals,
            inflater = { itemInflater, parent, _ ->
                SelectFriendListItemBinding.inflate(itemInflater, parent, false)
            },
            onBindViewHolderWithExecution = { holder, position ->
                val (friend, friendSelected) = currentList[position]
                holder.binding.apply {
                    data = friend.toCommonListItemData()
                    selected = friendSelected
                    root.setOnClickListener {
                        createGroupViewModel.toggleUserSelected(friend.friendUserId)
                    }
                }
                holder.binding.root.isSelected = friendSelected
            },
            itemViewType = { 0 },
        )
        binding.createGroupToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.createGroupButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle(R.string.create_group_title)
                setView(R.layout.dialog_create_group)
                setPositiveButton(R.string.create_group_dialog_ok) { dialog, _ ->
                    dialog as AlertDialog
                    val chatName =
                        dialog.findViewById<TextInputEditText>(R.id.dialog_create_group_name_edit_text)!!.text?.toString()
                    if (!chatName.isNullOrEmpty()) {
                        createGroupViewModel.createGroup(chatName)
                    } else {
                        // prevent from submitting empty request
                        dialog.dismiss()
                    }
                }
                setNegativeButton(R.string.create_group_dialog_cancel) { _, _ -> }
            }.show().apply {
                getButton(AlertDialog.BUTTON_POSITIVE).let {
                    it.isEnabled = false
                    findViewById<TextInputEditText>(R.id.dialog_create_group_name_edit_text)!!
                        .addTextChangedListener { text ->
                            when (text?.length) {
                                null, 0 -> it.isEnabled = false
                                else -> it.isEnabled = true
                            }
                        }
                }
            }
        }
        createGroupViewModel.createGroupChatResponse.observe(viewLifecycleOwner) {
            it?.let {
                when (it) {
                    is NetworkResponse.NetworkError, is NetworkResponse.Reject ->
                        snackbarMakeError(
                            binding.root,
                            getString(R.string.create_chat_failed_message).format(it.message ?: ""),
                            Snackbar.LENGTH_SHORT
                        )
                    is NetworkResponse.Success -> {
                        requireMainActivity().openChat(it.data)
                    }
                }
                createGroupViewModel.createGroupChatResponse.value = null
            }
        }
        return binding.root
    }
}
