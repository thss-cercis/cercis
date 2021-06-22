package cn.cercis.ui.activity

import android.content.ContentResolver
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.databinding.FragmentActivityNewBinding
import cn.cercis.util.getTempFile
import cn.cercis.viewmodel.ActivityNewViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import java.io.FileOutputStream
import java.io.IOException

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
            viewModel.submit {
                findNavController().popBackStack()
            }
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
                        viewModel.addImage(file)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        binding.activityNewAddImage.setOnClickListener {
            fromGalleryLauncher.launch("image/*")
        }

        return binding.root
    }
}
