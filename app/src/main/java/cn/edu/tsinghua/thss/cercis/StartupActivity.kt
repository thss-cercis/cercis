package cn.edu.tsinghua.thss.cercis

import android.animation.LayoutTransition
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import cn.edu.tsinghua.thss.cercis.Constants.URL_BASE
import cn.edu.tsinghua.thss.cercis.api.*
import cn.edu.tsinghua.thss.cercis.databinding.*
import cn.edu.tsinghua.thss.cercis.util.PreferencesHelper
import cn.edu.tsinghua.thss.cercis.viewmodel.LoginViewModel
import cn.edu.tsinghua.thss.cercis.viewmodel.UserViewModel
import com.dd.processbutton.FlatButton
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class StartupActivity : AppCompatActivity() {

    private val userViewModel: UserViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (userViewModel.loggedIn.value == true) {
            switchToMainActivity()
            return
        }

        // splash view
        splashViewBinding = binding.splashView
        if (!PreferencesHelper.Auth.isFirstStartup(this)) {
            switchToLoginView()
        } else {
            PreferencesHelper.Auth.setFirstStartup(this, false)
        }

        // login view
        loginViewBinding = binding.loginView
        enableTransition(loginViewBinding.loginMainArea)
        mLoginUserIdTextInput = loginViewBinding.loginUserId
        mLoginUserIdAutoCompleteTextView = loginViewBinding.loginUserIdList
        mLoginPasswordTextInput = loginViewBinding.signupPassword
        mLoginSubmitButton = loginViewBinding.loginSubmit
        mLoginUserIdAutoCompleteTextView.setAdapter(ArrayAdapter(this, R.layout.list_item, emptyList<String>()))
        mLoginSubmitButton.setOnClickListener { loginSubmit() }

        // signup view
        signupViewBinding = binding.signupView
        enableTransition(signupViewBinding.signupMainArea)
        mSignUpNicknameTextInput = signupViewBinding.signupNickname
        mSignUpMobileTextInput = signupViewBinding.signupMobile
        mSignUpEmailTextInput = signupViewBinding.signupEmail
        mSignUpPasswordTextInput = signupViewBinding.signupPassword
        mSignUpConfirmPasswordTextInput = signupViewBinding.signupConfirmPassword
        mSignUpSubmitButton = signupViewBinding.signupSubmit
        mBackButton = signupViewBinding.back
        mSignUpSubmitButton.setOnClickListener { signupSubmit() }
        mBackButton.setOnClickListener {
            switchToLoginView()
        }

        // signup success
        signupSuccessBinding = signupViewBinding.signupSuccessView
        signupSuccessBinding.signupSuccessSubmit.setOnClickListener {
            loginViewBinding.loginUserIdList.setText(signupSuccessBinding.cercisIdView.text)
            switchToLoginView()
        }

        // bottom
        mResetPasswordLink = binding.loginView.linkResetPassword
        mSignUpLink = binding.loginView.linkUserSignup
        mSignUpLink.setOnClickListener { switchToSignUpViewPage1() }

        // okHttp

        val cookieManager = CookieManager()
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .cookieJar(JavaNetCookieJar(cookieManager))
                .build()
        retrofit = Retrofit.Builder()
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .baseUrl(URL_BASE)
                .build()
                .create(CercisHttpService::class.java)
    }

    /**
     * Enables transition animation for a given view.
     *
     * @param view the view
     */
    private fun enableTransition(view: ViewGroup) {
        val transition = view.layoutTransition
        if (transition == null) {
            val newTransition = LayoutTransition()
            newTransition.enableTransitionType(LayoutTransition.CHANGING)
            view.layoutTransition = newTransition
        } else {
            view.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        }
    }

    private fun switchToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
    
    /**
     * Checks text fields and do the login post.
     */
    private fun loginSubmit() {
        val loginId: CharSequence? = mLoginUserIdTextInput.editText?.text
        val loginPassword: CharSequence? = mLoginPasswordTextInput.editText?.text
        if (loginId == null || loginPassword == null || loginId.isEmpty() || loginPassword.isEmpty()) {
            // TODO validator
            return
        }

        retrofit.login(LoginRequest(loginId.toString(), loginPassword.toString()))
                .enqueue(object : Callback<EmptyResponse> {
                    override fun onFailure(call: Call<EmptyResponse>, t: Throwable) {
                        // mLoginPassword.error = getString(R.string.error_network_exception)
                        Log.e(TAG, "login error: $t")
                        Toast.makeText(this@StartupActivity, R.string.error_network_exception, Toast.LENGTH_LONG).show()
                    }

                    override fun onResponse(call: Call<EmptyResponse>, response: retrofit2.Response<EmptyResponse>) {
                        Log.d(TAG, "login response: ${response.body()}")
                        if (response.isSuccessful) {
                            userViewModel.loggedIn.postValue(false)
                        } else {
                            Toast.makeText(this@StartupActivity, R.string.error_invalid_login, Toast.LENGTH_LONG).show()
                        }
                    }
                })
    }

    private fun signupSubmit() {
        val signUpNickname = mSignUpNicknameTextInput.editText!!.text.toString()
        val signUpMobile = mSignUpMobileTextInput.editText!!.text.toString()
        val signUpEmail = mSignUpEmailTextInput.editText!!.text.toString()
        val signUpPassword = mSignUpPasswordTextInput.editText!!.text.toString()
        val signUpPasswordConfirm = mSignUpConfirmPasswordTextInput.editText!!.text.toString()

        // TODO validator

        retrofit.signUp(SignUpRequest(
                nickname = signUpNickname,
                mobile = signUpMobile,
                email = signUpEmail,
                password = signUpPassword))
                .enqueue(object : Callback<SignUpResponse> {
                    override fun onFailure(call: Call<SignUpResponse>, t: Throwable) {
                        Log.e(TAG, "singup error: $t")
                        Toast.makeText(this@StartupActivity, R.string.error_network_exception, Toast.LENGTH_LONG).show()
                    }

                    override fun onResponse(call: Call<SignUpResponse>, response: retrofit2.Response<SignUpResponse>) {
                        Log.d(TAG, "signup response: ${response.body()}")
                        if (response.isSuccessful) {
                            Toast.makeText(this@StartupActivity, R.string.success_signup, Toast.LENGTH_LONG).show()
                            signupSuccessBinding.cercisIdView.text = response.body()?.payload?.userId.toString()
                            switchToSignUpViewPage2()
                        } else {
                            Toast.makeText(this@StartupActivity, R.string.error_invalid_login, Toast.LENGTH_LONG).show()
                        }
                    }
                })
    }

    private fun switchToSplashView() {
        binding.flipper.displayedChild = SPLASH_VIEW
    }

    private fun switchToLoginView() {
        binding.flipper.displayedChild = LOGIN_VIEW
    }

    private fun switchToSignUpViewPage1() {
        binding.flipper.displayedChild = SIGNUP_VIEW
        binding.signupView.signupFlipper.displayedChild = SIGNUP_VIEW_P1
    }

    private fun switchToSignUpViewPage2() {
        binding.flipper.displayedChild = SIGNUP_VIEW
        binding.signupView.signupFlipper.displayedChild = SIGNUP_VIEW_P2
    }

    private lateinit var binding: ActivityStartupBinding
    // view flipper to switch between different views

    // splash view
    private lateinit var splashViewBinding: LayoutSplashBinding

    // login view
    private lateinit var loginViewBinding: LayoutLoginBinding
    private lateinit var mLoginUserIdTextInput: TextInputLayout
    private lateinit var mLoginUserIdAutoCompleteTextView: AutoCompleteTextView
    private lateinit var mLoginPasswordTextInput: TextInputLayout
    private lateinit var mLoginSubmitButton: MaterialButton

    // signup view
    private lateinit var signupViewBinding: LayoutSignupBinding
    private lateinit var mSignUpNicknameTextInput: TextInputLayout
    private lateinit var mSignUpMobileTextInput: TextInputLayout
    private lateinit var mSignUpEmailTextInput: TextInputLayout
    private lateinit var mSignUpPasswordTextInput: TextInputLayout
    private lateinit var mSignUpConfirmPasswordTextInput: TextInputLayout
    private lateinit var mSignUpSubmitButton: FlatButton
    private lateinit var mBackButton: ImageButton

    private lateinit var signupSuccessBinding: LayoutSignupSuccessBinding

    // bottom
    private lateinit var mSignUpLink: MaterialTextView
    private lateinit var mResetPasswordLink: MaterialTextView

    // retrofit
    private lateinit var retrofit: CercisHttpService

    companion object {
        private const val SPLASH_VIEW = 0
        private const val LOGIN_VIEW = 1
        private const val SIGNUP_VIEW = 2
        private const val SIGNUP_VIEW_P1 = 0
        private const val SIGNUP_VIEW_P2 = 1
    }
}