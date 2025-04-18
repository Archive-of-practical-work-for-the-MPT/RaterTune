package com.example.ratertune.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ratertune.R;
import com.example.ratertune.adapters.ReviewsAdapter;
import com.example.ratertune.api.SupabaseClient;
import com.example.ratertune.models.Review;
import com.example.ratertune.utils.SessionManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AllReviewsActivity extends AppCompatActivity {
    private RecyclerView reviewsRecyclerView;
    private List<Review> reviewsList;
    private List<Review> filteredReviewsList;
    private ReviewsAdapter reviewsAdapter;
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    private Spinner filterSpinner;
    
    // Константы для фильтрации
    private static final int FILTER_ALL = 0;
    private static final int FILTER_HIGH_RATING = 1;
    private static final int FILTER_LOW_RATING = 2;
    private static final int FILTER_NEWEST = 3;
    private static final int FILTER_OLDEST = 4;
    
    private int currentFilterOption = FILTER_NEWEST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reviews);

        // Инициализация toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Все рецензии");

        // Инициализация SupabaseClient и SessionManager
        supabaseClient = SupabaseClient.getInstance();
        sessionManager = new SessionManager(this);

        // Инициализация спиннера для фильтрации
        filterSpinner = findViewById(R.id.filterSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.review_filter_options, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        filterSpinner.setAdapter(adapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilterOption = position;
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });

        // Инициализация RecyclerView
        reviewsRecyclerView = findViewById(R.id.allReviewsRecyclerView);
        reviewsList = new ArrayList<>();
        filteredReviewsList = new ArrayList<>();
        reviewsAdapter = new ReviewsAdapter(filteredReviewsList, this::onReviewClick);
        
        // Настраиваем RecyclerView
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setAdapter(reviewsAdapter);

        // Загружаем все рецензии
        loadAllReviews();
    }

    private void loadAllReviews() {
        String token = sessionManager.getAccessToken();
        
        if (token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Показываем индикатор загрузки, если есть
        View loadingIndicator = findViewById(R.id.loadingIndicator);
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(View.VISIBLE);
        }
        
        // Без ограничения по количеству (или с большим лимитом, например 100)
        supabaseClient.getLatestReviews(token, 100, new SupabaseClient.ReviewsCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                runOnUiThread(() -> {
                    // Скрываем индикатор загрузки
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(View.GONE);
                    }
                    
                    // Сохраняем список рецензий
                    reviewsList.clear();
                    reviewsList.addAll(reviews);
                    
                    // Применяем фильтры
                    applyFilters();
                    
                    // Показываем сообщение, если нет рецензий
                    View noReviewsText = findViewById(R.id.noReviewsText);
                    if (noReviewsText != null) {
                        noReviewsText.setVisibility(reviewsList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Скрываем индикатор загрузки
                    if (loadingIndicator != null) {
                        loadingIndicator.setVisibility(View.GONE);
                    }
                    
                    // Показываем сообщение об ошибке
                    Toast.makeText(AllReviewsActivity.this, 
                            "Ошибка загрузки рецензий: " + error, 
                            Toast.LENGTH_SHORT).show();
                    
                    // Показываем сообщение, что нет рецензий
                    View noReviewsText = findViewById(R.id.noReviewsText);
                    if (noReviewsText != null) {
                        noReviewsText.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }
    
    private void applyFilters() {
        filteredReviewsList.clear();
        
        // Если список пуст, нечего фильтровать
        if (reviewsList.isEmpty()) {
            reviewsAdapter.notifyDataSetChanged();
            return;
        }
        
        // Добавляем все рецензии в отфильтрованный список
        filteredReviewsList.addAll(reviewsList);
        
        // Применяем фильтр
        switch (currentFilterOption) {
            case FILTER_ALL:
                // Все рецензии, ничего не делаем
                break;
                
            case FILTER_HIGH_RATING:
                // Фильтруем по высокому рейтингу (>=7)
                filteredReviewsList.removeIf(review -> review.getRating() < 7.0f);
                break;
                
            case FILTER_LOW_RATING:
                // Фильтруем по низкому рейтингу (<7)
                filteredReviewsList.removeIf(review -> review.getRating() >= 7.0f);
                break;
                
            case FILTER_NEWEST:
                // Сортируем по дате (сначала новые)
                // По умолчанию уже отсортировано по newest, ничего не делаем
                break;
                
            case FILTER_OLDEST:
                // Сортируем по дате (сначала старые)
                // Просто переворачиваем список
                Collections.reverse(filteredReviewsList);
                break;
        }
        
        // Обновляем адаптер
        reviewsAdapter.notifyDataSetChanged();
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
                    Intent intent = new Intent(AllReviewsActivity.this, ReleaseDetailsActivity.class);
                    intent.putExtra("id", String.valueOf(release.getId()));
                    intent.putExtra("title", release.getTitle());
                    intent.putExtra("artist", release.getArtist());
                    intent.putExtra("imageUrl", release.getCoverUrl());
                    intent.putExtra("rating", 0.0f); // Будет обновлено на экране деталей
                    intent.putExtra("releaseDate", release.getReleaseDate());
                    startActivity(intent);
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(AllReviewsActivity.this, 
                            "Ошибка загрузки релиза: " + errorMessage, 
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 