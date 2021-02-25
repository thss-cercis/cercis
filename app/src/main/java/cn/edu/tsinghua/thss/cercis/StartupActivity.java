package cn.edu.tsinghua.thss.cercis;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.animation.LayoutTransition;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.collect.ImmutableMap;

import org.json.JSONObject;

import java.util.Collections;

public class StartupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        mViewFlipper = findViewById(R.id.flipper);

        // login view
        enableTransition((ViewGroup) findViewById(R.id.login_main_area));
        mLoginUserId = findViewById(R.id.login_user_id);
        mLoginUserIdTextView = findViewById(R.id.login_user_id_list);
        mLoginPassword = findViewById(R.id.login_password);
        mLoginSubmitButton = findViewById(R.id.login_submit);

        mLoginUserIdTextView.setAdapter(new ArrayAdapter<>(this, R.layout.list_item, Collections.<String>emptyList()));
        mLoginSubmitButton.setOnClickListener(event -> loginSubmit());
    }

    /**
     * Enables transition animation for a given view.
     *
     * @param view the view
     */
    private void enableTransition(ViewGroup view) {
        LayoutTransition transition = view.getLayoutTransition();
        if (transition == null) {
            LayoutTransition newTransition = new LayoutTransition();
            newTransition.enableTransitionType(LayoutTransition.CHANGING);
            view.setLayoutTransition(newTransition);
        } else {
            view.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        }
    }

    // view flipper to switch between different views
    private ViewFlipper mViewFlipper;

    // ==========
    // Login view
    // ==========

    /**
     * Checks text fields and do the login post.
     */
    private void loginSubmit() {
        @Nullable CharSequence loginId = mLoginUserId.getEditText() != null ? mLoginUserId.getEditText().getText() : null;
        @Nullable CharSequence loginPassword = mLoginPassword.getEditText() != null ? mLoginPassword.getEditText().getText() : null;
        if (loginId == null || loginPassword == null || loginId.length() == 0 || loginPassword.length() == 0) {
            // invalid login
            return;
        }
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, Constants.URL_AUTH_LOGIN, new JSONObject(ImmutableMap.of(
                "id", loginId.toString(),
                "password", loginPassword.toString()
        )), response -> {
            Toast.makeText(this, R.string.success_login, Toast.LENGTH_LONG).show();
            Log.i("login_view", "login response: " + response.toString());
        }, error -> {
            mLoginPassword.setError(getString(R.string.error_network_exception));
            Log.e("login_view", "login error: " + error.toString());
        });
        requestQueue.add(request);
    }

    private TextInputLayout mLoginUserId;
    private AutoCompleteTextView mLoginUserIdTextView;
    private TextInputLayout mLoginPassword;
    private MaterialButton mLoginSubmitButton;

}