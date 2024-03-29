package cn.cercis.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.databinding.FragmentSignupBinding
import cn.cercis.util.helper.enableTransition
import cn.cercis.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class SignUpFragment : Fragment() {
    private val signUpViewModel: SignUpViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentSignupBinding.inflate(inflater, container, false)
        binding.viewModel = signUpViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.signupRootLayout.enableTransition()
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
                when (it.first) {
                    SignUpViewModel.NavAction.FRAGMENT -> Unit
                    SignUpViewModel.NavAction.FRAGMENT_SUCCESS -> findNavController().navigate(
                        SignUpFragmentDirections.actionSignUpFragmentToSignUpSuccessFragment(it.second))
                    SignUpViewModel.NavAction.BACK -> findNavController().popBackStack()
                }
            }
        }
        signUpViewModel.verificationError.observe(viewLifecycleOwner) {
            binding.signupCode.error = it
        }
        return binding.root
    }
}
