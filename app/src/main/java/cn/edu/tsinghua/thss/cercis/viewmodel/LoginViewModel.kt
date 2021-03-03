package cn.edu.tsinghua.thss.cercis.viewmodel

import android.content.ContentValues
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.api.CercisHttpService
import cn.edu.tsinghua.thss.cercis.api.EmptyResponse
import cn.edu.tsinghua.thss.cercis.api.LoginRequest
import cn.edu.tsinghua.thss.cercis.dao.UserDao
import cn.edu.tsinghua.thss.cercis.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.Call
import retrofit2.Callback
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val httpService: CercisHttpService,
        private val userRepository: UserRepository,
        private val userDao: UserDao,
) : ViewModel() {
    val userId = MutableLiveData("")
    val password = MutableLiveData("")
    val isValid by lazy {
        val liveData = MediatorLiveData<Boolean>()
        liveData.addSource(userId) {
            liveData.value = !TextUtils.isEmpty(userId.value) && !TextUtils.isEmpty(password.value)
        }
        liveData.addSource(password) {
            liveData.value = !TextUtils.isEmpty(userId.value) && !TextUtils.isEmpty(password.value)
        }
        liveData
    }
    val currentUserList by lazy {
        userDao.loadCurrentUsers().asLiveData()
    }

    fun onLoginButtonClicked(view: View) {
        login()
    }

    fun login() {
        httpService.login(LoginRequest(userId.value!!, password.value!!))
                .enqueue(object : Callback<EmptyResponse> {
                    override fun onFailure(call: Call<EmptyResponse>, t: Throwable) {
                        Log.e(ContentValues.TAG, "login error: $t")
                        Toast.makeText(context, R.string.error_network_exception, Toast.LENGTH_LONG).show()
                    }

                    override fun onResponse(call: Call<EmptyResponse>, response: retrofit2.Response<EmptyResponse>) {
                        Log.d(ContentValues.TAG, "login response: ${response.body()}")
                        if (response.isSuccessful) {
                            userRepository.loggedIn.postValue(true)
                        } else {
                            Toast.makeText(context, R.string.error_invalid_login, Toast.LENGTH_LONG).show()
                        }
                    }
                })
    }
}
