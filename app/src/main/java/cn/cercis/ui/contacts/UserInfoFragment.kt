package cn.cercis.ui.contacts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.coroutineScope
import cn.cercis.MainActivity
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.FragmentUserInfoBinding
import cn.cercis.util.resource.Resource
import cn.cercis.util.snackbarMakeError
import cn.cercis.viewmodel.UserInfoViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class UserInfoFragment : Fragment() {
    private val userInfoViewModel: UserInfoViewModel by viewModels()
    private lateinit var binding: FragmentUserInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserInfoBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = userInfoViewModel
            userInfoSendMessage.setOnClickListener { openChat() }
        }
        return binding.root
    }

    fun openChat() {
        lifecycle.coroutineScope.launch(Dispatchers.IO) {
            userInfoViewModel.getChat().let { chatRes ->
                when (chatRes) {
                    is Resource.Error -> snackbarMakeError(
                        binding.root,
                        chatRes.message,
                        Snackbar.LENGTH_SHORT
                    )
                    is Resource.Success -> {
                        Log.d(LOG_TAG, "getting ${chatRes.data}")
                        launch(Dispatchers.Main) {
                            (requireActivity() as MainActivity).openChat(chatRes.data)
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }
}
