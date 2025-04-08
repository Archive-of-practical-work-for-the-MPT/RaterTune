package com.example.ratertune.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ratertune.R;
import com.example.ratertune.api.SupabaseClient;
import com.example.ratertune.utils.SessionManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class AddReleaseActivity extends AppCompatActivity {
    private ImageView coverImage;
    private TextInputEditText albumTitleInput;
    private TextInputEditText artistInput;
    private TextInputEditText releaseDateInput;
    private Button uploadButton;
    private Button saveButton;
    private View progressOverlay;
    private Uri selectedImageUri;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();
    private SessionManager sessionManager;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        Picasso.get().load(selectedImageUri).into(coverImage);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_release);

        // Инициализация менеджера сессии
        sessionManager = new SessionManager(this);

        // Инициализация компонентов
        coverImage = findViewById(R.id.coverImage);
        albumTitleInput = findViewById(R.id.albumTitleInput);
        artistInput = findViewById(R.id.artistInput);
        releaseDateInput = findViewById(R.id.releaseDateInput);
        uploadButton = findViewById(R.id.uploadButton);
        saveButton = findViewById(R.id.saveButton);
        ImageButton backButton = findViewById(R.id.backButton);
        progressOverlay = findViewById(R.id.addReleaseProgressOverlay);

        // Обработчик нажатия на кнопку загрузки обложки
        uploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // Обработчик нажатия на поле даты выпуска
        releaseDateInput.setOnClickListener(v -> showDatePicker());

        // Обработчик нажатия на кнопку сохранения
        saveButton.setOnClickListener(v -> saveRelease());

        // Обработчик нажатия на кнопку назад
        backButton.setOnClickListener(v -> finish());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    releaseDateInput.setText(dateFormat.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void saveRelease() {
        String albumTitle = Objects.requireNonNull(albumTitleInput.getText()).toString().trim();
        String artist = Objects.requireNonNull(artistInput.getText()).toString().trim();
        String releaseDate = Objects.requireNonNull(releaseDateInput.getText()).toString().trim();

        if (albumTitle.isEmpty() || artist.isEmpty() || releaseDate.isEmpty() || selectedImageUri == null) {
            Toast.makeText(this, "Пожалуйста, заполните все поля и загрузите обложку", Toast.LENGTH_SHORT).show();
            return;
        }

        // Показываем индикатор загрузки
        showLoading(true);

        // Получаем данные пользователя из сессии
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();

        // Загружаем альбом через Supabase API
        SupabaseClient.getInstance().addRelease(
            albumTitle,
            artist,
            releaseDate,
            selectedImageUri,
            this,
            userId,
            token,
            new SupabaseClient.ReleaseCallback() {
                @Override
                public void onSuccess(SupabaseClient.Release release) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showSuccessMessage();
                        // Закрываем экран после успешной загрузки
                        finish();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showErrorMessage(errorMessage);
                    });
                }
            }
        );
    }

    // Показывает сообщение об успешном добавлении альбома
    private void showSuccessMessage() {
        Snackbar.make(
            findViewById(android.R.id.content),
            "Альбом успешно добавлен",
            Snackbar.LENGTH_LONG
        ).show();
    }

    // Показывает сообщение об ошибке при добавлении альбома
    private void showErrorMessage(String errorMessage) {
        String userFriendlyMessage;
        
        if (errorMessage.contains("Configuration error")) {
            userFriendlyMessage = "Не удалось подключиться к серверу. Пожалуйста, сообщите разработчикам об этой проблеме.";
        } 
        else if (errorMessage.contains("Network error") || errorMessage.contains("timeout")) {
            userFriendlyMessage = "Проверьте подключение к интернету и попробуйте снова.";
        }
        else if (errorMessage.contains("Failed to upload image")) {
            userFriendlyMessage = "Не удалось загрузить изображение. Выберите другое изображение или попробуйте позже.";
        }
        else {
            userFriendlyMessage = "Не удалось добавить альбом. Пожалуйста, попробуйте позже.";
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ошибка добавления альбома")
               .setMessage(userFriendlyMessage)
               .setPositiveButton("Понятно", null)
               .show();
    }

    // Показывает или скрывает индикатор загрузки
    private void showLoading(boolean isLoading) {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        
        // Блокируем кнопки во время загрузки
        saveButton.setEnabled(!isLoading);
        uploadButton.setEnabled(!isLoading);
    }
} 