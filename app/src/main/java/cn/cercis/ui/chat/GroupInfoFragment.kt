package cn.cercis.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.FragmentGroupInfoBinding
import cn.cercis.databinding.GroupInfoMemberListItemBinding
import cn.cercis.entity.GroupChatPermission.GROUP_ADMIN
import cn.cercis.entity.GroupChatPermission.GROUP_OWNER
import cn.cercis.util.helper.DiffRecyclerViewAdapter
import cn.cercis.util.livedata.waitUtilOnce
import cn.cercis.viewmodel.GroupInfoViewModel
import cn.cercis.viewmodel.toCommonListItemData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.*

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class GroupInfoFragment : Fragment() {
    private val groupInfoViewModel: GroupInfoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentGroupInfoBinding.inflate(inflater, container, false)
        binding.viewModel = groupInfoViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.executePendingBindings()
        binding.groupInfoMemberListView.adapter = DiffRecyclerViewAdapter.getInstance(
            dataSource = groupInfoViewModel.groupMemberList,
            viewLifecycleOwnerSupplier = { viewLifecycleOwner },
            itemIndex = { id },
            contentsSameCallback = Objects::equals,
            inflater = { inflater1, parent, _ ->
                GroupInfoMemberListItemBinding.inflate(inflater1, parent, false)
            },
            onBindViewHolderWithExecution = { holder, position ->
                val value = currentList[position]
                holder.binding.data = value.toCommonListItemData()
                holder.binding.selected = false
                holder.binding.permission = permissionToString(value.first.permission)
            },
            itemViewType = { 0 }
        )
        binding.groupInfoMemberListView.apply {
            itemAnimator = null
        }
        binding.groupInfoToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        postponeEnterTransition()
        val start0 = System.currentTimeMillis()
        groupInfoViewModel.groupMemberList.waitUtilOnce(viewLifecycleOwner,
            until = { !it.isNullOrEmpty() },
            observer = {
                (view?.parent as? ViewGroup)?.doOnPreDraw {
                    // Parent has been drawn. Start transitioning!
                    // https://medium.com/androiddevelopers/fragment-transitions-ea2726c3f36f
                    Log.d(LOG_TAG, "waited: ${System.currentTimeMillis() - start0}")
                    startPostponedEnterTransition()
                }
            })
        return binding.root
    }

    private fun permissionToString(groupChatPermission: Int): String {
        return when (groupChatPermission) {
            GROUP_ADMIN.value -> getString(R.string.group_member_admin)
            GROUP_OWNER.value -> getString(R.string.group_member_owner)
            else -> getString(R.string.group_member_member)
        }
    }
}
