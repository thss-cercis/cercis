package cn.edu.tsinghua.thss.cercis.ui.startup

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.databinding.LayoutLoginBinding
import cn.edu.tsinghua.thss.cercis.util.enableTransition
import cn.edu.tsinghua.thss.cercis.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private val loginViewModel: LoginViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = LayoutLoginBinding.inflate(inflater, container, false)
        binding.viewModel = loginViewModel
        binding.loginRootLayout.enableTransition()
        binding.lifecycleOwner = this
        loginViewModel.loginError.observe(viewLifecycleOwner) {
            binding.signupPassword.error = it
        }
        binding.linkResetPassword.setOnClickListener {
            Log.d(TAG, "reset password not implemented")
        }
        binding.linkUserSignup.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signup_nav_graph)
        }
        loginViewModel.currentUserList.observe(viewLifecycleOwner) {
            binding.loginUserIdList.setAdapter(ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    it.map { it1 -> it1.id.toString() }
            ))
        }
        loginViewModel.currentUserId.observe(viewLifecycleOwner) {
            binding.loginUserIdList.setText(it.toString())
        }
        return binding.root
    }
}