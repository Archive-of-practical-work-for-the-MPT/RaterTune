package com.example.ratertune.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.ratertune.R;
import com.example.ratertune.utils.SessionManager;
import com.example.ratertune.utils.Config;
import com.example.ratertune.api.SupabaseClient;

import java.util.Objects;

public class AuthActivity extends AppCompatActivity {
    private TextInputLayout emailLayout, passwordLayout;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton;
    private TextView registerButton, authTitleText, promptText;
    private boolean isLoginMode = true;
    private SessionManager sessionManager;
    private View progressOverlay;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Инициализируем Config для загрузки переменных из .env.properties
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
        rootView = findViewById(android.R.id.content);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = (TextInputEditText) emailLayout.getEditText();
        passwordInput = (TextInputEditText) passwordLayout.getEditText();
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        authTitleText = findViewById(R.id.authTitleText);
        promptText = findViewById(R.id.promptText);
        progressOverlay = findViewById(R.id.progressOverlay);

        // Устанавливаем обработчики событий
        loginButton.setOnClickListener(v -> handleAuth());
        registerButton.setOnClickListener(v -> toggleAuthMode());
    }

    private void toggleAuthMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            authTitleText.setText(R.string.login_title);
            loginButton.setText(R.string.login_button);
            registerButton.setText(R.string.register_link);
            promptText.setText(R.string.no_account);
        } else {
            authTitleText.setText(R.string.register_title);
            loginButton.setText(R.string.register_button);
            registerButton.setText(R.string.login_link);
            promptText.setText(R.string.have_account);
        }
    }

    private void handleAuth() {
        String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
        String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();

        // Проверка введенных данных
        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.error_empty_email));
            return;
        }
        emailLayout.setError(null);

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.error_empty_password));
            return;
        }
        if (password.length() < 6) {
            passwordLayout.setError(getString(R.string.error_short_password));
            return;
        }
        passwordLayout.setError(null);

        // Показываем индикатор загрузки
        showLoading(true);
        
        // Реализация авторизации через Supabase
        if (isLoginMode) {
            // Выполняем вход
            SupabaseClient.getInstance().signIn(email, password, new SupabaseClient.AuthCallback() {
                @Override
                public void onSuccess(SupabaseClient.AuthResponse response) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        // Сохраняем данные сессии
                        sessionManager.createLoginSession(
                            response.getUser().getId(),
                            response.getUser().getEmail(),
                            response.getUser().getName(),
                            response.getAccessToken(),
                            response.getRefreshToken(),
                            response.getUser().getAvatarUrl()
                        );
                        // Переходим на главный экран
                        startActivity(new Intent(AuthActivity.this, MainActivity.class));
                        finish();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showAuthError(errorMessage, true);
                    });
                }
            });
        } else {
            // Выполняем регистрацию
            SupabaseClient.getInstance().signUp(email, password, new SupabaseClient.AuthCallback() {
                @Override
                public void onSuccess(SupabaseClient.AuthResponse response) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showRegistrationSuccess();
                        // После успешной регистрации переключаемся на экран входа
                        isLoginMode = true;
                        authTitleText.setText(R.string.login_title);
                        loginButton.setText(R.string.login_button);
                        registerButton.setText(R.string.register_link);
                        promptText.setText(R.string.no_account);
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showAuthError(errorMessage, false);
                    });
                }
            });
        }
    }
    
    /**
     * Показывает сообщение об ошибке авторизации с пользовательски понятным описанием
     * @param errorMessage Техническое сообщение об ошибке
     * @param isLogin Флаг, указывающий, произошла ли ошибка при входе или регистрации
     */
    private void showAuthError(String errorMessage, boolean isLogin) {
        String userFriendlyMessage;
        
        // Анализируем сообщение об ошибке и формируем понятное пользователю сообщение
        if (errorMessage.contains("Configuration error")) {
            userFriendlyMessage = "Не удалось подключиться к серверу. Пожалуйста, сообщите разработчикам об этой проблеме.";
        } 
        else if (errorMessage.contains("Network error") || errorMessage.contains("timeout")) {
            userFriendlyMessage = "Проверьте подключение к интернету и попробуйте снова.";
        }
        else if (errorMessage.contains("invalid_grant") || errorMessage.contains("Invalid login")) {
            userFriendlyMessage = "Неверный email или пароль. Попробуйте снова.";
        }
        else if (errorMessage.contains("User already registered")) {
            userFriendlyMessage = "Пользователь с таким email уже зарегистрирован. Попробуйте войти.";
        }
        else if (errorMessage.contains("Invalid email")) {
            userFriendlyMessage = "Указан неверный формат email. Проверьте и попробуйте снова.";
        }
        else if (errorMessage.contains("weak password")) {
            userFriendlyMessage = "Пароль слишком простой. Используйте комбинацию букв, цифр и специальных символов.";
        }
        else {
            // Общее сообщение, если не удалось определить конкретную причину
            userFriendlyMessage = isLogin 
                ? "Не удалось войти в аккаунт. Пожалуйста, проверьте введенные данные и попробуйте снова." 
                : "Не удалось создать аккаунт. Пожалуйста, попробуйте позже или используйте другой email.";
        }
        
        // Создаем диалоговое окно с ошибкой
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isLogin ? "Ошибка входа" : "Ошибка регистрации")
               .setMessage(userFriendlyMessage)
               .setPositiveButton("Понятно", null)
               .show();
    }
    
    // Показывает сообщение об успешной регистрации
    private void showRegistrationSuccess() {
        Snackbar.make(rootView, "Регистрация успешна! Теперь вы можете войти, используя созданный аккаунт.", 
                     Snackbar.LENGTH_LONG)
                .setBackgroundTint(getResources().getColor(R.color.accent_dark, null))
                .setTextColor(getResources().getColor(R.color.text_primary, null))
                .show();
    }
    
    // Показывает или скрывает индикатор загрузки
    private void showLoading(boolean isLoading) {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Блокируем кнопки во время загрузки
        loginButton.setEnabled(!isLoading);
        registerButton.setEnabled(!isLoading);
    }
} 