package cn.cercis.ui.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.FragmentGroupInfoBinding
import cn.cercis.databinding.GroupInfoMemberListItemBinding
import cn.cercis.entity.GroupChatPermission
import cn.cercis.entity.GroupChatPermission.GROUP_ADMIN
import cn.cercis.entity.GroupChatPermission.GROUP_OWNER
import cn.cercis.util.getTempFile
import cn.cercis.util.helper.DiffRecyclerViewAdapter
import cn.cercis.util.helper.showImageDialog
import cn.cercis.util.helper.showInputDialog
import cn.cercis.util.livedata.observeFilterFirst
import cn.cercis.util.makeSnackbar
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.snackbarMakeError
import cn.cercis.util.snackbarMakeSuccess
import cn.cercis.viewmodel.GroupInfoViewModel
import cn.cercis.viewmodel.toCommonListItemData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.*

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class GroupInfoFragment : Fragment() {
    private val viewModel: GroupInfoViewModel by viewModels()
    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { it ->
                // The image was saved into the given Uri -> do something with it
                Log.d(LOG_TAG, it.toString())
                UCrop.of(uri, Uri.fromFile(getTempFile(".png")))
                    .withAspectRatio(1f, 1f)
                    .withMaxResultSize(512, 512)
                    .start(requireContext(), this)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentGroupInfoBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.executePendingBindings()
        binding.groupInfoAvatar.apply {
            setOnClickListener {
                viewModel.chat.value?.avatar.takeUnless { it.isNullOrEmpty() }?.let { avatar ->
                    showImageDialog(requireContext(), avatar)
                }
            }
        }
        binding.groupInfoMemberListView.adapter = DiffRecyclerViewAdapter.getInstance(
            dataSource = viewModel.groupMemberList,
            viewLifecycleOwnerSupplier = { viewLifecycleOwner },
            itemIndex = { id },
            contentsSameCallback = Objects::equals,
            inflater = { inflater1, parent, _ ->
                GroupInfoMemberListItemBinding.inflate(inflater1, parent, false).apply {
                    this.root.setOnCreateContextMenuListener { menu, _, _ ->
                        menu.add(getString(R.string.group_info_give_away_owner_dialog_title))
                            .setOnMenuItemClickListener {
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.group_info_give_away_owner_dialog_title)
                                    .setMessage(getString(R.string.group_info_give_away_owner_dialog_message).format(
                                        this.data?.displayName ?: ""))
                                    .setPositiveButton(R.string.dialog_ok) { _, _ ->
                                        val userId = this.userId
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            val makeRequest = suspend {
                                                viewModel.reassignGroupOwner(userId)
                                            }
                                            val result = MutableLiveData(makeRequest())
                                            lifecycleScope.launch(Dispatchers.Main) {
                                                result.observe(viewLifecycleOwner) {
                                                    when (it) {
                                                        is NetworkResponse.NetworkError, is NetworkResponse.Reject -> {
                                                            snackbarMakeError(binding.root,
                                                                it.message!!,
                                                                Snackbar.LENGTH_SHORT) {
                                                                setAction(R.string.snackbar_retry) {
                                                                    launch(Dispatchers.IO) {
                                                                        result.postValue(makeRequest())
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        is NetworkResponse.Success -> {
                                                            snackbarMakeSuccess(binding.root,
                                                                getString(R.string.group_info_give_away_owner_success),
                                                                Snackbar.LENGTH_SHORT
                                                            )
                                                        }
                                                        null -> Unit
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .setNegativeButton(R.string.dialog_cancel) { _, _ -> }
                                    .show()
                                true
                            }
                    }
                }
            },
            onBindViewHolderWithExecution = { holder, position ->
                val value = currentList[position]
                holder.binding.data = value.toCommonListItemData()
                holder.binding.selected = false
                holder.binding.permission = permissionToString(value.first.permission)
                holder.binding.userId = value.first.userId
            },
            itemViewType = { 0 }
        )
        binding.groupInfoMemberListView.apply {
            itemAnimator = null
        }
        binding.groupInfoToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.groupInfoAddMembers.setOnClickListener {
            findNavController().navigate(GroupInfoFragmentDirections.actionGroupInfoFragmentToAddGroupUserFragment(
                viewModel.chatId
            ))
        }
        binding.groupInfoRemoveMembers.setOnClickListener {
            findNavController().navigate(GroupInfoFragmentDirections.actionGroupInfoFragmentToRemoveGroupMemberFragment(
                viewModel.chatId
            ))
        }
        binding.groupInfoEditInfo.apply {
            setOnClickListener {
                it!!.showContextMenu()
            }
            setOnCreateContextMenuListener { menu, _, _ ->
                menu.add(getString(R.string.group_info_change_avatar))
                    .setOnMenuItemClickListener {
                        pickImages.launch("image/*")
                        true
                    }
                menu.add(getString(R.string.group_info_edit_name))
                    .setOnMenuItemClickListener {
                        showInputDialog(
                            requireContext(),
                            getString(R.string.group_info_change_name_dialog_title),
                            viewModel.chat.value?.name ?: ""
                        ) {
                            makeSnackbar(
                                { viewModel.editGroupName(it) },
                                { getString(R.string.group_info_change_name_dialog_title) },
                            )
                        }.show()
                        true
                    }
            }
        }
        postponeEnterTransition()
        val start0 = System.currentTimeMillis()
        viewModel.groupMemberList.observeFilterFirst(viewLifecycleOwner,
            until = { !it.isNullOrEmpty() },
            observer = {
                (view?.parent as? ViewGroup)?.doOnPreDraw {
                    // Parent has been drawn. Start transitioning!
                    // https://medium.com/androiddevelopers/fragment-transitions-ea2726c3f36f
                    Log.d(LOG_TAG, "waited: ${System.currentTimeMillis() - start0}")
                    startPostponedEnterTransition()
                }
            })
        binding.groupInfoExitGroup.setOnClickListener {
            if (!viewModel.canLeaveGroup()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.group_info_exit_group_dialog_title)
                    .setMessage(R.string.group_info_exit_group_dialog_retry_message)
                    .setNegativeButton(R.string.dialog_cancel) { _, _ -> }
                    .show()
            } else {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.group_info_exit_group_dialog_title)
                    .setMessage(R.string.group_info_exit_group_dialog_confirm_message)
                    .setPositiveButton(R.string.dialog_ok) { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            viewModel.leaveGroup().use {
                                viewModel.refreshChatList()
                                launch(Dispatchers.Main) {
                                    findNavController().popBackStack(R.id.emptyFragment, false)
                                }
                            }
                        }
                    }
                    .setNegativeButton(R.string.dialog_cancel) { _, _ -> }
                    .show()
            }
        }
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.let {
                makeSnackbar(
                    { viewModel.changeAvatar(it) },
                    { getString(R.string.group_info_avatar_changed_success) }
                )
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            snackbarMakeError(requireView(), cropError?.message ?: "", Snackbar.LENGTH_SHORT)
        }
    }

    private fun permissionToString(groupChatPermission: Int): String {
        return when (groupChatPermission) {
            GROUP_ADMIN.value -> getString(R.string.group_member_admin)
            GROUP_OWNER.value -> getString(R.string.group_member_owner)
            else -> getString(R.string.group_member_member)
        }
    }
}
