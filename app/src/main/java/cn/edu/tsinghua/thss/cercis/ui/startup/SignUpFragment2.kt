package cn.edu.tsinghua.thss.cercis.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.databinding.LayoutSignup2Binding
import cn.edu.tsinghua.thss.cercis.util.enableTransition
import cn.edu.tsinghua.thss.cercis.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment2 : Fragment() {
    private val signUpViewModel: SignUpViewModel by navGraphViewModels(R.id.signup_nav_graph)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = LayoutSignup2Binding.inflate(inflater, container, false)
        binding.viewModel = signUpViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.signup2RootLayout.enableTransition()
        signUpViewModel.signUpError.observe(viewLifecycleOwner) {
            binding.signupSubmitError.error = it
        }
        signUpViewModel.passwordError.observe(viewLifecycleOwner) {
            binding.signupPassword.error = it
        }
        signUpViewModel.navAction.observe(viewLifecycleOwner) {
            when(it) {
                SignUpViewModel.NavAction.FRAGMENT1 -> Unit
                SignUpViewModel.NavAction.FRAGMENT2 -> Unit
                SignUpViewModel.NavAction.FRAGMENT_SUCCESS -> findNavController().navigate(R.id.signUpSuccessFragment)
                SignUpViewModel.NavAction.LOGIN -> findNavController().navigate(R.id.action_global_loginFragment)
                else -> Unit
            }
        }
        return binding.root
    }
}

