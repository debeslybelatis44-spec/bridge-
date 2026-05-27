
package com.lotato.pro.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.lotato.pro.R;
import com.lotato.pro.api.ApiService;
import com.lotato.pro.models.AppSession;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnAgent, btnSupervisor, btnOwner;
    private ProgressBar progressBar;
    private TextView tvError;
    private String selectedRole = "agent";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppSession session = AppSession.getInstance(this);
        if (session.isLoggedIn()) {
            startMain();
            return;
        }

        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnAgent = findViewById(R.id.btnRoleAgent);
        btnSupervisor = findViewById(R.id.btnRoleSupervisor);
        btnOwner = findViewById(R.id.btnRoleOwner);
        progressBar = findViewById(R.id.progressBar);
        tvError = findViewById(R.id.tvError);

        selectRole("agent");

        btnAgent.setOnClickListener(v -> selectRole("agent"));
        btnSupervisor.setOnClickListener(v -> selectRole("supervisor"));
        btnOwner.setOnClickListener(v -> selectRole("owner"));

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void selectRole(String role) {
        selectedRole = role;
        int activeColor = getResources().getColor(R.color.primary);
        int inactiveColor = getResources().getColor(R.color.surface);
        int activeText = getResources().getColor(R.color.white);
        int inactiveText = getResources().getColor(R.color.text_dim);

        btnAgent.setBackgroundColor(role.equals("agent") ? activeColor : inactiveColor);
        btnSupervisor.setBackgroundColor(role.equals("supervisor") ? activeColor : inactiveColor);
        btnOwner.setBackgroundColor(role.equals("owner") ? activeColor : inactiveColor);
        btnAgent.setTextColor(role.equals("agent") ? activeText : inactiveText);
        btnSupervisor.setTextColor(role.equals("supervisor") ? activeText : inactiveText);
        btnOwner.setTextColor(role.equals("owner") ? activeText : inactiveText);
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Ranpli tout chan yo");
            return;
        }

        setLoading(true);
        tvError.setVisibility(View.GONE);

        ApiService.getInstance().login(username, password, selectedRole, new ApiService.Callback<JsonObject>() {
            @Override
            public void onSuccess(JsonObject result) {
                setLoading(false);
                String token = result.has("token") ? result.get("token").getAsString() : null;
                String name = result.has("name") ? result.get("name").getAsString() : username;
                String role = result.has("role") ? result.get("role").getAsString() : selectedRole;
                String agentId = null;
                String ownerId = null;

                if (result.has("agentId") && !result.get("agentId").isJsonNull()) {
                    agentId = result.get("agentId").getAsString();
                }
                if (result.has("ownerId") && !result.get("ownerId").isJsonNull()) {
                    ownerId = result.get("ownerId").getAsString();
                }
                if (agentId == null && result.has("id")) {
                    agentId = result.get("id").getAsString();
                }

                AppSession.getInstance(LoginActivity.this)
                    .saveLogin(token, agentId, name, ownerId, role);
                ApiService.getInstance().setAuthToken(token);
                startMain();
            }

            @Override
            public void onError(String error) {
                setLoading(false);
                showError(error);
            }
        });
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Connexion..." : getString(R.string.login_btn));
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
