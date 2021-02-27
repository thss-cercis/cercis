package cn.edu.tsinghua.thss.cercis

import android.animation.LayoutTransition
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.common.collect.ImmutableMap
import org.json.JSONObject

class StartupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)
        mViewFlipper = findViewById(R.id.flipper)

        // login view
        enableTransition(findViewById<View>(R.id.login_main_area) as ViewGroup)
        mLoginUserId = findViewById(R.id.login_user_id)
        mLoginUserIdTextView = findViewById(R.id.login_user_id_list)
        mLoginPassword = findViewById(R.id.login_password)
        mLoginSubmitButton = findViewById(R.id.login_submit)
        mLoginUserIdTextView.setAdapter(ArrayAdapter(this, R.layout.list_item, emptyList<String>()))
        mLoginSubmitButton.setOnClickListener { loginSubmit() }
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

    // view flipper to switch between different views
    private var mViewFlipper: ViewFlipper? = null
    // ==========
    // Login view
    // ==========
    /**
     * Checks text fields and do the login post.
     */
    private fun loginSubmit() {
        val loginId: CharSequence? = if (mLoginUserId!!.editText != null) mLoginUserId!!.editText!!.text else null
        val loginPassword: CharSequence? = if (mLoginPassword!!.editText != null) mLoginPassword!!.editText!!.text else null
        if (loginId == null || loginPassword == null || loginId.length == 0 || loginPassword.length == 0) {
            // invalid login
            return
        }
        val requestQueue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(Request.Method.POST, Constants.URL_AUTH_LOGIN, JSONObject(ImmutableMap.of(
                "id", loginId.toString(),
                "password", loginPassword.toString()
        ) as Map<*, *>), Response.Listener { response: JSONObject ->
            Toast.makeText(this, R.string.success_login, Toast.LENGTH_LONG).show()
            Log.i("login_view", "login response: $response")
            startActivity(Intent(this, MainActivity::class.java))
        }, Response.ErrorListener { error: VolleyError ->
            mLoginPassword!!.error = getString(R.string.error_network_exception)
            Log.e("login_view", "login error: $error")
        })
        requestQueue.add(request)
    }

    private lateinit var mLoginUserId: TextInputLayout
    private lateinit var mLoginUserIdTextView: AutoCompleteTextView
    private lateinit var mLoginPassword: TextInputLayout
    private lateinit var mLoginSubmitButton: MaterialButton
}