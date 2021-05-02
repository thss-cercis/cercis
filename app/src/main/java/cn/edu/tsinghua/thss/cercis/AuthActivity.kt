package cn.edu.tsinghua.thss.cercis

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import cn.edu.tsinghua.thss.cercis.databinding.ActivityAuthBinding
import cn.edu.tsinghua.thss.cercis.util.LOG_TAG
import cn.edu.tsinghua.thss.cercis.viewmodel.LoginViewModel
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
        val screenLayoutSize =
            resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        if (screenLayoutSize == Configuration.SCREENLAYOUT_SIZE_SMALL || screenLayoutSize == Configuration.SCREENLAYOUT_SIZE_NORMAL) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
