package cn.cercis.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import cn.cercis.common.LOG_TAG
import cn.cercis.common.NO_USER
import cn.cercis.common.UserId
import cn.cercis.common.matches
import cn.cercis.dao.LoginHistoryDao
import cn.cercis.repository.AuthRepository
import cn.cercis.util.livedata.generateMediatorLiveData
import cn.cercis.util.resource.NetworkResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import javax.inject.Inject

@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    loginHistoryDao: LoginHistoryDao,
) : ViewModel() {
    val loginError = MutableLiveData<String?>(null)
    val userId = MutableLiveData(authRepository.currentUserId.let {
        if (it == NO_USER) "" else it.toString()
    })
    val password = MutableLiveData("")
    private val isInputValid = generateMediatorLiveData(userId, password) {
        loginError.postValue(null)
        !userId.value.isNullOrEmpty() && !password.value.isNullOrEmpty()
    }
    private val isBusyLogin = MutableLiveData(false)
    val canSubmitLogin = generateMediatorLiveData(isInputValid, isBusyLogin) {
        isInputValid.value == true && isBusyLogin.value == false
    }
    val currentUserList = loginHistoryDao.loadLoginHistoryList()
        .asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)

    // used by AuthActivity
    val loggedIn = authRepository.loggedIn

    fun onLoginButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if (isBusyLogin.value == true) {
            return
        }
        // updates this value on main thread to prevent double submit
        isBusyLogin.value = true
        loginError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var id: UserId? = null
                var mobile: String? = null
                userId.value!!.let {
                    if (it matches  """\d{11}""") {
                        mobile = "+86${it}"
                    } else {
                        id = it.toLong()
                    }
                }
                val response = authRepository.login(
                    id = id,
                    mobile = mobile,
                    password = password.value!!
                )
                Log.d(LOG_TAG, "login response: $response")
                when (response) {
                    is NetworkResponse.Success -> {}
                    is NetworkResponse.Reject -> loginError.postValue(response.message)
                    is NetworkResponse.NetworkError -> loginError.postValue(response.message)
                }
            } finally {
                isBusyLogin.postValue(false)
            }
        }
    }
}
