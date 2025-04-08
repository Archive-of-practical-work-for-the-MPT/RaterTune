package com.example.ratertune.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ratertune.R;
import com.example.ratertune.model.Release;
import com.example.ratertune.widget.SquareImageView;
import com.squareup.picasso.Picasso;

public class ReleaseDetailsActivity extends AppCompatActivity {
    private ImageButton backButton;
    private SquareImageView coverImage;
    private TextView titleText;
    private TextView artistText;
    private TextView releaseDateText;
    private TextView ratingText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_release_details);
        
        // Инициализация компонентов
        backButton = findViewById(R.id.backButton);
        coverImage = findViewById(R.id.coverImage);
        titleText = findViewById(R.id.titleText);
        artistText = findViewById(R.id.artistText);
        releaseDateText = findViewById(R.id.releaseDateText);
        ratingText = findViewById(R.id.ratingText);
        
        // Получаем данные альбома из Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String id = extras.getString("id");
            String title = extras.getString("title");
            String artist = extras.getString("artist");
            String imageUrl = extras.getString("imageUrl");
            float rating = extras.getFloat("rating", 0.0f);
            String releaseDate = extras.getString("releaseDate");
            
            // Отображаем данные
            titleText.setText(title);
            artistText.setText(artist);
            releaseDateText.setText(releaseDate);
            
            // Форматируем рейтинг
            String ratingText = String.format("%.1f", rating);
            this.ratingText.setText(ratingText);
            
            // Загружаем изображение
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Picasso.get()
                       .load(imageUrl)
                       .resize(500, 500)
                       .centerCrop()
                       .placeholder(R.drawable.ic_album_placeholder)
                       .error(R.drawable.ic_album_placeholder)
                       .into(coverImage);
            }
        }
        
        // Обработчик нажатия на кнопку назад
        backButton.setOnClickListener(v -> finish());
    }
} 