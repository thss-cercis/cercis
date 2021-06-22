package cn.cercis.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.databinding.FragmentActivityNewBinding
import cn.cercis.viewmodel.ActivityNewViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ActivityNewFragment : Fragment() {
    private val viewModel: ActivityNewViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentActivityNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.fragmentActivityNewToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.activityNewSubmit.setOnClickListener {
            viewModel.submit()
            findNavController().popBackStack()
        }

        return binding.root
    }
}
