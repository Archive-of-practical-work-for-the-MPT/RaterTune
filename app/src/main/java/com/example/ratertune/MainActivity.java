package com.example.ratertune;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.ratertune.adapter.ReleasesPagerAdapter;
import com.example.ratertune.model.Release;
import com.example.ratertune.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 releasesPager;
    private ImageButton profileButton;
    private ImageButton addReleaseButton;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        
        // Проверяем, есть ли активная сессия
        if (!sessionManager.isLoggedIn()) {
            // Если нет, переходим на экран авторизации
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Инициализация компонентов
        releasesPager = findViewById(R.id.releasesPager);
        profileButton = findViewById(R.id.profileButton);
        addReleaseButton = findViewById(R.id.addReleaseButton);

        // Настройка ViewPager
        List<Release> releases = new ArrayList<>();
        releasesPager.setAdapter(new ReleasesPagerAdapter(releases));

        // Обработчики нажатий
        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        addReleaseButton.setOnClickListener(v -> {
            startActivity(new Intent(this, AddReleaseActivity.class));
        });
    }
}