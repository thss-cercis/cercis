package cn.cercis.ui.profile

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.FragmentProfileEditBinding
import cn.cercis.viewmodel.ProfileEditViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ProfileEditFragment : Fragment() {
    private val profileEditViewModel: ProfileEditViewModel by viewModels()
    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { it ->
                // The image was saved into the given Uri -> do something with it
                Log.d(LOG_TAG, it.toString())
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        binding.viewModel = profileEditViewModel
        binding.lifecycleOwner = viewLifecycleOwner
//        binding.profileEditRootLayout.enableTransition()
        binding.fragmentProfileEditToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        profileEditViewModel.navAction.observe(viewLifecycleOwner) {
            if (it == ProfileEditViewModel.NavAction.BACK) {
                findNavController().popBackStack()
            }
        }
        binding.profileEditAvatar.setOnClickListener {
            // load image
            pickImages.launch("image/*")
        }
        return binding.root
    }
}
