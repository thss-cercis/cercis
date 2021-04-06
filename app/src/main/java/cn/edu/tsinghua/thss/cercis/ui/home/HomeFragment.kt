package cn.edu.tsinghua.thss.cercis.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.databinding.LayoutHomeBinding
import cn.edu.tsinghua.thss.cercis.util.doGlobalNavigation
import cn.edu.tsinghua.thss.cercis.viewmodel.HomeViewModel
import cn.edu.tsinghua.thss.cercis.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = LayoutHomeBinding.inflate(inflater, container, false)
        binding.topAppBar.setNavigationOnClickListener {
            binding.navDrawerLayout.openDrawer(GravityCompat.START)
        }
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_drawer_item_logout -> homeViewModel.logout()
            }
            true
        }
        binding.bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            val navController = Navigation.findNavController(binding.navHostFragment)
            when (menuItem.itemId) {
                R.id.page_messages -> navController.navigate(R.id.action_global_sessionListFragment)
                R.id.page_contacts -> navController.navigate(R.id.action_global_contactListFragment)
                R.id.page_discovery -> {
                    // THIS IS A SPECIAL CASE
                    // starts discovery fragment directly in global
                    doGlobalNavigation(R.id.action_homeFragment_to_discoveryFragment)
                }
            }
            true
        }
        return binding.root
    }

    private fun menuLogout() {

    }
}