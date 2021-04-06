package cn.edu.tsinghua.thss.cercis.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.edu.tsinghua.thss.cercis.dao.CurrentUser
import cn.edu.tsinghua.thss.cercis.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
        val userRepository: UserRepository
): ViewModel() {
    fun currentUser(): LiveData<CurrentUser?> {
        return userRepository.currentUser(viewModelScope)
    }

    fun refreshCurrentUser() {
        userRepository.currentUser(viewModelScope)
    }

    fun logout() {
        userRepository.loggedIn.postValue(false)
    }

}