package com.example.ratertune;

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
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddReleaseActivity extends AppCompatActivity {
    private ImageView coverImage;
    private TextInputEditText albumTitleInput;
    private TextInputEditText artistInput;
    private TextInputEditText releaseDateInput;
    private Button uploadButton;
    private Button saveButton;
    private ImageButton backButton;
    private Uri selectedImageUri;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();

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

        // Инициализация компонентов
        coverImage = findViewById(R.id.coverImage);
        albumTitleInput = findViewById(R.id.albumTitleInput);
        artistInput = findViewById(R.id.artistInput);
        releaseDateInput = findViewById(R.id.releaseDateInput);
        uploadButton = findViewById(R.id.uploadButton);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);

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
        String albumTitle = albumTitleInput.getText().toString().trim();
        String artist = artistInput.getText().toString().trim();
        String releaseDate = releaseDateInput.getText().toString().trim();

        if (albumTitle.isEmpty() || artist.isEmpty() || releaseDate.isEmpty() || selectedImageUri == null) {
            Toast.makeText(this, "Пожалуйста, заполните все поля и загрузите обложку", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Загрузка изображения в Supabase Storage и сохранение данных в базу
        // Здесь будет код для загрузки изображения и сохранения данных

        Toast.makeText(this, "Альбом успешно добавлен", Toast.LENGTH_SHORT).show();
        finish();
    }
} 