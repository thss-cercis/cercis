package cn.edu.tsinghua.thss.cercis.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import cn.edu.tsinghua.thss.cercis.databinding.LayoutHomeBinding
import cn.edu.tsinghua.thss.cercis.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * This fragment is NOT USED now.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = LayoutHomeBinding.inflate(inflater, container, false)
        binding.topAppBar.setNavigationOnClickListener {
            binding.navDrawerLayout.openDrawer(GravityCompat.START)
        }
        return binding.root
    }
}