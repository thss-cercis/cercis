package cn.edu.tsinghua.thss.cercis.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cn.edu.tsinghua.thss.cercis.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionListViewModel @Inject constructor(
        application: Application,
        userRepository: UserRepository,
) : AndroidViewModel(application) {
    val loggedIn = userRepository.loggedIn
}