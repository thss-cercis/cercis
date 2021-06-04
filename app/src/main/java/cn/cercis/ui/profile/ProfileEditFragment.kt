package cn.cercis.ui.profile

import android.app.Activity.RESULT_OK
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.FragmentProfileEditBinding
import cn.cercis.util.getTempFile
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.snackbarMakeError
import cn.cercis.util.snackbarMakeSuccess
import cn.cercis.viewmodel.ProfileEditViewModel
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch


@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class ProfileEditFragment : Fragment() {
    private val profileEditViewModel: ProfileEditViewModel by viewModels()
    private lateinit var binding: FragmentProfileEditBinding
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
        binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
        profileEditViewModel.avatarUploadResult.observe(viewLifecycleOwner) {
            if (it == null) {
                return@observe
            }
            val (uri, res) = it
            when (res) {
                is NetworkResponse.NetworkError, is NetworkResponse.Reject -> {
                    snackbarMakeError(binding.root,
                        res.message!!,
                        Snackbar.LENGTH_SHORT) {
                        setAction(R.string.dialog_friend_request_retry) {
                            lifecycleScope.launch {
                                profileEditViewModel.uploadAvatar(uri)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            val resultUri = UCrop.getOutput(data!!)
            resultUri?.let {
                profileEditViewModel.uploadAvatar(resultUri)
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            snackbarMakeError(binding.root, cropError?.message ?: "", Snackbar.LENGTH_SHORT)
        }
    }
}
