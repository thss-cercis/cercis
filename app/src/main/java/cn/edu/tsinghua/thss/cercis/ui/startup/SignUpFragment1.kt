package cn.edu.tsinghua.thss.cercis.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.databinding.LayoutSignup1Binding
import cn.edu.tsinghua.thss.cercis.util.enableTransition
import cn.edu.tsinghua.thss.cercis.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment1 : Fragment() {
    private val signUpViewModel: SignUpViewModel by hiltNavGraphViewModels(R.id.signup_nav_graph)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = LayoutSignup1Binding.inflate(inflater, container, false)
        binding.viewModel = signUpViewModel
        binding.lifecycleOwner = this
        binding.signup1RootLayout.enableTransition()
        signUpViewModel.signUpError.observe(viewLifecycleOwner) {
            binding.signupSubmitError.text = it
        }
        signUpViewModel.passwordError.observe(viewLifecycleOwner) {
            binding.signupPassword.error = it
        }
        signUpViewModel.verificationError.observe(viewLifecycleOwner) {
            binding.signupCode.error = it
        }
        signUpViewModel.navAction.observe(viewLifecycleOwner) {
            it?.let {
                when (it) {
                    SignUpViewModel.NavAction.FRAGMENT1 -> Unit
                    SignUpViewModel.NavAction.FRAGMENT_SUCCESS -> findNavController().navigate(R.id.signUpSuccessFragment)
                    SignUpViewModel.NavAction.LOGIN -> findNavController().navigate(R.id.action_global_loginFragment)
                }
                signUpViewModel.navAction.value = null
            }
        }
        signUpViewModel.verificationError.observe(viewLifecycleOwner) {
            binding.signupCode.error = it
        }
        return binding.root
    }
}