package com.example.ratertune;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.ratertune.utils.SessionManager;

public class AuthActivity extends AppCompatActivity {
    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton;
    private TextView registerButton, authTitleText, promptText;
    private boolean isLoginMode = true;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Инициализируем Config для загрузки переменных из .env
        Config.init(this);
        
        sessionManager = new SessionManager(this);
        
        // Проверяем, есть ли активная сессия
        if (sessionManager.isLoggedIn()) {
            // Если есть, переходим на главный экран
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_auth);

        // Инициализация элементов UI
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = (TextInputEditText) emailLayout.getEditText();
        passwordInput = (TextInputEditText) passwordLayout.getEditText();
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        authTitleText = findViewById(R.id.authTitleText);
        promptText = findViewById(R.id.promptText);

        // Устанавливаем обработчики событий
        loginButton.setOnClickListener(v -> handleAuth());
        registerButton.setOnClickListener(v -> toggleAuthMode());
    }

    private void toggleAuthMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            authTitleText.setText(R.string.login_title);
            loginButton.setText(R.string.login_button);
            promptText.setText(R.string.no_account);
            registerButton.setText(R.string.register_link);
        } else {
            authTitleText.setText(R.string.register_title);
            loginButton.setText(R.string.register_button);
            promptText.setText(R.string.have_account);
            registerButton.setText(R.string.login_link);
        }
    }

    private void handleAuth() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Валидация полей
        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.error_empty_email));
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.error_empty_password));
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError(getString(R.string.error_short_password));
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (!isValid) {
            return;
        }

        // Показываем прогресс
        showProgress(true);

        if (isLoginMode) {
            // Вход
            SupabaseClient.getInstance().signIn(email, password, new SupabaseClient.AuthCallback() {
                @Override
                public void onSuccess() {
                    showProgress(false);
                    sessionManager.createLoginSession("user123", email, "User Name", "access_token", "refresh_token");
                    navigateToMain();
                }

                @Override
                public void onError(String errorMessage) {
                    showProgress(false);
                    showToast(getString(R.string.login_error, errorMessage));
                }
            });
        } else {
            // Регистрация
            SupabaseClient.getInstance().signUp(email, password, new SupabaseClient.AuthCallback() {
                @Override
                public void onSuccess() {
                    showProgress(false);
                    showToast(getString(R.string.registration_success));
                    isLoginMode = true;
                    authTitleText.setText(R.string.login_title);
                    loginButton.setText(R.string.login_button);
                    promptText.setText(R.string.no_account);
                    registerButton.setText(R.string.register_link);
                }

                @Override
                public void onError(String errorMessage) {
                    showProgress(false);
                    showToast(getString(R.string.register_error, errorMessage));
                }
            });
        }
    }

    private void showProgress(boolean show) {
        loginButton.setEnabled(!show);
        registerButton.setEnabled(!show);
        
        // Здесь можно добавить анимацию загрузки, если нужно
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
} 