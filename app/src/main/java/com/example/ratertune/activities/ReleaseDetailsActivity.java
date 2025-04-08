package com.example.ratertune.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
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
import com.example.ratertune.widget.SquareImageView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class ReleaseDetailsActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private SquareImageView coverImage;
    private TextView titleText;
    private TextView artistText;
    private TextView releaseDateText;
    private TextView ratingText;
    private TextView noReviewsText;
    private RecyclerView reviewsRecyclerView;
    private ReviewsAdapter reviewsAdapter;
    private ImageButton addReviewButton;
    private String releaseId;
    private SupabaseClient supabaseClient;
    private ImageButton backButton;
    private SessionManager sessionManager;
    
    // Bottom sheet components
    private View addReviewBottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private Slider ratingSlider;
    private TextView ratingValueText;
    private TextInputEditText reviewTextInput;
    private MaterialButton cancelButton;
    private MaterialButton submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_release_details);

        supabaseClient = SupabaseClient.getInstance();
        sessionManager = new SessionManager(this);
        supabaseClient.setSessionManager(sessionManager);

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        coverImage = findViewById(R.id.coverImage);
        titleText = findViewById(R.id.titleText);
        artistText = findViewById(R.id.artistText);
        releaseDateText = findViewById(R.id.releaseDateText);
        ratingText = findViewById(R.id.ratingText);
        noReviewsText = findViewById(R.id.noReviewsText);
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);
        addReviewButton = findViewById(R.id.addReviewButton);
        backButton = findViewById(R.id.backButton);
        
        // Initialize bottom sheet components
        addReviewBottomSheet = findViewById(R.id.addReviewBottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(addReviewBottomSheet);
        ratingSlider = addReviewBottomSheet.findViewById(R.id.ratingSlider);
        ratingValueText = addReviewBottomSheet.findViewById(R.id.ratingValueText);
        reviewTextInput = addReviewBottomSheet.findViewById(R.id.reviewTextInput);
        cancelButton = addReviewBottomSheet.findViewById(R.id.cancelButton);
        submitButton = addReviewBottomSheet.findViewById(R.id.submitButton);

        // Setup toolbar
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup reviews RecyclerView
        reviewsAdapter = new ReviewsAdapter(new ArrayList<>());
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setAdapter(reviewsAdapter);

        // Get release data from intent
        Intent intent = getIntent();
        releaseId = intent.getStringExtra("id");
        String title = intent.getStringExtra("title");
        String artist = intent.getStringExtra("artist");
        String imageUrl = intent.getStringExtra("imageUrl");
        float rating = intent.getFloatExtra("rating", 0f);
        String releaseDate = intent.getStringExtra("releaseDate");

        // Set release data
        titleText.setText(title);
        artistText.setText(artist);
        releaseDateText.setText(releaseDate);
        ratingText.setText(String.format("%.1f", rating));

        // Load cover image
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                   .load(imageUrl)
                   .resize(500, 500)
                   .centerCrop()
                   .placeholder(R.drawable.ic_album_placeholder)
                   .error(R.drawable.ic_album_placeholder)
                   .into(coverImage);
        } else {
            coverImage.setImageResource(R.drawable.ic_album_placeholder);
        }

        // Setup add review button
        addReviewButton.setOnClickListener(v -> showAddReviewBottomSheet());

        // Setup back button
        backButton.setOnClickListener(v -> finish());
        
        // Setup bottom sheet components
        setupBottomSheet();

        // Load reviews
        loadReviews();
    }

    private void setupBottomSheet() {
        // Set initial rating value
        ratingSlider.setValue(5.0f);
        ratingValueText.setText(String.format("%.1f", 5.0f));

        // Update rating value text when slider changes
        ratingSlider.addOnChangeListener((slider, value, fromUser) -> 
            ratingValueText.setText(String.format("%.1f", value))
        );
        
        // Setup cancel button
        cancelButton.setOnClickListener(v -> hideAddReviewBottomSheet());
        
        // Setup submit button
        submitButton.setOnClickListener(v -> submitReview());
    }
    
    private void showAddReviewBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
    
    private void hideAddReviewBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }
    
    private void submitReview() {
        String text = reviewTextInput.getText().toString().trim();
        float rating = ratingSlider.getValue();
        
        if (text.isEmpty()) {
            reviewTextInput.setError("Введите текст рецензии");
            return;
        }
        
        if (rating == 0) {
            Toast.makeText(this, "Выберите оценку", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Получаем токен и ID пользователя из SessionManager
        String token = sessionManager.getAccessToken();
        
        // Показываем индикатор загрузки
        submitButton.setEnabled(false);
        submitButton.setText("Отправка...");
        
        supabaseClient.addReview(releaseId, rating, text, token, new SupabaseClient.ReviewCallback() {
            @Override
            public void onSuccess(Review review) {
                runOnUiThread(() -> {
                    // Скрываем индикатор загрузки
                    submitButton.setEnabled(true);
                    submitButton.setText("Отправить");
                    
                    // Очищаем поля ввода
                    reviewTextInput.setText("");
                    ratingSlider.setValue(5.0f);
                    
                    // Скрываем bottom sheet
                    hideAddReviewBottomSheet();
                    
                    // Обновляем список рецензий
                    loadReviews();
                    
                    // Показываем сообщение об успехе
                    Toast.makeText(ReleaseDetailsActivity.this, 
                        "Рецензия успешно добавлена", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Скрываем индикатор загрузки
                    submitButton.setEnabled(true);
                    submitButton.setText("Отправить");
                    
                    // Показываем сообщение об ошибке
                    Toast.makeText(ReleaseDetailsActivity.this, 
                        "Ошибка: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadReviews() {
        // Получаем токен из SessionManager
        String token = sessionManager.getAccessToken();
        
        supabaseClient.getReviews(releaseId, token, new SupabaseClient.ReviewsCallback() {
            @Override
            public void onSuccess(List<Review> reviews) {
                runOnUiThread(() -> {
                    if (reviews.isEmpty()) {
                        noReviewsText.setVisibility(View.VISIBLE);
                        reviewsRecyclerView.setVisibility(View.GONE);
                    } else {
                        noReviewsText.setVisibility(View.GONE);
                        reviewsRecyclerView.setVisibility(View.VISIBLE);
                        // Создаем новый адаптер с обновленным списком
                        reviewsAdapter = new ReviewsAdapter(reviews);
                        reviewsRecyclerView.setAdapter(reviewsAdapter);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(ReleaseDetailsActivity.this, 
                        "Ошибка загрузки рецензий: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
} 