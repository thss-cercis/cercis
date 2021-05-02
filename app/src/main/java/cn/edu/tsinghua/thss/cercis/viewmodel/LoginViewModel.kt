package cn.edu.tsinghua.thss.cercis.viewmodel

import android.util.Log
import android.view.View
import androidx.annotation.MainThread
import androidx.lifecycle.*
import cn.edu.tsinghua.thss.cercis.dao.UserDao
import cn.edu.tsinghua.thss.cercis.http.CercisHttpService
import cn.edu.tsinghua.thss.cercis.http.LoginRequest
import cn.edu.tsinghua.thss.cercis.module.AuthorizedLiveEvent
import cn.edu.tsinghua.thss.cercis.repository.AuthRepository
import cn.edu.tsinghua.thss.cercis.util.LOG_TAG
import cn.edu.tsinghua.thss.cercis.util.NetworkResponse
import cn.edu.tsinghua.thss.cercis.util.PairLiveData
import cn.edu.tsinghua.thss.cercis.util.SingleLiveEvent
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
    @AuthorizedLiveEvent val authorized: SingleLiveEvent<Boolean?>,
    private val httpService: CercisHttpService,
    private val authRepository: AuthRepository,
    userDao: UserDao,
) : ViewModel() {
    val loginError = MutableLiveData<String?>(null)

    val userId = MutableLiveData(authRepository.userId.let {
        if (it == -1L) "" else it.toString()
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

    val currentUserList = userDao.loadAllLoginHistory()
        .asLiveData(viewModelScope.coroutineContext + Dispatchers.IO)

    val loggedIn = MutableLiveData(authRepository.loggedIn).apply {
        Log.d(LOG_TAG, "${authorized.hashCode()}")
        authorized.observeForever {
            if (it == false) {
                authRepository.logout()
                postValue(false)
            }
        }
    }

    fun onLoginButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) = login()

    @MainThread
    private fun login() {
        if (isBusyLogin.value == true) {
            return
        }
        // updates this value on main thread to prevent double submit
        isBusyLogin.value = true
        loginError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loginRequest = userId.value!!.let {
                    if (it.matches(Regex("\\d{11}"))) {
                        LoginRequest(mobile = "+86${it}", password = password.value!!, id = null)
                    } else {
                        LoginRequest(id = it.toLong(), password = password.value!!, mobile = null)
                    }
                }
                val response = httpService.login(loginRequest)
                Log.d(LOG_TAG, "login response: $response")
                when (response) {
                    is NetworkResponse.Success -> {
                        authRepository.userId = response.data.userId
                        loggedIn.postValue(true)
                    }
                    is NetworkResponse.Reject -> loginError.postValue(response.message)
                    is NetworkResponse.NetworkError -> loginError.postValue(response.message)
                }
            } finally {
                isBusyLogin.postValue(false)
            }
        }
    }
}
