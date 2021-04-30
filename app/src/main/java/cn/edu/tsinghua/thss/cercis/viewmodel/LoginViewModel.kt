package cn.edu.tsinghua.thss.cercis.viewmodel

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.annotation.MainThread
import androidx.lifecycle.*
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.api.CercisHttpService
import cn.edu.tsinghua.thss.cercis.api.LoginRequest
import cn.edu.tsinghua.thss.cercis.dao.CurrentUser
import cn.edu.tsinghua.thss.cercis.dao.UserDao
import cn.edu.tsinghua.thss.cercis.repository.UserRepository
import cn.edu.tsinghua.thss.cercis.util.LOG_TAG
import cn.edu.tsinghua.thss.cercis.util.NetworkResponse
import cn.edu.tsinghua.thss.cercis.util.PairLiveData
import cn.edu.tsinghua.thss.cercis.util.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpService: CercisHttpService,
    private val userRepository: UserRepository,
) : ViewModel() {
    val userId: MutableLiveData<String> = MutableLiveData(userRepository.currentUserId.value.let {
        when (it) {
            -1L, null -> ""
            else -> it.toString()
        }
    })
    val password = MutableLiveData("")
    val isUserIdPasswordValid: LiveData<Boolean> =
        Transformations.map(PairLiveData(userId, password)) {
            loginError.postValue(null)
            !it.first.isNullOrEmpty() && !it.second.isNullOrEmpty()
        }
    val isBusyLogin = MutableLiveData(false)
    val canSubmitLogin = Transformations.map(PairLiveData(isUserIdPasswordValid, isBusyLogin)) {
        it.first == true && it.second == false
    }
    val loginError = MutableLiveData<String?>(null)
    val currentUserList: LiveData<List<CurrentUser>> = run {
        userRepository.currentUsers
    }
    val currentUserId: LiveData<UserId> = run {
        userRepository.currentUserId
    }

    fun onLoginButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        login()
    }

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
                    is NetworkResponse.Success -> userRepository.loggedIn.postValue(true)
                    is NetworkResponse.Reject -> loginError.postValue(response.message)
                    is NetworkResponse.NetworkError -> loginError.postValue(response.message)
                }
            } finally {
                isBusyLogin.postValue(false)
            }
        }
    }
}
