package cn.edu.tsinghua.thss.cercis.viewmodel

import android.app.Application
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import cn.edu.tsinghua.thss.cercis.Constants.SEND_CODE_COUNTDOWN
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.dao.UserDao
import cn.edu.tsinghua.thss.cercis.entity.LoginHistory
import cn.edu.tsinghua.thss.cercis.http.CercisHttpService
import cn.edu.tsinghua.thss.cercis.http.MobileSignUpRequest
import cn.edu.tsinghua.thss.cercis.http.SignUpRequest
import cn.edu.tsinghua.thss.cercis.repository.AuthRepository
import cn.edu.tsinghua.thss.cercis.util.LOG_TAG
import cn.edu.tsinghua.thss.cercis.util.NetworkResponse
import cn.edu.tsinghua.thss.cercis.util.PairLiveData
import cn.edu.tsinghua.thss.cercis.util.PasswordChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * View model for sign up fragment.
 * SignUpViewModel is shared across all sign up fragments.
 *
 * Procedure:
 *  Fragment1:
 *      enter mobile
 *      send code
 *      check code
 *  Fragment2:
 *      enter nickname
 *      enter password
 *      enter password confirmation
 *  Fragment3:
 *      show result
 *      add to local users
 *  Back to login
 */
@FlowPreview
@ExperimentalCoroutinesApi
@HiltViewModel
class SignUpViewModel @Inject constructor(
    application: Application,
    private val httpService: CercisHttpService,
    private val authRepository: AuthRepository,
    private val userDao: UserDao,
) : AndroidViewModel(application) {

    enum class NavAction {
        FRAGMENT1, FRAGMENT_SUCCESS, BACK,
    }

    val navAction = MutableLiveData<Pair<NavAction, Long>?>(null)

    // SignUpFragment1

    // hint messages for UI
    val mobile = MutableLiveData("")
    val verificationCode = MutableLiveData("")
    val verificationError = MutableLiveData<String?>(null)
    val verificationCodeCountDown = MutableLiveData(0)
    val canSendCode = Transformations.map(PairLiveData(mobile, verificationCodeCountDown)) {
        it?.let {
            !it.first.isNullOrEmpty() && it.second == 0
        } ?: false
    }
    val countdownText = Transformations.map(verificationCodeCountDown) {
        it?.let { if (it == 0) getString(R.string.signup_send_code) else "${getString(R.string.signup_send_code)}(${it})" }
    }

    // SignUpFragment2
    val nickname = MutableLiveData("")
    val password = MutableLiveData("")
    val passwordVisible = MutableLiveData(false)
    val signUpError = MutableLiveData<String?>(null)
    val signUpSubmittingBusy = MutableLiveData(false)
    val passwordChecker = PasswordChecker(password)
    val canSubmit: LiveData<Boolean> = run {
        MediatorLiveData<Boolean>().apply {
            val checkCanSubmit = { it: Any ->
                value = !mobile.value.isNullOrEmpty()
                        && !verificationCode.value.isNullOrEmpty()
                        && !nickname.value.isNullOrEmpty()
                        && passwordChecker.result.value?.valid ?: false
                        && signUpSubmittingBusy.value != true
            }
            addSource(mobile, checkCanSubmit)
            addSource(verificationCode, checkCanSubmit)
            addSource(nickname, checkCanSubmit)
            addSource(passwordChecker.result, checkCanSubmit)
            addSource(signUpSubmittingBusy, checkCanSubmit)
        }
    }
    val passwordError: LiveData<String?> = Transformations.map(passwordChecker.result) {
        it?.let {
            when {
                it.emptyOrValid -> null
                !it.ruleLength -> getString(R.string.error_password_min_8_max_20)
                !it.ruleAllowedCharacters -> getString(R.string.error_password_invalid_character)
                    .replace("{}", it.invalidCharacter!!)
                else -> getString(R.string.error_password_should_3_out_of_4)
            }
        }
    }

    private suspend fun sendVerificationCode() {
        verificationCodeCountDown.postValue(SEND_CODE_COUNTDOWN)
        try {
            verificationError.postValue(null)
            when (val resp =
                httpService.mobileSignUp(MobileSignUpRequest("+86${mobile.value}"))) {
                is NetworkResponse.Success -> {
                    for (i in SEND_CODE_COUNTDOWN - 1 downTo 0) {
                        delay(1000)
                        verificationCodeCountDown.postValue(i)
                    }
                }
                is NetworkResponse.Reject -> {
                    verificationError.postValue(resp.message)
                    verificationCodeCountDown.postValue(0)
                }
                is NetworkResponse.NetworkError -> {
                    verificationError.postValue(resp.message)
                    verificationCodeCountDown.postValue(0)
                }
            }
        } finally {
            verificationCodeCountDown.postValue(0)
        }
    }


    /**
     * Listener on button for sending verification code.
     */
    fun onSendCodeButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        if (verificationCodeCountDown.value == 0) {
            viewModelScope.launch(Dispatchers.IO) {
                sendVerificationCode()
            }
        }

    }

    /**
     * Listener on button for submit sign up request.
     */
    fun onSubmitButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        viewModelScope.launch(Dispatchers.IO) {
            signUp()
        }
    }

    override fun onCleared() {
//        passwordChecker.clear()
        Log.d(LOG_TAG, "SignUpViewModel destroyed.")
    }

    private suspend fun signUp() {
        // clear error message
        signUpError.postValue(null)
        signUpSubmittingBusy.postValue(true)

        val nickname = this.nickname.value ?: ""
        val mobile = "+86${this.mobile.value}"
        val password = this.password.value ?: ""
        val verificationCode = this.verificationCode.value ?: ""

        try {
            val response = httpService.signUp(SignUpRequest(
                nickname = nickname,
                mobile = mobile,
                password = password,
                verificationCode = verificationCode,
            ))
            Log.d(LOG_TAG, "signup resp: $response")
            when (response) {
                is NetworkResponse.Success -> {
//                    val user = UserDetail(
//                        id = response.data.userId,
//                        nickname = nickname,
//                        mobile = mobile,
//                        avatar = "",
//                        bio = "",
//                    )
//                    userDao.insertCurrentUser(user)
                    LoginHistory(
                        userId = response.data.userId,
                        mobile = mobile,
                    ).also {
                        userDao.insertLoginHistory(it)
                        authRepository.userId = it.id
                        navAction.postValue(NavAction.FRAGMENT_SUCCESS to it.id)
                    }
                }
                is NetworkResponse.NetworkError -> signUpError.postValue(response.message)
                is NetworkResponse.Reject -> {
                    signUpError.postValue(response.message)
                }
            }
        } finally {
            signUpSubmittingBusy.postValue(false)
        }
    }

    private fun getString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }
}
