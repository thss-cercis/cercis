package cn.edu.tsinghua.thss.cercis.viewmodel

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.*
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.api.CercisHttpService
import cn.edu.tsinghua.thss.cercis.api.LoginRequest
import cn.edu.tsinghua.thss.cercis.dao.CurrentUser
import cn.edu.tsinghua.thss.cercis.dao.UserDao
import cn.edu.tsinghua.thss.cercis.repository.UserRepository
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
    val userId = MutableLiveData("")
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

    private fun login() {
        viewModelScope.launch(Dispatchers.IO) {
            isBusyLogin.postValue(true)
            try {
                val response = httpService.login(LoginRequest(userId.value!!, password.value!!))
                Log.d(ContentValues.TAG, "login response: $response")
                if (response.successful) {
                    userRepository.loggedIn.postValue(true)
                } else {
                    loginError.postValue(response.msg)
                }
            } catch (t: Throwable) {
                Log.e(ContentValues.TAG, "login error: $t")
                loginError.postValue(context.getString(R.string.error_network_exception))
            } finally {
                isBusyLogin.postValue(false)
            }
        }
    }
}
