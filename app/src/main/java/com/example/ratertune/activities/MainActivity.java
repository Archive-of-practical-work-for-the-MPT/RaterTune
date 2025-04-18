package com.example.ratertune.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ratertune.R;
import com.example.ratertune.adapters.ReleasesAdapter;
import com.example.ratertune.adapters.ReviewsAdapter;
import com.example.ratertune.adapters.StoriesAdapter;
import com.example.ratertune.api.SupabaseClient;
import com.example.ratertune.api.StoriesListCallback;
import com.example.ratertune.models.Release;
import com.example.ratertune.models.Review;
import com.example.ratertune.models.Story;
import com.example.ratertune.utils.PicassoCache;
import com.example.ratertune.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ReleasesAdapter.OnReleaseClickListener {
    private static final String TAG = "MainActivity";

    private RecyclerView latestReviewsRecyclerView;
    private TextView noLatestReviewsText;
    
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    
    private List<Story> storiesList;
    private StoriesAdapter storiesAdapter;
    
    private List<Release> releasesList;
    private ReleasesAdapter releasesAdapter;
    
    private List<Review> latestReviewsList;
    private ReviewsAdapter latestReviewsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация SupabaseClient и SessionManager
        supabaseClient = SupabaseClient.getInstance();
        sessionManager = new SessionManager(this);
        supabaseClient.setSessionManager(sessionManager);

        // Инициализация UI элементов
        RecyclerView storiesRecyclerView = findViewById(R.id.storiesRecycler);
        RecyclerView releasesRecyclerView = findViewById(R.id.releasesRecyclerView);
        latestReviewsRecyclerView = findViewById(R.id.latestReviewsRecyclerView);
        noLatestReviewsText = findViewById(R.id.noLatestReviewsText);
        
        ImageButton profileButton = findViewById(R.id.profileButton);
        ImageButton addStoryButton = findViewById(R.id.addStoryButton);
        ImageButton addReleaseButton = findViewById(R.id.addReleaseButton);
        TextView viewAllReleasesButton = findViewById(R.id.viewAllReleasesButton);
        TextView viewAllReviewsButton = findViewById(R.id.viewAllReviewsButton);

        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
        
        addStoryButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddStoryActivity.class);
            startActivity(intent);
        });
        
        addReleaseButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddReleaseActivity.class);
            startActivity(intent);
        });
        
        viewAllReleasesButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AllReleasesActivity.class);
            startActivity(intent);
        });
        
        viewAllReviewsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AllReviewsActivity.class);
            startActivity(intent);
        });
        
        // Настройка RecyclerView для сторизов
        storiesList = new ArrayList<>();
        storiesAdapter = new StoriesAdapter(storiesList, this::onStoryClick, this);
        LinearLayoutManager storiesLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        storiesRecyclerView.setLayoutManager(storiesLayoutManager);
        storiesRecyclerView.setAdapter(storiesAdapter);
        
        // Настройка RecyclerView для релизов
        releasesList = new ArrayList<>();
        releasesAdapter = new ReleasesAdapter(releasesList, this, this);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        releasesRecyclerView.setLayoutManager(layoutManager);
        releasesRecyclerView.setAdapter(releasesAdapter);

        // Настройка RecyclerView для последних рецензий
        latestReviewsList = new ArrayList<>();
        latestReviewsAdapter = new ReviewsAdapter(latestReviewsList, this::onReviewClick);
        LinearLayoutManager reviewsLayoutManager = new LinearLayoutManager(this);
        latestReviewsRecyclerView.setLayoutManager(reviewsLayoutManager);
        latestReviewsRecyclerView.setAdapter(latestReviewsAdapter);
        
        // Загрузка данных
        loadUserStories();
        loadUserReleases();
        loadLatestReviews();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем данные при возвращении на экран
        loadUserStories();
        loadUserReleases();
        loadLatestReviews();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Отменяем загрузки изображений с тегами "main_activity" при уходе с экрана
        PicassoCache.cancelTag("main_activity");
    }
    
    @Override
    protected void onDestroy() {
        // Отменяем все активные загрузки для предотвращения утечек памяти
        if (releasesList != null) {
            for (Release release : releasesList) {
                PicassoCache.cancelTag(release.getId());
            }
        }
        
        if (storiesList != null) {
            for (Story story : storiesList) {
                PicassoCache.cancelTag(story.getId());
            }
        }
        
        super.onDestroy();
    }
    
    private void onStoryClick(Story story) {
        // Отмечаем сториз как просмотренный
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        
        supabaseClient.markStoryAsViewed(story.getId(), userId, token, new SupabaseClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                // Обновляем статус просмотра
                runOnUiThread(() -> {
                    storiesAdapter.updateStoryViewedStatus(story.getId(), true);
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error marking story as viewed: " + errorMessage);
            }
        });
        
        Intent intent = new Intent(MainActivity.this, StoryViewActivity.class);
        intent.putExtra("id", story.getId());
        intent.putExtra("imageUrl", story.getImageUrl());
        intent.putExtra("text", story.getText());
        startActivity(intent);
    }
    
    private void loadUserStories() {
        String token = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();
        
        if (token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Сначала загрузим просмотренные сторизы
        supabaseClient.getViewedStoriesForUser(userId, token, new SupabaseClient.ViewedStoriesCallback() {
            @Override
            public void onSuccess(List<String> viewedStoryIds) {
                // Теперь загрузим все сторизы
                supabaseClient.getAllStories(token, new StoriesListCallback() {
                    @Override
                    public void onSuccess(List<Story> stories) {
                        // Отмечаем просмотренные сторизы
                        for (Story story : stories) {
                            if (viewedStoryIds.contains(story.getId())) {
                                story.setViewed(true);
                            }
                        }
                        
                        runOnUiThread(() -> {
                            storiesList.clear();
                            storiesList.addAll(stories);
                            storiesAdapter.notifyDataSetChanged();
                            
                            // Предварительно загружаем все изображения в кэш
                            for (Story story : stories) {
                                PicassoCache.preloadImage(MainActivity.this, story.getImageUrl());
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Ошибка загрузки сторизов: " + errorMessage, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                // Даже если ошибка с просмотренными сторизами, всё равно загружаем сторизы
                supabaseClient.getAllStories(token, new StoriesListCallback() {
                    @Override
                    public void onSuccess(List<Story> stories) {
                        runOnUiThread(() -> {
                            storiesList.clear();
                            storiesList.addAll(stories);
                            storiesAdapter.notifyDataSetChanged();
                            
                            // Предварительно загружаем все изображения в кэш
                            for (Story story : stories) {
                                PicassoCache.preloadImage(MainActivity.this, story.getImageUrl());
                            }
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "Ошибка загрузки сторизов: " + error, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        });
    }
    
    private void loadUserReleases() {
        String token = sessionManager.getAccessToken();
        
        if (token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        supabaseClient.getAllReleases(token, new SupabaseClient.ReleasesListCallback() {
            @Override
            public void onSuccess(List<SupabaseClient.Release> releases) {
                // Конвертируем SupabaseClient.Release в нашу модель Release
                releasesList.clear();
                
                // Сначала добавляем все релизы с временными оценками 0
                for (SupabaseClient.Release release : releases) {
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
                    
                    // Для каждого релиза запрашиваем средний рейтинг
                    for (int i = 0; i < releasesList.size(); i++) {
                        final int position = i;
                        String releaseId = releasesList.get(i).getId();
                        supabaseClient.calculateAverageRating(releaseId, token, new SupabaseClient.AverageRatingCallback() {
                            @Override
                            public void onSuccess(float averageRating, int reviewsCount) {
                                // Обновляем рейтинг в модели и в адаптере
                                runOnUiThread(() -> {
                                    releasesList.get(position).setRating(averageRating);
                                    releasesAdapter.notifyItemChanged(position);
                                });
                            }
                            
                            @Override
                            public void onError(String errorMessage) {
                                // Просто логируем ошибку, не показываем пользователю
                                Log.e(TAG, "Error calculating rating for release " + releaseId + ": " + errorMessage);
                            }
                        });
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки релизов: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void loadLatestReviews() {
        String token = sessionManager.getAccessToken();
        
        if (token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Получаем последние 4 рецензии
        supabaseClient.getLatestReviews(token, 4, new SupabaseClient.ReviewsCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                runOnUiThread(() -> {
                    latestReviewsList.clear();
                    latestReviewsList.addAll(reviews);
                    
                    if (reviews.isEmpty()) {
                        noLatestReviewsText.setVisibility(View.VISIBLE);
                        latestReviewsRecyclerView.setVisibility(View.GONE);
                    } else {
                        noLatestReviewsText.setVisibility(View.GONE);
                        latestReviewsRecyclerView.setVisibility(View.VISIBLE);
                        
                        // Предварительно загружаем аватары пользователей в кэш
                        for (Review review : reviews) {
                            if (review.getUserAvatarUrl() != null && !review.getUserAvatarUrl().isEmpty()) {
                                PicassoCache.preloadImage(MainActivity.this, review.getUserAvatarUrl());
                            }
                        }
                        
                        latestReviewsAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Loaded " + reviews.size() + " latest reviews");
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки рецензий: " + error, Toast.LENGTH_SHORT).show();
                    noLatestReviewsText.setVisibility(View.VISIBLE);
                    latestReviewsRecyclerView.setVisibility(View.GONE);
                });
            }
        });
    }
    
    // Обработка нажатия на релиз
    @Override
    public void onReleaseClick(Release release) {
        Intent intent = new Intent(MainActivity.this, ReleaseDetailsActivity.class);
        intent.putExtra("id", release.getId());
        intent.putExtra("title", release.getTitle());
        intent.putExtra("artist", release.getArtist());
        intent.putExtra("imageUrl", release.getImageUrl());
        intent.putExtra("rating", release.getRating());
        intent.putExtra("releaseDate", release.getReleaseDate());
        startActivity(intent);
    }

    private void onReviewClick(Review review) {
        // Получаем ID релиза из рецензии
        String releaseId = review.getReleaseId();
        
        // Получаем данные релиза перед открытием экрана
        String token = sessionManager.getAccessToken();
        if (token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Показываем индикатор загрузки
        Toast.makeText(this, "Загрузка релиза...", Toast.LENGTH_SHORT).show();
        
        // Получаем данные о релизе
        supabaseClient.getRelease(releaseId, token, new SupabaseClient.ReleaseCallback() {
            @Override
            public void onSuccess(SupabaseClient.Release release) {
                runOnUiThread(() -> {
                    // Открываем экран с деталями релиза
                    Intent intent = new Intent(MainActivity.this, ReleaseDetailsActivity.class);
                    intent.putExtra("id", String.valueOf(release.getId()));
                    intent.putExtra("title", release.getTitle());
                    intent.putExtra("artist", release.getArtist());
                    intent.putExtra("imageUrl", release.getCoverUrl());
                    intent.putExtra("rating", 0.0f); // Временно используем 0
                    intent.putExtra("releaseDate", release.getReleaseDate());
                    startActivity(intent);
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки релиза: " + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}