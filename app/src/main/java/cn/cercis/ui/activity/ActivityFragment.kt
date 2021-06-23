package cn.cercis.ui.activity

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.ActivityCommentItemBinding
import cn.cercis.common.mapRun
import cn.cercis.databinding.ActivityListItemBinding
import cn.cercis.databinding.FragmentActivityBinding
import cn.cercis.util.getTempFile
import cn.cercis.util.helper.DiffRecyclerViewAdapter
import cn.cercis.util.helper.doDetailNavigation
import cn.cercis.util.helper.showImageDialog
import cn.cercis.util.livedata.generateMediatorLiveData
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.setDimensionRatio
import cn.cercis.util.snackbarMakeError
import cn.cercis.viewmodel.ActivityListItem.Companion.VIEW_TYPE_VIDEO
import cn.cercis.viewmodel.ActivityViewModel
import cn.cercis.viewmodel.CommonListItemData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ActivityFragment : Fragment() {
    private val viewModel: ActivityViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentActivityBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.fragmentActivityToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val fromGalleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            val cr: ContentResolver = requireContext().contentResolver
            if (uri != null && cr.getType(uri) != null) {
                try {
                    cr.openInputStream(uri)?.let { input ->
                        val file = getTempFile(".tmp")
                        FileOutputStream(file).use {
                            input.copyTo(it)
                        }
                        viewModel.publishVideoActivity(file)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        binding.fragmentActivityToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_new_normal_activity -> doDetailNavigation(R.id.activityNewFragment)
                R.id.action_new_video_activity -> {
                    fromGalleryLauncher.launch("video/*")
                }
            }
            true
        }

        binding.activityRecyclerView.itemAnimator = null
        binding.activityRecyclerView.adapter = DiffRecyclerViewAdapter.getInstance(
            viewModel.activities,
            { viewLifecycleOwner },
            itemIndex = { activityId },
            contentsSameCallback = Objects::equals,
            inflater = { subInflater, parent, _ ->
                ActivityListItemBinding.inflate(subInflater, parent, false).apply {
                    activityItemCommentList.itemAnimator = null
                    arrayOf(
                        this.activityItemImage0,
                        this.activityItemImage1,
                        this.activityItemImage2,
                        this.activityItemImage3,
                        this.activityItemImage4,
                        this.activityItemImage5,
                        this.activityItemImage6,
                        this.activityItemImage7,
                        this.activityItemImage8,
                    ).forEachIndexed { idx, view ->
                        view.setOnClickListener {
                            activity?.let {
                                showImageDialog(requireContext(), it.getImageUrl(idx))
                            }
                        }
                    }
                }
            },
            onBindViewHolderWithExecution = { holder, position ->
                holder.binding.apply {
                    activityItemThumbUpUsers.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_thumb_up_filled_16, 0, 0, 0
                    )
                    activity = currentList[position].also {
                        user = viewModel.loadUser(it.userId)
                        val thumbUpLiveDataList = it.thumbUpUserIdList.map(viewModel::loadUser)
                        thumbUpUsersText =
                            generateMediatorLiveData(*thumbUpLiveDataList.toTypedArray()) {
                                thumbUpLiveDataList.mapRun { value?.displayName }.filterNotNull()
                                    .joinToString(cn.cercis.util.getString(R.string.user_separator))
                            }
                        activityItemButtonThumbUp.apply {
                            if (!it.thumbUpUserIdList.contains(viewModel.currentUserId)) {
                                setIconResource(R.drawable.ic_thumb_up_24)
                                setOnClickListener { _ ->
                                    viewModel.thumbUp(it.activityId, true)
//                                    setIconResource(R.drawable.ic_thumb_up_filled_24)
                                }
                            } else {
                                setIconResource(R.drawable.ic_thumb_up_filled_24)
                                setOnClickListener { _ ->
                                    viewModel.thumbUp(it.activityId, false)
//                                    setIconResource(R.drawable.ic_thumb_up_24)
                                }
                            }
                        }
                        when (it.viewType) {
                            VIEW_TYPE_VIDEO -> {
                                buttonVisible = false
                                progressVisible = true
                                activityItemVideoStartButton.setOnClickListener {
                                    activityItemVideo.start()
                                    buttonVisible = false
                                    binding.executePendingBindings()
                                }
                                activityItemVideo.apply {
                                    setVideoURI(Uri.parse(it.videoUrl))
                                    setOnPreparedListener {
                                        progressVisible = false
                                        buttonVisible = true
                                        binding.executePendingBindings()
//                                        activityItemRootLayout.setDimensionRatio(
//                                            activityItemVideo.id,
//                                            "${mp.videoWidth}:${mp.videoHeight}"
//                                        )
                                    }
                                    setOnCompletionListener {
                                        buttonVisible = true
                                        binding.executePendingBindings()
                                    }
//                                    setOnClickListener {
//                                        Log.d(LOG_TAG, "video clicked")
//                                        when {
//                                            isPlaying -> pause()
//                                            else -> start()
//                                        }
//                                    }
                                }
                            }
                            else -> {
                                activityItemRootLayout.setDimensionRatio(
                                    activityItemImageGrid.id,
                                    it.dimensionRatio
                                )
                            }
                        }
                        // comments
                        activityItemCommentList.adapter = DiffRecyclerViewAdapter.getInstance(
                            dataSource = viewModel.getCommentLiveData(it.activityId),
                            viewLifecycleOwnerSupplier = { viewLifecycleOwner },
                            itemIndex = { id },
                            contentsSameCallback = Objects::equals,
                            inflater = { inflater, parent, _ ->
                                ActivityCommentItemBinding.inflate(inflater, parent, false)
                            },
                            onBindViewHolderWithExecution = { holder, position ->
                                val data = currentList[position]
                                holder.binding.apply {
                                    displayName = viewModel.loadUser(data.commenterId)
                                        .map { p: CommonListItemData? ->
                                            p?.let { p.displayName.toString() } ?: ""
                                        }
                                    content = data.content
                                }
                            },
                            itemViewType = { 0 }
                        )
                        activityItemButtonComment.setOnClickListener {
                            val activityId = currentList[position].activityId
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("评论")
                                .setView(R.layout.dialog_send_comment)
                                .setPositiveButton(R.string.dialog_ok) { dialog, _ ->
                                    dialog as AlertDialog
                                    val editText =
                                        dialog.findViewById<TextInputEditText>(R.id.dialog_send_comment_edit_text)!!
                                    if (editText.text.toString().isNotEmpty()) {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            viewModel.sendComment(activityId,
                                                editText.text.toString()).apply {
                                                if (this !is NetworkResponse.Success) {
                                                    snackbarMakeError(
                                                        binding.root,
                                                        getString(R.string.activity_send_comment_failed_message).format(
                                                            this.message ?: ""),
                                                        Snackbar.LENGTH_SHORT
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                .setNegativeButton(R.string.dialog_cancel) { _, _ -> }
                                .show()
                        }
                    }
                }
            },
            itemViewType = { viewType },
        )

        binding.activitySwipe.setOnRefreshListener {
            viewModel.refresh()
            viewModel.isLoading.let {
                val observer = object : Observer<Boolean> {
                    override fun onChanged(value: Boolean?) {
                        Log.d(LOG_TAG, "value: $value")
                        if (value == false) {
                            binding.activitySwipe.isRefreshing = false
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
