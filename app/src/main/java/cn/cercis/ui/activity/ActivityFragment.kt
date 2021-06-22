package cn.cercis.ui.activity

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.ActivityListItemBinding
import cn.cercis.databinding.FragmentActivityBinding
import cn.cercis.util.getSharedTempFile
import cn.cercis.util.getTempFile
import cn.cercis.util.helper.DiffRecyclerViewAdapter
import cn.cercis.util.helper.doDetailNavigation
import cn.cercis.util.setDimensionRatio
import cn.cercis.viewmodel.ActivityListItem.Companion.VIEW_TYPE_VIDEO
import cn.cercis.viewmodel.ActivityViewModel
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.io.File
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

        binding.activityRecyclerView.adapter = DiffRecyclerViewAdapter.getInstance(
            viewModel.activities,
            { viewLifecycleOwner },
            itemIndex = { activityId },
            contentsSameCallback = Objects::equals,
            inflater = { subInflater, parent, _ ->
                ActivityListItemBinding.inflate(subInflater, parent, false)
            },
            onBindViewHolderWithExecution = { holder, position ->
                holder.binding.apply {
                    activity = currentList[position].also {
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
                    }
                }
            },
            itemViewType = { viewType },
        )
        binding.activityRecyclerView.apply {
            var pinToTop = true
            val linearLayoutManager = layoutManager as LinearLayoutManager
            setOnScrollChangeListener { _, _, _, _, _ ->
                val latestVisible = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                pinToTop = (latestVisible == 0)
            }
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                val latestVisible = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                pinToTop = (latestVisible == 0)
                if (pinToTop) {
                    post {
                        smoothScrollToPosition(0)
                    }
                }
            }
        }

        binding.activitySwipe.setOnRefreshListener {
            viewModel.refresh()
            viewModel.isLoading.let {
                val observer = object : Observer<Boolean> {
                    override fun onChanged(value: Boolean?) {
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
