package cn.cercis.ui.startup

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.R
import cn.cercis.databinding.FragmentLoginBinding
import cn.cercis.util.LOG_TAG
import cn.cercis.util.enableTransition
import cn.cercis.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class LoginFragment : Fragment() {
    private val loginViewModel: LoginViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding.viewModel = loginViewModel
        binding.loginRootLayout.enableTransition()
        binding.lifecycleOwner = this
        loginViewModel.loginError.observe(viewLifecycleOwner) {
            binding.signupPassword.error = it
        }
        binding.linkResetPassword.setOnClickListener {
            Log.d(LOG_TAG, "reset password not implemented")
            Toast.makeText(context, "暂不支持密码重置", Toast.LENGTH_SHORT).show()
        }
        binding.linkUserSignup.setOnClickListener {
            findNavController().navigate(R.id.signUpFragment1)
        }
        loginViewModel.currentUserList.observe(viewLifecycleOwner) {
            binding.loginUserIdList.setAdapter(ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    it.map { item -> item.userId.toString() }
            ))
        }
        (binding.root as ViewGroup).enableTransition()
        return binding.root
    }
}
