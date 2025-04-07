package com.example.ratertune;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ratertune.api.SupabaseClient;
import com.example.ratertune.utils.SessionManager;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;

public class ProfileActivity extends AppCompatActivity {
    private static final int MAX_IMAGE_SIZE_MB = 5; // Максимальный размер изображения в МБ
    
    private ImageButton backButton;
    private ShapeableImageView profileImage;
    private TextView userNameText;
    private TextView userEmailText;
    private TextView reviewsCountText;
    private TextView averageRatingText;
    private Button logoutButton;
    private TextInputLayout usernameLayout;
    private TextInputEditText usernameInput;
    private Button saveUsernameButton;
    private View profileProgressOverlay;
    private SessionManager sessionManager;
    private Uri selectedAvatarUri = null;
    
    // Лаунчер для выбора изображения из галереи
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    try {
                        // Проверяем размер изображения
                        long imageSize = getImageSize(imageUri);
                        if (imageSize > MAX_IMAGE_SIZE_MB * 1024 * 1024) {
                            showError("Изображение слишком большое. Максимальный размер: " + MAX_IMAGE_SIZE_MB + " МБ");
                            return;
                        }
                        
                        // Сохраняем URI изображения
                        selectedAvatarUri = imageUri;
                        
                        // Убираем любой tint перед загрузкой изображения
                        profileImage.setColorFilter(null);
                        
                        // Отображаем выбранное изображение с правильными настройками масштабирования
                        Picasso.get()
                               .load(imageUri)
                               .resize(300, 300)
                               .centerCrop()
                               .placeholder(R.drawable.ic_profile)
                               .error(R.drawable.ic_profile)
                               .into(profileImage);
                        
                        // Спрашиваем пользователя, загрузить ли аватарку на сервер
                        showUploadAvatarDialog();
                    } catch (Exception e) {
                        showError("Ошибка при обработке изображения: " + e.getMessage());
                    }
                }
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        
        // Инициализация компонентов
        backButton = findViewById(R.id.backButton);
        profileImage = findViewById(R.id.profileImage);
        userNameText = findViewById(R.id.userNameText);
        userEmailText = findViewById(R.id.userEmailText);
        reviewsCountText = findViewById(R.id.reviewsCountText);
        averageRatingText = findViewById(R.id.averageRatingText);
        logoutButton = findViewById(R.id.logoutButton);
        usernameLayout = findViewById(R.id.usernameLayout);
        usernameInput = findViewById(R.id.usernameInput);
        saveUsernameButton = findViewById(R.id.saveUsernameButton);
        profileProgressOverlay = findViewById(R.id.profileProgressOverlay);
        
        // Убираем tint с иконки профиля
        profileImage.setColorFilter(null);

        // Загрузка данных пользователя
        loadUserData();

        // Обработчики нажатий
        backButton.setOnClickListener(v -> finish());

        saveUsernameButton.setOnClickListener(v -> updateUsername());

        logoutButton.setOnClickListener(v -> {
            sessionManager.logout();
            // Возвращаемся на экран авторизации
            Intent intent = new Intent(this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
        
        // Обработчик нажатия на изображение профиля
        profileImage.setOnClickListener(v -> openImagePicker());
    }

    private void loadUserData() {
        // Загрузка данных из сессии
        String userName = sessionManager.getUserName();
        String userEmail = sessionManager.getUserEmail();
        String avatarUrl = sessionManager.getUserAvatarUrl();

        // Отображение данных
        userNameText.setText(userName);
        userEmailText.setText(userEmail);
        usernameInput.setText(userName);
        
        // Загрузка аватарки, если URL есть
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            loadProfileImage(avatarUrl);
        }

        // TODO: Загрузка статистики из Supabase
        reviewsCountText.setText("0");
        averageRatingText.setText("0.0");
    }
    
    /**
     * Загружает изображение в ImageView профиля с оптимальными настройками
     */
    private void loadProfileImage(String imageUrl) {
        // Убираем tint с изображения перед загрузкой аватарки
        profileImage.setColorFilter(null);
        
        // Добавляем параметр к URL для обхода кэша, если изображение обновилось
        String imageUrlWithCacheBusting = imageUrl + "?t=" + System.currentTimeMillis();
        
        Picasso.get()
               .load(imageUrlWithCacheBusting)
               .resize(300, 300) // Оптимальный размер для хорошего качества
               .centerCrop() // Центрирование изображения
               .placeholder(R.drawable.ic_profile)
               .error(R.drawable.ic_profile)
               .memoryPolicy(com.squareup.picasso.MemoryPolicy.NO_CACHE)
               .networkPolicy(com.squareup.picasso.NetworkPolicy.NO_CACHE)
               .into(profileImage);
    }
    
    /**
     * Открывает диалог выбора изображения из галереи
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }
    
    /**
     * Показывает диалог подтверждения загрузки аватарки
     */
    private void showUploadAvatarDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Загрузить аватарку")
               .setMessage("Загрузить выбранное изображение как новую аватарку?")
               .setPositiveButton("Да", (dialog, which) -> uploadAvatar())
               .setNegativeButton("Нет", null)
               .show();
    }
    
    /**
     * Загружает выбранную аватарку в Supabase
     */
    private void uploadAvatar() {
        if (selectedAvatarUri == null) {
            showError("Сначала выберите изображение");
            return;
        }
        
        showLoading(true);
        
        // Получаем необходимые данные из сессии
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        
        if (userId == null || token == null) {
            showLoading(false);
            showError("Ошибка авторизации");
            return;
        }
        
        // Вызываем метод загрузки аватарки
        SupabaseClient.getInstance().updateUserAvatar(
            userId,
            selectedAvatarUri,
            this,
            token,
            new SupabaseClient.ProfileUpdateCallback() {
                @Override
                public void onSuccess(SupabaseClient.User updatedUser) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        
                        // Получаем URL аватарки из ответа
                        String avatarUrl = updatedUser.getAvatarUrl();
                        
                        // Если URL не пустой, сохраняем в сессии
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            sessionManager.updateUserAvatarUrl(avatarUrl);
                            
                            // Обновляем интерфейс с оптимальными настройками
                            loadProfileImage(avatarUrl);
                        }
                        
                        // Сообщаем пользователю об успешном обновлении
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "Аватарка успешно обновлена",
                            Snackbar.LENGTH_LONG
                        ).show();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showUpdateError(errorMessage);
                    });
                }
            }
        );
    }
    
    /**
     * Получает размер изображения в байтах
     */
    private long getImageSize(Uri imageUri) {
        try {
            return getContentResolver().openFileDescriptor(imageUri, "r").getStatSize();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Обновляет никнейм пользователя
     */
    private void updateUsername() {
        String newUsername = usernameInput.getText().toString().trim();
        
        // Проверка введенного имени
        if (TextUtils.isEmpty(newUsername)) {
            usernameLayout.setError("Введите никнейм");
            return;
        }
        
        if (newUsername.length() < 3) {
            usernameLayout.setError("Никнейм должен содержать минимум 3 символа");
            return;
        }
        
        usernameLayout.setError(null);
        showLoading(true);
        
        // Получаем необходимые данные из сессии
        String userId = sessionManager.getUserId();
        String token = sessionManager.getAccessToken();
        
        // Выполняем запрос на обновление профиля
        SupabaseClient.getInstance().updateUserProfile(
            userId,
            newUsername,
            token,
            new SupabaseClient.ProfileUpdateCallback() {
                @Override
                public void onSuccess(SupabaseClient.User updatedUser) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        
                        // Получаем имя пользователя из ответа
                        String userName = updatedUser.getName();
                        
                        // Убеждаемся, что имя не пустое
                        if (TextUtils.isEmpty(userName)) {
                            userName = newUsername;
                        }
                        
                        // Обновляем данные в сессии
                        sessionManager.updateUserName(userName);
                        
                        // Обновляем отображение имени в UI
                        userNameText.setText(userName);
                        
                        // Принудительно обновляем поле ввода, чтобы оно показывало обновленное имя
                        usernameInput.setText(userName);
                        
                        // Сообщаем пользователю об успешном обновлении
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "Никнейм успешно обновлен на: " + userName,
                            Snackbar.LENGTH_LONG
                        ).show();
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        showUpdateError(errorMessage);
                    });
                }
            }
        );
    }
    
    /**
     * Показывает сообщение об ошибке обновления профиля
     */
    private void showUpdateError(String errorMessage) {
        String userFriendlyMessage;
        
        if (errorMessage.contains("Configuration error")) {
            userFriendlyMessage = "Не удалось подключиться к серверу. Пожалуйста, сообщите разработчикам об этой проблеме.";
        } 
        else if (errorMessage.contains("Network error") || errorMessage.contains("timeout")) {
            userFriendlyMessage = "Проверьте подключение к интернету и попробуйте снова.";
        }
        else if (errorMessage.contains("token") || errorMessage.contains("auth")) {
            userFriendlyMessage = "Ошибка авторизации. Пожалуйста, выйдите из аккаунта и войдите снова.";
        }
        else {
            userFriendlyMessage = "Не удалось обновить профиль. Пожалуйста, попробуйте позже.";
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ошибка обновления")
               .setMessage(userFriendlyMessage)
               .setPositiveButton("Понятно", null)
               .show();
    }
    
    /**
     * Показывает ошибку в виде Toast
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Показывает или скрывает индикатор загрузки
     */
    private void showLoading(boolean isLoading) {
        if (profileProgressOverlay != null) {
            profileProgressOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        
        // Блокируем кнопки во время загрузки
        saveUsernameButton.setEnabled(!isLoading);
        logoutButton.setEnabled(!isLoading);
        profileImage.setEnabled(!isLoading);
    }
} 