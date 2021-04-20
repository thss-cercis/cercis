package cn.edu.tsinghua.thss.cercis

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import cn.edu.tsinghua.thss.cercis.databinding.ActivityAuthBinding
import cn.edu.tsinghua.thss.cercis.databinding.ActivityMainBinding
import cn.edu.tsinghua.thss.cercis.repository.UserRepository
import cn.edu.tsinghua.thss.cercis.util.LOG_TAG
import cn.edu.tsinghua.thss.cercis.util.PreferencesHelper
import cn.edu.tsinghua.thss.cercis.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(LOG_TAG, "Loading AuthActivity")
        userViewModel.loggedIn.observe(this) {
            if (it == true) {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }

    }
}