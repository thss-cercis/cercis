package cn.edu.tsinghua.thss.cercis.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.databinding.LayoutSessionBinding
import cn.edu.tsinghua.thss.cercis.viewmodel.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SessionFragment : Fragment() {
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = LayoutSessionBinding.inflate(inflater, container, false)
        binding.viewModel = sessionViewModel
        binding.executePendingBindings()
        binding.root.setOnClickListener { view -> findNavController().navigate(R.id.homeFragment) }
        return binding.root
    }
}