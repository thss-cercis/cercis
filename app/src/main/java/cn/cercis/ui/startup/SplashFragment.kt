package cn.cercis.ui.startup

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.FragmentSplashBinding
import cn.cercis.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : Fragment() {
    private val splashViewModel: SplashViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSplashBinding.inflate(inflater, container, false)
        binding.viewModel = splashViewModel
        Log.d(LOG_TAG, "Switched to splash fragment.")
        binding.splashButtonGotoLogin.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
        binding.splashButtonGotoUserSignup.setOnClickListener {
            findNavController().navigate(R.id.signUpFragment)
        }
        return binding.root
    }
}
