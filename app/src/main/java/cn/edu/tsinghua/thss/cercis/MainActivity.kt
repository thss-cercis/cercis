package cn.edu.tsinghua.thss.cercis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import cn.edu.tsinghua.thss.cercis.databinding.ActivityMainBinding
import cn.edu.tsinghua.thss.cercis.ui.activity.ActivityFragment
import cn.edu.tsinghua.thss.cercis.ui.contacts.ContactListFragment
import cn.edu.tsinghua.thss.cercis.ui.profile.ProfileFragment
import cn.edu.tsinghua.thss.cercis.ui.chatList.ChatListFragment
import cn.edu.tsinghua.thss.cercis.util.LOG_TAG
import cn.edu.tsinghua.thss.cercis.util.setupWithNavController
import cn.edu.tsinghua.thss.cercis.viewmodel.MainActivityViewModel
import cn.edu.tsinghua.thss.cercis.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@AndroidEntryPoint
@FlowPreview
class MainActivity : AppCompatActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var currentNavController: LiveData<NavController>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.viewModel = mainActivityViewModel
        setContentView(binding.root)

        // check login status and automatically jumps to login view
        loginViewModel.loggedIn.observe(this) {
            if (it == false) {
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
        }

        binding.reusedView.masterViewContainer.apply {
            adapter = object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
                override fun getItemCount(): Int {
                    return 4
                }

                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> ChatListFragment()
                        1 -> ContactListFragment()
                        2 -> ActivityFragment()
                        3 -> ProfileFragment()
                        else -> throw IllegalStateException("out of index") // should not happen
                    }
                }
            }
            isUserInputEnabled = false
        }

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setupBottomNavigationBar()
    }

    @MainThread
    fun doDetailNavigation(id: Int) {
        detailHost.navController.navigate(id)
    }

    private val detailHost: NavHostFragment
        get() {
            return supportFragmentManager.findFragmentById(R.id.detail_view_container) as NavHostFragment
        }

    @MainThread
    fun doDetailNavigation(navDirections: NavDirections) {
        detailHost.navController.navigate(navDirections)
    }

    private fun setupBottomNavigationBar() {
        val bottomNavigation = binding.reusedView.bottomNavigation
        val navIds = listOf(
            R.navigation.chat_list_nav_graph,
            R.navigation.contact_list_nav_graph,
            R.navigation.activity_list_nav_graph,
            R.navigation.profile_nav_graph,
        )
        if (navIds[0] == R.id.session_list_nav_graph) {
            Log.e(LOG_TAG, "test")
            Log.e(LOG_TAG, "${navIds[0]} == ${R.id.session_list_nav_graph}")
        }

        val controller = bottomNavigation.setupWithNavController(
            navGraphIds = navIds,
            masterViewPager2 = binding.reusedView.masterViewContainer,
            fragmentManager = supportFragmentManager,
            containerId = R.id.detail_view_container,
            intent = intent
        )

        val listener = NavController.OnDestinationChangedListener { navController, dest, _ ->
            val isStartDest = dest.id == navController.graph.startDestination
            mainActivityViewModel.detailHasNavigationDestination.postValue(
                !isStartDest
            )
        }

        controller.observe(this) { navController ->
            navController?.let {
                it.removeOnDestinationChangedListener(listener)
                it.addOnDestinationChangedListener(listener)
                if (it.currentBackStackEntry == null) {
                    mainActivityViewModel.detailHasNavigationDestination.postValue(false)
                }
            }
        }

        currentNavController = controller
    }

    private val isMasterDetail: Boolean by lazy {
        resources.getBoolean(R.bool.is_master_detail)
    }
}
