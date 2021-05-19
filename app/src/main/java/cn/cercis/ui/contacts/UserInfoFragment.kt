package cn.cercis.ui.contacts

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cn.cercis.MainActivity
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.FragmentUserInfoBinding
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.resource.Resource
import cn.cercis.util.snackbarMakeError
import cn.cercis.util.snackbarMakeSuccess
import cn.cercis.viewmodel.UserInfoViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
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
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentUserInfoBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = userInfoViewModel
            userInfoSendMessage.setOnClickListener { openChat() }
            userInfoAddFriend.setOnClickListener { sendFriendRequest() }
            userInfoDeleteFriend.setOnClickListener { deleteFriend() }
            executePendingBindings()
        }
        return binding.root
    }

    fun openChat() {
        lifecycleScope.launch(Dispatchers.IO) {
            userInfoViewModel.getChat().let { chatRes ->
                when (chatRes) {
                    is Resource.Error -> snackbarMakeError(
                        binding.root,
                        chatRes.message,
                        Snackbar.LENGTH_SHORT
                    )
                    is Resource.Success -> {
                        Log.d(LOG_TAG, "getting ${chatRes.data}")
                        // close self
                        launch(Dispatchers.Main) {
                            findNavController().popBackStack()
                            (requireActivity() as MainActivity).openChat(chatRes.data)
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }

    private fun sendFriendRequest() {
        AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_send_friend_request)
            .setTitle(getString(R.string.dialog_friend_request_title)
                .format(userInfoViewModel.userInfo.value?.nickname ?: ""))
            .setPositiveButton(R.string.dialog_friend_request_send) { dialog, _ ->
                dialog as AlertDialog
                val displayName =
                    dialog.findViewById<TextInputEditText>(R.id.dialog_friend_request_display_name_edit_text)?.text?.toString()
                        .takeIf { !it.isNullOrEmpty() }

                val remark =
                    dialog.findViewById<TextInputEditText>(R.id.dialog_friend_request_remark_edit_text)?.text?.toString()
                        .takeIf { !it.isNullOrEmpty() }
                lifecycleScope.launch(Dispatchers.IO) {
                    val makeRequest = suspend {
                        userInfoViewModel.sendFriendApply(displayName, remark)
                    }
                    val result = MutableLiveData(makeRequest())
                    launch(Dispatchers.Main) {
                        result.observe(viewLifecycleOwner) {
                            when (it) {
                                is NetworkResponse.NetworkError, is NetworkResponse.Reject -> {
                                    snackbarMakeError(binding.root,
                                        it.message!!,
                                        Snackbar.LENGTH_SHORT) {
                                        setAction(R.string.dialog_friend_request_retry) {
                                            launch {
                                                result.postValue(makeRequest())
                                            }
                                        }
                                    }
                                }
                                is NetworkResponse.Success -> snackbarMakeSuccess(binding.root,
                                    getString(R.string.dialog_friend_request_sent),
                                    Snackbar.LENGTH_SHORT
                                )
                                null -> Unit
                            }
                        }
                    }
                }
            }
            .setNegativeButton(R.string.dialog_friend_request_cancel) { _, _ -> }
            .show()
    }

    private fun deleteFriend() {
        lifecycleScope.launch(Dispatchers.IO) {
            val res = userInfoViewModel.deleteFriend()
            if (res !is NetworkResponse.Success) {
                snackbarMakeError(binding.root,
                    res.message!!,
                    Snackbar.LENGTH_SHORT) {
                    setAction(R.string.dialog_friend_request_retry) {
                        deleteFriend()
                    }
                }
            }
        }
    }
}
