package cn.cercis.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import cn.cercis.MainActivity
import cn.cercis.databinding.FragmentProfileBinding
import cn.cercis.util.helper.doDetailNavigation
import cn.cercis.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.viewModel = profileViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.buttonProfileEdit.setOnClickListener {
            profileViewModel.currentUser.value?.run {
                doDetailNavigation(ProfileEditFragmentDirections.actionGlobalProfileEditFragment(
                    user = this
                ))
            }
        }
        binding.buttonLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                (requireActivity() as MainActivity).logout()
            }
        }
        return binding.root
    }
}
