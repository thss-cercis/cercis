package cn.cercis.viewmodel

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import cn.cercis.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(authRepository: AuthRepository) : ViewModel() {
    val loggedIn = authRepository.loggedIn
    // this initial value is related to the one in [master_nav_graph]
    val detailHasNavigationDestination = MutableLiveData(false)
    val masterVisible = Transformations.map(detailHasNavigationDestination) {
        when (it) {
            null, false -> View.VISIBLE
            else -> View.GONE
        }
    }
    val detailVisible = Transformations.map(detailHasNavigationDestination) {
        when (it) {
            true -> View.VISIBLE
            else -> View.GONE
        }
    }
}
