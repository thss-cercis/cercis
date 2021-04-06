package cn.edu.tsinghua.thss.cercis

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavDirections
import androidx.navigation.NavGraph
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import cn.edu.tsinghua.thss.cercis.databinding.ActivityMainBinding
import cn.edu.tsinghua.thss.cercis.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container) as NavHostFragment
        val navInflater = navHostFragment.navController.navInflater
        // check login status and automatically jumps to login view
        val graph: NavGraph = navInflater.inflate(R.navigation.global_nav_graph)
        graph.startDestination = when (userViewModel.loggedIn.value) {
            true -> R.id.homeFragment
            false -> R.id.startup_nav_graph
            else -> R.id.startup_nav_graph
        }
        navHostFragment.navController.graph = graph

        userViewModel.loggedIn.observe(this) {
            if (it == true) {
                findNavController(R.id.main_fragment_container).navigate(R.id.homeFragment)
            } else {
                findNavController(R.id.main_fragment_container).navigate(R.id.startup_nav_graph)
            }
        }
    }

    fun doGlobalNavigation(id: Int) {
        findNavController(R.id.main_fragment_container).navigate(id)
    }

    fun doGlobalNavigation(navDirections: NavDirections) {
        findNavController(R.id.main_fragment_container).navigate(navDirections)
    }
}