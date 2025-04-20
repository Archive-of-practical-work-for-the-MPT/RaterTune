package com.example.ratertune.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ratertune.R;
import com.example.ratertune.utils.SessionManager;
import com.example.ratertune.api.StoryCallback;
import com.example.ratertune.api.SupabaseClient;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public class AddStoryActivity extends AppCompatActivity {
    private ImageView storyImage;
    private TextInputEditText storyTextInput;
    private Button uploadButton;
    private Button saveButton;
    private View progressOverlay;
    private Uri selectedImageUri;
    private SessionManager sessionManager;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        Picasso.get().load(selectedImageUri).into(storyImage);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_story);

        // Инициализация менеджера сессии
        sessionManager = new SessionManager(this);

        // Инициализация компонентов
        storyImage = findViewById(R.id.storyImage);
        storyTextInput = findViewById(R.id.storyTextInput);
        uploadButton = findViewById(R.id.uploadButton);
        saveButton = findViewById(R.id.saveButton);
        progressOverlay = findViewById(R.id.addStoryProgressOverlay);

        // Обработчик нажатия на кнопку загрузки изображения
        uploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // Обработчик нажатия на кнопку сохранения
        saveButton.setOnClickListener(v -> saveStory());
    }

    private void saveStory() {
        String text = Objects.requireNonNull(storyTextInput.getText()).toString().trim();

        if (text.isEmpty() || selectedImageUri == null) {
            Toast.makeText(this, "Пожалуйста, добавьте изображение и текст", Toast.LENGTH_SHORT).show();
            return;
        }

        // Показываем индикатор загрузки
        showLoading(true);

        // Получаем данные пользователя из сессии
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();

        // Устанавливаем время истечения сториза (24 часа)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 24);
        Date expiresAt = calendar.getTime();

        // Загружаем сториз через Supabase API
        SupabaseClient.getInstance().addStory(
            text,
            selectedImageUri,
            this,
            userId,
            token,
            expiresAt,
            new StoryCallback() {
                @Override
                public void onSuccess(com.example.ratertune.models.Story story) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(AddStoryActivity.this, "Сториз успешно добавлен", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(AddStoryActivity.this, "Ошибка: " + errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        );
    }

    private void showLoading(boolean isLoading) {
        progressOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!isLoading);
        uploadButton.setEnabled(!isLoading);
    }
} 