package cn.cercis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.coroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import cn.cercis.common.LOG_TAG
import cn.cercis.databinding.ActivityMainBinding
import cn.cercis.repository.AuthRepository
import cn.cercis.service.NotificationService
import cn.cercis.ui.activity.ActivityFragment
import cn.cercis.ui.chat.ChatListFragment
import cn.cercis.ui.contacts.ContactListFragment
import cn.cercis.ui.empty.EmptyFragment
import cn.cercis.ui.profile.ProfileFragment
import cn.cercis.util.resource.NetworkResponse
import cn.cercis.util.setupWithNavController
import cn.cercis.viewmodel.MainActivityViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalCoroutinesApi
@AndroidEntryPoint
@FlowPreview
class MainActivity : AppCompatActivity() {
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var currentNavController: LiveData<NavController>

    @Inject
    lateinit var authRepository: AuthRepository
    private val loggedInObserver = Observer<Boolean?> {
        if (it == false) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.main_401_dialog_title)
                .setMessage(
                    getString(R.string.main_401_dialog_body)
                        .format(getString(R.string.main_401_dialog_ok_button))
                )
                .setPositiveButton(R.string.main_401_dialog_ok_button) { _, _ ->
                    finishActivity()
                }
                .setOnDismissListener { finishActivity() }
                .show()

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.lifecycleOwner = this
        binding.viewModel = mainActivityViewModel
        setContentView(binding.root)

        // check login status and automatically jumps to login view
        mainActivityViewModel.loggedIn.observe(this, loggedInObserver)

        binding.reusedView.masterViewContainer.apply {
            adapter = object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
                override fun getItemCount(): Int {
                    return 4
                }

                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> ChatListFragment()
                        1 -> ContactListFragment()
                        2 -> EmptyFragment()// ActivityFragment()
                        3 -> ProfileFragment()
                        else -> throw IllegalStateException("out of index") // should not happen
                    }
                }
            }
            isUserInputEnabled = false
        }

        // starts the notification service
        // NotificationService runs in the background even if MainActivity dies.
        // Calling startService on this service multiple times would make no difference from calling once.
        startService(Intent(applicationContext, NotificationService::class.java))

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        // ensure fragment get noticed
        super.onNewIntent(intent)
        // todo finish logic here
        Log.d(LOG_TAG, "started intent: ${intent.toString()}")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setupBottomNavigationBar()
    }

    @MainThread
    fun doDetailNavigation(id: Int) {
        detailHost.navController.navigate(id)
    }

    @MainThread
    fun logout() {
        lifecycle.coroutineScope.launch(Dispatchers.Main) {
            if (authRepository.logout() is NetworkResponse.Success) {
                mainActivityViewModel.loggedIn.apply {
                    removeObserver(loggedInObserver)
                    value = false
                }
                finishActivity()
            }
        }
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
        if (navIds[0] == R.id.chat_list_nav_graph) {
            Log.e(LOG_TAG, "test")
            Log.e(LOG_TAG, "${navIds[0]} == ${R.id.chat_list_nav_graph}")
        }

        val controller = bottomNavigation.setupWithNavController(
            navGraphIds = navIds,
            masterViewPager2 = binding.reusedView.masterViewContainer,
            fragmentManager = supportFragmentManager,
            containerId = R.id.detail_view_container,
            intent = intent
        )

        val listener = NavController.OnDestinationChangedListener { navController, dest, _ ->
//            val isStartDest = dest.id == navController.graph.startDestination
//            mainActivityViewModel.detailHasNavigationDestination.postValue(
//                !isStartDest
//            )
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

    private fun finishActivity() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    private val isMasterDetail: Boolean by lazy {
        resources.getBoolean(R.bool.is_master_detail)
    }
}
