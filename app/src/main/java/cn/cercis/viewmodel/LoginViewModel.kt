package cn.cercis.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.*
import cn.cercis.common.LOG_TAG
import cn.cercis.common.NO_USER
import cn.cercis.common.UserId
import cn.cercis.dao.LoginHistoryDao
import cn.cercis.repository.AuthRepository
import cn.cercis.util.NetworkResponse
import cn.cercis.util.PairLiveData
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
    private val isInputValid = Transformations.map(PairLiveData(userId, password)) {
        loginError.postValue(null)
        !it.first.isNullOrEmpty() && !it.second.isNullOrEmpty()
    }
    private val isBusyLogin = MutableLiveData(false)
    val canSubmitLogin = Transformations.map(PairLiveData(isInputValid, isBusyLogin)) {
        it.first == true && it.second == false
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
                    if (it.matches(Regex("""\d{11}"""))) {
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
