package cn.cercis.ui.activity

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import cn.cercis.databinding.ActivityListItemBinding
import cn.cercis.databinding.FragmentActivityBinding
import cn.cercis.util.helper.DiffRecyclerViewAdapter
import cn.cercis.util.setDimensionRatio
import cn.cercis.viewmodel.ActivityListItem.Companion.VIEW_TYPE_VIDEO
import cn.cercis.viewmodel.ActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.util.*

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ActivityFragment : Fragment() {
    private val activityViewModel: ActivityViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentActivityBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = activityViewModel
        binding.fragmentActivityToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.activityRecyclerView.adapter = DiffRecyclerViewAdapter.getInstance(
            activityViewModel.activities,
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
                                activityItemVideo.apply {
                                    setVideoURI(Uri.parse(it.videoUrl))
//                                    setOnPreparedListener { mp: MediaPlayer ->
//                                        activityItemRootLayout.setDimensionRatio(
//                                            activityItemVideo.id,
//                                            "${mp.videoWidth}:${mp.videoHeight}"
//                                        )
//                                    }
                                    setOnClickListener {
                                        when {
                                            isPlaying -> pause()
                                            else -> start()
                                        }
                                    }
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

        binding.activitySwipe.setOnRefreshListener {
            activityViewModel.refresh()
            activityViewModel.isLoading.let {
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
