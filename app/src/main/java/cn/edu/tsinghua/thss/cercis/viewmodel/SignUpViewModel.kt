package cn.edu.tsinghua.thss.cercis.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor() : ViewModel() {

    fun signUp() {
//        val signUpNickname = mSignUpNicknameTextInput.editText!!.text.toString()
//        val signUpMobile = mSignUpMobileTextInput.editText!!.text.toString()
//        val signUpEmail = mSignUpEmailTextInput.editText!!.text.toString()
//        val signUpPassword = mSignUpPasswordTextInput.editText!!.text.toString()
//        val signUpPasswordConfirm = mSignUpConfirmPasswordTextInput.editText!!.text.toString()
//
//        // TODO validator
//
//        retrofit.signUp(SignUpRequest(
//                nickname = signUpNickname,
//                mobile = signUpMobile,
//                email = signUpEmail,
//                password = signUpPassword))
//                .enqueue(object : Callback<SignUpResponse> {
//                    override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
//                        Log.e(ContentValues.TAG, "singup error: $t")
//                        Toast.makeText(this@StartupActivity, R.string.error_network_exception, Toast.LENGTH_LONG).show()
//                    }
//
//                    override fun onResponse(call: Call<SignUpResponse>, response: retrofit2.Response<SignUpResponse>) {
//                        Log.d(ContentValues.TAG, "signup response: ${response.body()}")
//                        if (response.isSuccessful) {
//                            Toast.makeText(this@StartupActivity, R.string.success_signup, Toast.LENGTH_LONG).show()
//                            signupSuccessBinding.cercisIdView.text = response.body()?.payload?.userId.toString()
//                            switchToSignUpViewPage2()
//                        } else {
//                            Toast.makeText(this@StartupActivity, R.string.error_invalid_login, Toast.LENGTH_LONG).show()
//                        }
//                    }
//                })
    }
}