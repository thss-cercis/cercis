package cn.cercis

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import cn.cercis.common.LOG_TAG
import cn.cercis.util.PreferencesHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
@AndroidEntryPoint
class StartupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // we do not use user repository here to speed up loading
        Log.d(LOG_TAG, "Logged in? ${PreferencesHelper.Auth.isLoggedIn(this)}")
        when (PreferencesHelper.Auth.isLoggedIn(this)) {
            true -> startActivity(Intent(this, MainActivity::class.java))
            false -> startActivity(Intent(this, AuthActivity::class.java))
        }
        finish()
    }
}
