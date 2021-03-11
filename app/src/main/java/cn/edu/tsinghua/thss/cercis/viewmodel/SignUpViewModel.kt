package cn.edu.tsinghua.thss.cercis.viewmodel

import android.content.ContentValues.TAG
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import cn.edu.tsinghua.thss.cercis.R
import cn.edu.tsinghua.thss.cercis.api.*
import cn.edu.tsinghua.thss.cercis.dao.CurrentUser
import cn.edu.tsinghua.thss.cercis.repository.UserRepository
import cn.edu.tsinghua.thss.cercis.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.jvm.Throws

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
 *      enter email (optional)
 *          check email used
 *      enter password
 *      enter password confirmation
 *  Fragment3:
 *      show result
 *      add to local users
 *  Back to login
 */
@HiltViewModel
class SignUpViewModel @Inject constructor(
        private val userRepository: UserRepository,
) : ViewModel() {

    enum class CodeMode {
        MOBILE, EMAIL,
    }

    enum class NavAction {
        FRAGMENT1, FRAGMENT2, FRAGMENT_SUCCESS, LOGIN,
    }

    val navAction = MutableLiveData(NavAction.FRAGMENT1)

    // SignUpFragment1
    // use mobile or email to signup
    val mode = MutableLiveData(CodeMode.MOBILE)

    // hint messages for UI
    val modeHeadline = MutableLiveData(getString(R.string.signup_send_mobile_code))
    val fieldHint = MutableLiveData(getString(R.string.signup_hint_mobile_code))
    val switchMessage = MutableLiveData(getString(R.string.signup_switch_to_email_mode))

    val emailOrMobile = MutableLiveData("")
    val verificationCode = MutableLiveData("")
    val verificationError = MutableLiveData<String?>(null)
    val verificationCodeSent = MutableLiveData(false)
    val verificationCodeCountDown = MutableLiveData(45)

    // SignUpFragment2
    val nickname = MutableLiveData("")
    val password = MutableLiveData("")
    val signUpError = MutableLiveData<String?>(null)
    val signUpSubmittingBusy = MutableLiveData(false)
    val passwordChecker = PasswordChecker(password)
    val canSubmit: LiveData<Boolean> = run {
        Transformations.map(TripleLiveData(signUpSubmittingBusy, nickname, passwordChecker.valid)) {
            it.first != true && !it.second.isNullOrEmpty() && it.third != false
        }
    }
    val passwordError: LiveData<String?> = Transformations.map(passwordChecker.valid) {
        if (it != false) null else getString(R.string.error_password_invalid)
    }

    // SignUpFragment3
    val newUserId: MutableLiveData<UserId> = MutableLiveData(-1)

    fun sendVerificationCode() {
        // TODO fill with actual logic
        verificationError.postValue(null)
        verificationCodeSent.postValue(true)
    }


    /**
     * Listener on text view for switching how verification code should be sent.
     */
    fun onSwitchViewClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        switchMode(when (mode.value) {
            CodeMode.EMAIL -> CodeMode.MOBILE
            CodeMode.MOBILE -> CodeMode.EMAIL
            else -> CodeMode.MOBILE
        })
    }

    /**
     * Listener on button for sending verification code.
     */
    fun onSendCodeButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        // TODO not implemented yet

    }

    /**
     * Listener on button for confirming verification code.
     */
    fun onFragment1NextButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val successful = checkVerificationCodeCorrectness()
                if (successful) {
                    navAction.postValue(NavAction.FRAGMENT2)
                }
            } catch (ex: DataFetchException) {
                verificationError.postValue(ex.message)
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

    /**
     * Listener on button for go back to login.
     */
    fun onBackToLoginButtonClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        navAction.postValue(NavAction.LOGIN)
    }

    override fun onCleared() {
        passwordChecker.clear()
        Log.d(TAG, "SignUpViewModel destroyed.")
    }

    @Throws(DataFetchException::class)
    private suspend fun checkVerificationCodeCorrectness(): Boolean {
        return true
        @Suppress("UNREACHABLE_CODE")
        viewModelScope.run {
            try {
                val response = userRepository.httpService.mobileSignUpCheck(MobileSignUpCheckRequest(
                        code = verificationCode.value ?: ""
                ))
                if (response.successful && response.payload != null) {
                    return response.payload.ok
                }
                throw DataFetchException(response.msg)
            } catch (t: Throwable) {
                throw DataFetchException(userRepository.context.getString(R.string.error_network_exception))
            }
        }
    }

    private suspend fun signUp(): Boolean {
        // clear error message
        signUpError.postValue(null)
        signUpSubmittingBusy.postValue(true)

        val nickname = this.nickname.value ?: ""
        val mobile: String = when (this.mode.value) {
            CodeMode.MOBILE -> this.emailOrMobile.value ?: ""
            CodeMode.EMAIL -> ""
            else -> ""
        }
        val email: String = when (this.mode.value) {
            CodeMode.MOBILE -> ""
            CodeMode.EMAIL -> this.emailOrMobile.value ?: ""
            else -> ""
        }
        val password = this.password.value ?: ""
        val verificationCode = this.verificationCode.value ?: ""

        try {
            val response = userRepository.httpService.signUp(SignUpRequest(
                    nickname = nickname,
                    mobile = mobile,
                    email = email,
                    password = password,
                    verificationCode = verificationCode,
            ))
            Log.d(TAG, "signup resp: $response")
            if (response.successful && response.payload != null) {
                val user = CurrentUser(
                        id = response.payload.userId,
                        nickname = nickname,
                        mobile = mobile,
                        email = email,
                        avatar = "",
                        bio = "",
                )
                userRepository.userDao.insertCurrentUser(user)
                userRepository.currentUserId.postValue(user.id)
                newUserId.postValue(user.id)
                navAction.postValue(NavAction.FRAGMENT_SUCCESS)
            } else {
                signUpError.postValue(response.msg)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "signup error: $t")
            signUpError.postValue(userRepository.context.getString(R.string.error_network_exception))
        } finally {
            signUpSubmittingBusy.postValue(false)
        }
        return false
    }

    private fun switchMode(mode: CodeMode) {
        this.mode.postValue(mode)
        this.modeHeadline.postValue(when (mode) {
            CodeMode.MOBILE -> getString(R.string.signup_send_mobile_code)
            CodeMode.EMAIL -> getString(R.string.signup_send_email_code)
        })
        this.fieldHint.postValue(when (mode) {
            CodeMode.MOBILE -> getString(R.string.signup_hint_mobile_code)
            CodeMode.EMAIL -> getString(R.string.signup_hint_email_code)
        })
        this.switchMessage.postValue(when (mode) {
            CodeMode.MOBILE -> getString(R.string.signup_switch_to_email_mode)
            CodeMode.EMAIL -> getString(R.string.signup_switch_to_mobile_mode)
        })
        this.emailOrMobile.postValue("")
        this.verificationCode.postValue("")
        this.verificationError.postValue(null)
        this.verificationCodeSent.postValue(false)
    }

    private fun getString(resId: Int): String {
        return userRepository.context.getString(resId)
    }
}