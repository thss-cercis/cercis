package cn.edu.tsinghua.thss.cercis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import cn.edu.tsinghua.thss.cercis.databinding.ActivityMainBinding
import cn.edu.tsinghua.thss.cercis.util.LOG_TAG
import cn.edu.tsinghua.thss.cercis.util.NavDirectionsById
import cn.edu.tsinghua.thss.cercis.util.setupWithNavController
import cn.edu.tsinghua.thss.cercis.viewmodel.MainActivityViewModel
import cn.edu.tsinghua.thss.cercis.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val userViewModel: UserViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private lateinit var masterHost: NavHostFragment
    private lateinit var binding: ActivityMainBinding
    private lateinit var currentNavController: LiveData<NavController>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.viewModel = mainActivityViewModel
        setContentView(binding.root)

        masterHost = supportFragmentManager.findFragmentById(R.id.master_view_container) as NavHostFragment

        // check login status and automatically jumps to login view
        userViewModel.loggedIn.observe(this) {
            if (it == false) {
                startActivity(Intent(this, AuthActivity::class.java))
            }
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
                R.navigation.session_list_nav_graph,
                R.navigation.contact_list_nav_graph,
                R.navigation.activity_list_nav_graph,
                R.navigation.profile_nav_graph,
        )
        val masterNavDestinations = listOf(
                NavDirectionsById(R.id.sessionListFragment),
                NavDirectionsById(R.id.contactListFragment),
                NavDirectionsById(R.id.activityFragment),
                NavDirectionsById(R.id.profileFragment),
        )
        if (navIds[0] == R.id.session_list_nav_graph) {
            Log.e(LOG_TAG, "test")
            Log.e(LOG_TAG, "${navIds[0]} == ${R.id.session_list_nav_graph}")
        }

        val controller = bottomNavigation.setupWithNavController(
                navGraphIds = navIds,
                masterNavController = masterHost.navController,
                masterNavDestinations = masterNavDestinations,
                fragmentManager = supportFragmentManager,
                containerId = R.id.detail_view_container,
                intent = intent
        )

        val listener = NavController.OnDestinationChangedListener { navController, dest, _ ->
            val isNotEmptyFragmentDest = dest.id != R.id.action_global_emptyFragment && dest.id != R.id.emptyFragment
            val isNotStackBottom = navController.currentBackStackEntry == null
            mainActivityViewModel.detailHasNavigationDestination.postValue(
                    isNotEmptyFragmentDest || isNotStackBottom
            )
        }

        controller.observe(this) { navController ->
            navController?.let {
                it.removeOnDestinationChangedListener(listener)
                it.addOnDestinationChangedListener(listener)
            }
        }

        currentNavController = controller
    }
}