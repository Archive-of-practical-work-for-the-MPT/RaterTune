package com.example.ratertune.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ratertune.R;
import com.example.ratertune.adapters.ReviewsAdapter;
import com.example.ratertune.api.SupabaseClient;
import com.example.ratertune.models.Review;
import com.example.ratertune.utils.PicassoCache;
import com.example.ratertune.utils.SessionManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {
    private static final String TAG = "UserProfileActivity";

    private TextView likesCount;
    private TextView reviewsCount;
    private RecyclerView userReviewsRecyclerView;
    private TextView noReviewsText;
    private View progressOverlay;
    
    private SupabaseClient supabaseClient;
    private SessionManager sessionManager;
    
    private List<Review> userReviews;
    private ReviewsAdapter reviewsAdapter;
    
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        
        // Получаем переданные данные
        userId = getIntent().getStringExtra("userId");
        String userNameStr = getIntent().getStringExtra("userName");
        String userAvatarUrl = getIntent().getStringExtra("userAvatarUrl");
        
        if (userId == null) {
            Toast.makeText(this, "Ошибка загрузки профиля пользователя", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Инициализация SupabaseClient и SessionManager
        supabaseClient = SupabaseClient.getInstance();
        sessionManager = new SessionManager(this);
        
        // Инициализация UI элементов
        ShapeableImageView userAvatar = findViewById(R.id.userAvatar);
        TextView userName = findViewById(R.id.userName);
        likesCount = findViewById(R.id.likesCount);
        reviewsCount = findViewById(R.id.reviewsCount);
        userReviewsRecyclerView = findViewById(R.id.userReviewsRecyclerView);
        noReviewsText = findViewById(R.id.noReviewsText);
        progressOverlay = findViewById(R.id.progressOverlay);
        ImageButton backButton = findViewById(R.id.backButton);
        
        // Настройка кнопки назад
        backButton.setOnClickListener(v -> finish());
        
        // Настройка списка рецензий
        userReviews = new ArrayList<>();
        reviewsAdapter = new ReviewsAdapter(userReviews, this::onReviewClick);
        userReviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userReviewsRecyclerView.setAdapter(reviewsAdapter);
        
        // Устанавливаем переданные данные
        userName.setText(userNameStr);
        if (userAvatarUrl != null && !userAvatarUrl.isEmpty()) {
            Picasso.get()
                    .load(userAvatarUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(userAvatar);
        }
        
        // Загрузка данных пользователя
        loadUserData();
    }
    
    private void loadUserData() {
        showLoading(true);
        
        String token = sessionManager.getAccessToken();
        if (token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }
        
        // Загрузка статистики пользователя
        supabaseClient.getUserStatistics(userId, token, new SupabaseClient.UserStatisticsCallback() {
            @Override
            public void onSuccess(int likes, int reviews) {
                runOnUiThread(() -> {
                    likesCount.setText(String.valueOf(likes));
                    reviewsCount.setText(String.valueOf(reviews));
                    
                    // После получения статистики загружаем рецензии пользователя
                    loadUserReviews();
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error loading user statistics: " + errorMessage);
                runOnUiThread(() -> {
                    Toast.makeText(UserProfileActivity.this, 
                            "Ошибка загрузки статистики: " + errorMessage, 
                            Toast.LENGTH_SHORT).show();
                    likesCount.setText("0");
                    reviewsCount.setText("0");
                    
                    // Даже если получение статистики не удалось, пытаемся загрузить рецензии
                    loadUserReviews();
                });
            }
        });
    }
    
    private void loadUserReviews() {
        String token = sessionManager.getAccessToken();
        
        // Загрузка рецензий пользователя
        supabaseClient.getUserReviews(userId, token, new SupabaseClient.ReviewsCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    if (reviews.isEmpty()) {
                        noReviewsText.setVisibility(View.VISIBLE);
                        userReviewsRecyclerView.setVisibility(View.GONE);
                    } else {
                        noReviewsText.setVisibility(View.GONE);
                        userReviewsRecyclerView.setVisibility(View.VISIBLE);
                        
                        // Обновляем данные адаптера
                        userReviews.clear();
                        userReviews.addAll(reviews);
                        reviewsAdapter.notifyDataSetChanged();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading user reviews: " + error);
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(UserProfileActivity.this, 
                            "Ошибка загрузки рецензий: " + error, 
                            Toast.LENGTH_SHORT).show();
                    noReviewsText.setVisibility(View.VISIBLE);
                    userReviewsRecyclerView.setVisibility(View.GONE);
                });
            }
        });
    }
    
    private void onReviewClick(Review review) {
        // Переход к деталям релиза
        String releaseId = review.getReleaseId();
        
        // Получаем данные релиза
        String token = sessionManager.getAccessToken();
        if (token == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        
        supabaseClient.getRelease(releaseId, token, new SupabaseClient.ReleaseCallback() {
            @Override
            public void onSuccess(SupabaseClient.Release release) {
                SupabaseClient.getInstance().getReleaseAverageRating(
                        releaseId, 
                        token, 
                        new SupabaseClient.AverageRatingCallback() {
                            @Override
                            public void onSuccess(float averageRating, int reviewsCount) {
                                runOnUiThread(() -> {
                                    // Переход к экрану деталей релиза
                                    ReleaseDetailsActivity.start(
                                            UserProfileActivity.this,
                                            String.valueOf(release.getId()),
                                            release.getTitle(),
                                            release.getArtist(),
                                            release.getCoverUrl(),
                                            averageRating,
                                            release.getReleaseDate()
                                    );
                                });
                            }
                            
                            @Override
                            public void onError(String errorMessage) {
                                // Даже если не удалось получить рейтинг, всё равно открываем экран
                                ReleaseDetailsActivity.start(
                                        UserProfileActivity.this,
                                        String.valueOf(release.getId()),
                                        release.getTitle(),
                                        release.getArtist(),
                                        release.getCoverUrl(),
                                        0.0f,
                                        release.getReleaseDate()
                                );
                            }
                        });
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(UserProfileActivity.this, 
                            "Ошибка загрузки релиза: " + errorMessage, 
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showLoading(boolean isLoading) {
        progressOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
    
    // Статический метод для запуска активности
    public static void start(android.content.Context context, String userId, String userName, String userAvatarUrl) {
        android.content.Intent intent = new android.content.Intent(context, UserProfileActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userName", userName);
        intent.putExtra("userAvatarUrl", userAvatarUrl);
        context.startActivity(intent);
    }
} 