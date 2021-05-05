package cn.cercis.viewmodel

import android.app.Application
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import cn.cercis.Constants.SEND_CODE_COUNTDOWN
import cn.cercis.R
import cn.cercis.common.LOG_TAG
import cn.cercis.dao.LoginHistoryDao
import cn.cercis.entity.LoginHistory
import cn.cercis.repository.AuthRepository
import cn.cercis.util.NetworkResponse
import cn.cercis.util.PairLiveData
import cn.cercis.util.validation.validatePassword
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * View model for sign up fragment.
 * SignUpViewModel is shared across all sign up fragments.
 *
 * Procedure:
 *  Fragment:
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
    private val authRepository: AuthRepository,
    private val loginHistoryDao: LoginHistoryDao,
) : AndroidViewModel(application) {

    enum class NavAction {
        FRAGMENT, FRAGMENT_SUCCESS, BACK,
    }

    val navAction = MutableLiveData<Pair<NavAction, Long>?>(null)

    // SignUpFragment

    // hint messages for UI
    val mobile = MutableLiveData("")
    val verificationCode = MutableLiveData("")
    val verificationError = MutableLiveData<String?>(null)
    private val verificationCodeCountDown = MutableLiveData(0)
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
    private val signUpSubmittingBusy = MutableLiveData(false)
    private val passwordValid = validatePassword(password)
    val canSubmit: LiveData<Boolean> = run {
        MediatorLiveData<Boolean>().apply {
            val checkCanSubmit = { _: Any ->
                value = !mobile.value.isNullOrEmpty()
                        && !verificationCode.value.isNullOrEmpty()
                        && !nickname.value.isNullOrEmpty()
                        && passwordValid.value?.valid ?: false
                        && signUpSubmittingBusy.value != true
            }
            addSource(mobile, checkCanSubmit)
            addSource(verificationCode, checkCanSubmit)
            addSource(nickname, checkCanSubmit)
            addSource(passwordValid, checkCanSubmit)
            addSource(signUpSubmittingBusy, checkCanSubmit)
        }
    }
    val passwordError: LiveData<String?> = Transformations.map(passwordValid) {
        it?.let {
            when {
                it.emptyOrValid -> null
                !it.ruleLength -> getString(R.string.signup_error_password_min_8_max_20)
                !it.ruleAllowedCharacters -> getString(R.string.signup_error_password_invalid_character)
                    .replace("{}", it.invalidCharacter!!)
                else -> getString(R.string.signup_error_password_should_3_out_of_4)
            }
        }
    }

    private suspend fun sendVerificationCode() {
        verificationCodeCountDown.postValue(SEND_CODE_COUNTDOWN)
        try {
            verificationError.postValue(null)
            when (val resp = authRepository.sendSignUpSms("+86${mobile.value}")) {
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
            val response = authRepository.signUp(
                nickname = nickname,
                mobile = mobile,
                password = password,
                verificationCode = verificationCode,
            )
            Log.d(LOG_TAG, "sign up resp: $response")
            when (response) {
                is NetworkResponse.Success -> {
                    val userId = response.data.id
                    val loginHistory = LoginHistory(
                        userId = userId,
                        mobile = mobile,
//                        nickname = nickname,
//                        avatar = "",
                    )
                    loginHistoryDao.saveLoginHistory(loginHistory)
                    navAction.postValue(NavAction.FRAGMENT_SUCCESS to userId)
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
