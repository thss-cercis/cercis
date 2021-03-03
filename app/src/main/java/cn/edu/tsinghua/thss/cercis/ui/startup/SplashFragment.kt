package cn.edu.tsinghua.thss.cercis.ui.startup

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.databinding.LayoutSplashBinding
import cn.edu.tsinghua.thss.cercis.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : Fragment() {
    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = LayoutSplashBinding.inflate(inflater, container, false)
        binding.viewModel = splashViewModel
        Log.d(TAG, "Switched to splash fragment.")
        binding.splashButtonGotoLogin.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
        binding.splashButtonGotoUserSignup.setOnClickListener {
            findNavController().navigate(R.id.signUpFragment)
        }
        return binding.root
    }
}