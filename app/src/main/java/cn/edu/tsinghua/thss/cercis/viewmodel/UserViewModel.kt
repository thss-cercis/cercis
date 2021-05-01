package cn.edu.tsinghua.thss.cercis.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cn.edu.tsinghua.thss.cercis.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
        application: Application,
        val userRepository: UserRepository,
) : AndroidViewModel(application) {
    /**
     * Checks if a user is logged in.
     *
     * Setting this value would trigger a write-back to shared preferences.
     * @see UserRepository.loggedIn
     */
    val loggedIn = userRepository.loggedIn
}
