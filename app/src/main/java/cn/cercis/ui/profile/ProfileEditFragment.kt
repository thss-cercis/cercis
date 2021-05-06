package cn.cercis.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.databinding.FragmentProfileEditBinding
import cn.cercis.util.helper.enableTransition
import cn.cercis.viewmodel.ProfileEditViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ProfileEditFragment : Fragment() {
    private val profileEditViewModel: ProfileEditViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        binding.viewModel = profileEditViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.profileEditRootLayout.enableTransition()
        binding.fragmentProfileEditToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        profileEditViewModel.navAction.observe(viewLifecycleOwner) {
            if (it == ProfileEditViewModel.NavAction.BACK) {
                findNavController().popBackStack()
            }
        }
        return binding.root
    }
}
