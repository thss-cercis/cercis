package cn.edu.tsinghua.thss.cercis.ui.startup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cn.edu.tsinghua.thss.cercis.databinding.LayoutSignupBinding
import cn.edu.tsinghua.thss.cercis.viewmodel.SignUpViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpFragment : Fragment() {
    private val signUpViewModel: SignUpViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = LayoutSignupBinding.inflate(inflater, container, false)
        binding.viewModel = signUpViewModel
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}