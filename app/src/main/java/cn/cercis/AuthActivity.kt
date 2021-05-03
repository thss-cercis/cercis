package cn.cercis

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import cn.cercis.databinding.ActivityAuthBinding
import cn.cercis.util.LOG_TAG
import cn.cercis.viewmodel.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
            Configuration.SCREENLAYOUT_SIZE_SMALL,
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        val binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(LOG_TAG, "Loading AuthActivity")
        loginViewModel.loggedIn.observe(this) {
            if (it == true) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
