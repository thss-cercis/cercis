package cn.cercis.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.R
import cn.cercis.databinding.FragmentSignupSuccessBinding
import cn.cercis.viewmodel.SignUpSuccessViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpSuccessFragment : Fragment() {
    private val signupSuccessViewModel: SignUpSuccessViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentSignupSuccessBinding.inflate(inflater, container, false)
        binding.viewModel = signupSuccessViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.signupSuccessSubmit.setOnClickListener {
            findNavController().navigate(R.id.action_global_loginFragment)
        }
        return binding.root
    }
}
