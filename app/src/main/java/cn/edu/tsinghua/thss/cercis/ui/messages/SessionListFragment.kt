package cn.edu.tsinghua.thss.cercis.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.databinding.LayoutSessionsBinding
import cn.edu.tsinghua.thss.cercis.util.ActivityHelper
import cn.edu.tsinghua.thss.cercis.util.PreferencesHelper
import cn.edu.tsinghua.thss.cercis.viewmodel.SessionListViewModel

class SessionListFragment : Fragment() {
    private val sessionListViewModel: SessionListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = LayoutSessionsBinding.inflate(inflater, container, false)
        binding.topAppBar.setNavigationOnClickListener {
            binding.navDrawerLayout.openDrawer(GravityCompat.START)
        }
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId){
                R.id.nav_drawer_item_logout -> menuLogout()
            }
            true
        }
        return binding.root
    }

    private fun menuLogout() {
        val context = this.context
        if (context != null) {
            sessionListViewModel.loggedIn.postValue(false)
            ActivityHelper.switchToStartupActivity(context)
        }
    }

    private lateinit var binding: LayoutSessionsBinding
}