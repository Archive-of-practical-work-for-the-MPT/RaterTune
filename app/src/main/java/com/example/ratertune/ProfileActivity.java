package com.example.ratertune;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ratertune.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {
    private ImageButton backButton;
    private TextView userNameText;
    private TextView userEmailText;
    private TextView reviewsCountText;
    private TextView averageRatingText;
    private Button logoutButton;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        
        // Инициализация компонентов
        backButton = findViewById(R.id.backButton);
        userNameText = findViewById(R.id.userNameText);
        userEmailText = findViewById(R.id.userEmailText);
        reviewsCountText = findViewById(R.id.reviewsCountText);
        averageRatingText = findViewById(R.id.averageRatingText);
        logoutButton = findViewById(R.id.logoutButton);

        // Загрузка данных пользователя
        loadUserData();

        // Обработчики нажатий
        backButton.setOnClickListener(v -> finish());

        logoutButton.setOnClickListener(v -> {
            sessionManager.logout();
            // Возвращаемся на экран авторизации
            Intent intent = new Intent(this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserData() {
        // Загрузка данных из сессии
        String userName = sessionManager.getUserName();
        String userEmail = sessionManager.getUserEmail();

        // Отображение данных
        userNameText.setText(userName);
        userEmailText.setText(userEmail);

        // TODO: Загрузка статистики из Supabase
        reviewsCountText.setText("0");
        averageRatingText.setText("0.0");
    }
} 