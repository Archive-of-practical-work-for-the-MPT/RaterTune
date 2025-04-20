package com.example.ratertune.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ratertune.R;
import com.squareup.picasso.Picasso;

public class StoryViewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_view);

        // Инициализация компонентов
        ImageView storyImage = findViewById(R.id.storyImage);
        TextView storyText = findViewById(R.id.storyText);

        // Получаем данные из Intent
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String text = getIntent().getStringExtra("text");

        // Загружаем изображение
        Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.ic_album_placeholder)
                .error(R.drawable.ic_album_placeholder)
                .into(storyImage);

        // Устанавливаем текст, если он есть
        if (text != null && !text.isEmpty()) {
            storyText.setText(text);
            storyText.setVisibility(View.VISIBLE);
        } else {
            storyText.setVisibility(View.GONE);
        }

        // Обработчик нажатия на изображение для закрытия
        storyImage.setOnClickListener(v -> finish());
    }
}
