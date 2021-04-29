package cn.edu.tsinghua.thss.cercis.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.databinding.LayoutSignupSuccessBinding
import cn.edu.tsinghua.thss.cercis.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpSuccessFragment : Fragment() {
    private val signUpViewModel: SignUpViewModel by navGraphViewModels(R.id.signup_nav_graph)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = LayoutSignupSuccessBinding.inflate(inflater, container, false)
        binding.viewModel = signUpViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        signUpViewModel.navAction.observe(viewLifecycleOwner) {
            it?.let {
                when (it) {
                    SignUpViewModel.NavAction.FRAGMENT1 -> Unit
                    SignUpViewModel.NavAction.FRAGMENT_SUCCESS -> Unit
                    SignUpViewModel.NavAction.LOGIN -> findNavController().navigate(R.id.action_global_loginFragment)
                }
                signUpViewModel.navAction.value = null
            }
        }
        return binding.root
    }
}