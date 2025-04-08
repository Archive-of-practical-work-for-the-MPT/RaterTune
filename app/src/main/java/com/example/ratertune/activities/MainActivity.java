package com.example.ratertune.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ratertune.R;
import com.example.ratertune.adapter.ReleasesAdapter;
import com.example.ratertune.api.SupabaseClient;
import com.example.ratertune.model.Release;
import com.example.ratertune.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ReleasesAdapter.OnReleaseClickListener {
    private RecyclerView releasesRecyclerView;
    private ImageButton profileButton;
    private ImageButton addReleaseButton;
    private SessionManager sessionManager;
    private List<Release> releasesList;
    private ReleasesAdapter releasesAdapter;
    private SupabaseClient supabaseClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        supabaseClient = SupabaseClient.getInstance();
        
        // Проверяем, есть ли активная сессия
        if (!sessionManager.isLoggedIn()) {
            // Если нет, переходим на экран авторизации
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Инициализация компонентов
        releasesRecyclerView = findViewById(R.id.releasesRecyclerView);
        profileButton = findViewById(R.id.profileButton);
        addReleaseButton = findViewById(R.id.addReleaseButton);

        // Настройка RecyclerView
        releasesList = new ArrayList<>();
        releasesAdapter = new ReleasesAdapter(releasesList, this);
        
        // Устанавливаем сетку 2 колонки
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        releasesRecyclerView.setLayoutManager(layoutManager);
        releasesRecyclerView.setAdapter(releasesAdapter);

        // Обработчики нажатий
        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        addReleaseButton.setOnClickListener(v -> {
            startActivity(new Intent(this, AddReleaseActivity.class));
        });
        
        // Загружаем альбомы пользователя
        loadUserReleases();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем список альбомов при возврате на экран
        loadUserReleases();
    }
    
    /**
     * Загружает альбомы пользователя из Supabase
     */
    private void loadUserReleases() {
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        
        if (userId == null || token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        supabaseClient.getUserReleases(userId, token, new SupabaseClient.ReleasesListCallback() {
            @Override
            public void onSuccess(List<SupabaseClient.Release> releases) {
                // Конвертируем SupabaseClient.Release в нашу модель Release
                releasesList.clear();
                
                for (SupabaseClient.Release release : releases) {
                    // Используем 0.0f как временную оценку, так как в текущей реализации нет оценок
                    Release modelRelease = new Release(
                            String.valueOf(release.getId()),
                            release.getTitle(),
                            release.getArtist(),
                            release.getCoverUrl(),
                            0.0f,
                            release.getReleaseDate()
                    );
                    releasesList.add(modelRelease);
                }
                
                // Обновляем UI в основном потоке
                runOnUiThread(() -> {
                    releasesAdapter.notifyDataSetChanged();
                    
                    if (releasesList.isEmpty()) {
                        Toast.makeText(MainActivity.this, "У вас пока нет добавленных альбомов", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                // Обработка ошибки в основном потоке
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки альбомов: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    @Override
    public void onReleaseClick(Release release) {
        // Открываем экран с деталями альбома
        Intent intent = new Intent(this, ReleaseDetailsActivity.class);
        intent.putExtra("id", release.getId());
        intent.putExtra("title", release.getTitle());
        intent.putExtra("artist", release.getArtist());
        intent.putExtra("imageUrl", release.getImageUrl());
        intent.putExtra("rating", release.getRating());
        intent.putExtra("releaseDate", release.getReleaseDate());
        startActivity(intent);
    }
}