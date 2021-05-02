package cn.edu.tsinghua.thss.cercis.viewmodel

import androidx.lifecycle.ViewModel
import cn.edu.tsinghua.thss.cercis.http.AuthenticationData
import cn.edu.tsinghua.thss.cercis.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authenticationData: AuthenticationData
): ViewModel() {
    fun logout() {
        authenticationData.loggedIn.postValue(false)
    }
}
