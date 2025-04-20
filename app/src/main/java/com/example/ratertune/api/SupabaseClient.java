package com.example.ratertune.api;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.ratertune.BuildConfig;
import com.example.ratertune.models.PopularUser;
import com.example.ratertune.models.Review;
import com.example.ratertune.utils.Config;
import com.example.ratertune.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;


// Клиент для взаимодействия с Supabase API
public class SupabaseClient {
    private static final String TAG = "SupabaseClient";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static SupabaseClient instance;
    private final OkHttpClient client;
    private String supabaseUrl;
    private String supabaseKey;
    private final Gson gson;
    private boolean isConfigValid = false;
    private SessionManager sessionManager;

    public interface ReviewsCallback {
        void onSuccess(List<Review> reviews);
        void onError(String error);
    }

    public interface ReviewCallback {
        void onSuccess(Review review);
        void onError(String error);
    }

    private SupabaseClient() {
        // Получаем URL и ключ из конфигурационного файла
        supabaseUrl = Config.get("SUPABASE_URL");
        supabaseKey = Config.get("SUPABASE_KEY");

        // Если URL или ключ отсутствуют, попробуем получить из BuildConfig
        if (supabaseUrl == null || supabaseKey == null) {
            Log.w(TAG, "Trying to get credentials from BuildConfig");
            try {
                // Используем значения из BuildConfig если они определены
                supabaseUrl = BuildConfig.SUPABASE_URL;
                supabaseKey = BuildConfig.SUPABASE_KEY;
                
                // Проверка на временные placeholder значения из BuildConfig
            } catch (Exception e) {
                Log.e(TAG, "Error accessing BuildConfig fields", e);
            }
        }

        // Финальная проверка URL и ключа
        // Проверяем формат URL, чтобы он начинался с http:// или https://
        if (!supabaseUrl.startsWith("http://") && !supabaseUrl.startsWith("https://")) {
            supabaseUrl = "https://" + supabaseUrl;
            Log.w(TAG, "Added https:// prefix to Supabase URL");
        }
        isConfigValid = true;
        Log.i(TAG, "Supabase configuration loaded successfully: " + supabaseUrl);

        // Настраиваем HTTP-клиент с логированием
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        gson = new Gson();
    }

    // Получает единственный экземпляр клиента
    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    // Устанавливает менеджер сессии
    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    // Интерфейс для обратного вызова авторизации
    public interface AuthCallback {
        void onSuccess(AuthResponse response);
        void onError(String errorMessage);
    }

    // Регистрирует нового пользователя
    public void signUp(String email, String password, AuthCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                JsonObject requestJson = new JsonObject();
                requestJson.addProperty("email", email);
                requestJson.addProperty("password", password);

                Request request = new Request.Builder()
                        .url(supabaseUrl + "/auth/v1/signup")
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(requestJson.toString(), JSON))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    if (response.isSuccessful() && !responseBody.isEmpty()) {
                        AuthResponse authResponse = gson.fromJson(responseBody, AuthResponse.class);
                        if (authResponse != null && authResponse.getUser() != null) {
                            callback.onSuccess(authResponse);
                        } else {
                            callback.onError("Registration failed: Invalid response format");
                        }
                    } else {
                        String errorMessage = parseErrorMessage(responseBody, response.code());
                        callback.onError("Registration failed: " + errorMessage);
                    }
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "Network error during signup: Unknown host", e);
                callback.onError("Network error: Не удалось подключиться к серверу. Проверьте интернет-соединение.");
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Network timeout during signup", e);
                callback.onError("Network error: Сервер не отвечает. Попробуйте позже.");
            } catch (IOException e) {
                Log.e(TAG, "Error during signup", e);
                callback.onError("Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during signup", e);
                callback.onError("Unexpected error: " + e.getMessage());
            }
        }).start();
    }

    // Выполняет вход существующего пользователя
    public void signIn(String email, String password, AuthCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                JsonObject requestJson = new JsonObject();
                requestJson.addProperty("email", email);
                requestJson.addProperty("password", password);

                Request request = new Request.Builder()
                        .url(supabaseUrl + "/auth/v1/token?grant_type=password")
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(requestJson.toString(), JSON))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    if (response.isSuccessful() && !responseBody.isEmpty()) {
                        AuthResponse authResponse = gson.fromJson(responseBody, AuthResponse.class);
                        if (authResponse != null && authResponse.getUser() != null) {
                            callback.onSuccess(authResponse);
                        } else {
                            callback.onError("Login failed: Invalid response format");
                        }
                    } else {
                        String errorMessage = parseErrorMessage(responseBody, response.code());
                        callback.onError("Login failed: " + errorMessage);
                    }
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "Network error during signin: Unknown host", e);
                callback.onError("Network error: Не удалось подключиться к серверу. Проверьте интернет-соединение.");
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Network timeout during signin", e);
                callback.onError("Network error: Сервер не отвечает. Попробуйте позже.");
            } catch (IOException e) {
                Log.e(TAG, "Error during signin", e);
                callback.onError("Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during signin", e);
                callback.onError("Unexpected error: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Обновляет данные профиля пользователя
     * @param userId ID пользователя
     * @param newUsername новое имя пользователя
     * @param token токен доступа
     * @param callback обратный вызов с результатом операции
     */
    public void updateUserProfile(String userId, String newUsername, String token, ProfileUpdateCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Подготавливаем данные для обновления
                JsonObject userMetadata = new JsonObject();
                userMetadata.addProperty("name", newUsername);
                
                JsonObject requestJson = new JsonObject();
                requestJson.add("data", userMetadata);

                // Логируем запрос
                Log.d(TAG, "Update profile request: " + requestJson.toString());

                // Создаем запрос к Supabase API
                Request request = new Request.Builder()
                        .url(supabaseUrl + "/auth/v1/user")
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .put(RequestBody.create(requestJson.toString(), JSON))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    Log.d(TAG, "Update profile response: " + responseBody);
                    
                    if (response.isSuccessful() && !responseBody.isEmpty()) {
                        // Создаем временный объект пользователя с новым именем
                        // Это нужно, т.к. API может не вернуть обновленные метаданные сразу
                        User tempUser = new User();
                        
                        try {
                            // Пробуем получить данные из ответа
                            User updatedUser = gson.fromJson(responseBody, User.class);
                            
                            if (updatedUser != null) {
                                // Логируем метаданные для отладки
                                Log.d(TAG, "User metadata from response: " + updatedUser.getMetadataDebug());
                                
                                // Проверяем, есть ли имя в метаданных
                                String userName = updatedUser.getName();
                                if (userName != null && !userName.equals(newUsername)) {
                                    // Если имя из метаданных не совпадает с запрошенным,
                                    // создаем объект с принудительно установленным именем
                                    Log.w(TAG, "Name in response (" + userName + ") doesn't match requested name (" + newUsername + ")");
                                    tempUser = updatedUser;
                                    
                                    // Создаем метаданные с новым именем
                                    JsonObject metadata = new JsonObject();
                                    metadata.addProperty("name", newUsername);
                                    
                                    try {
                                        // Используем рефлексию для установки метаданных
                                        java.lang.reflect.Field field = User.class.getDeclaredField("metadata");
                                        field.setAccessible(true);
                                        field.set(tempUser, metadata);
                                    } catch (Exception e) {
                                        Log.e(TAG, "Failed to set metadata via reflection", e);
                                    }
                                } else {
                                    // Имя совпадает, используем полученный объект
                                    tempUser = updatedUser;
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing user from response", e);
                        }
                        
                        // Если у пользователя нет ID или email, значит что-то пошло не так
                        if (tempUser.getId() == null || tempUser.getEmail() == null) {
                            // Создаем пользователя вручную с минимальными данными
                            Log.w(TAG, "Creating manual user object");
                            final User manualUser = new User() {
                                @Override
                                public String getId() {
                                    return userId;
                                }
                                
                                @Override
                                public String getEmail() {
                                    return "user@example.com";
                                }
                                
                                @Override
                                public String getName() {
                                    return newUsername;
                                }
                            };
                            callback.onSuccess(manualUser);
                        } else {
                            callback.onSuccess(tempUser);
                        }
                    } else {
                        String errorMessage = parseErrorMessage(responseBody, response.code());
                        callback.onError("Profile update failed: " + errorMessage);
                    }
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "Network error during profile update: Unknown host", e);
                callback.onError("Network error: Не удалось подключиться к серверу. Проверьте интернет-соединение.");
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Network timeout during profile update", e);
                callback.onError("Network error: Сервер не отвечает. Попробуйте позже.");
            } catch (IOException e) {
                Log.e(TAG, "Error during profile update", e);
                callback.onError("Network error: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during profile update", e);
                callback.onError("Unexpected error: " + e.getMessage());
            }
        }).start();
    }
    
    // Интерфейс для обратного вызова обновления профиля
    public interface ProfileUpdateCallback {
        void onSuccess(User updatedUser);
        void onError(String errorMessage);
    }

    // Модель ответа аутентификации от Supabase
    public static class AuthResponse {
        @SerializedName("access_token")
        private String accessToken;
        
        @SerializedName("refresh_token")
        private String refreshToken;
        
        @SerializedName("user")
        private User user;
        
        public String getAccessToken() {
            return accessToken;
        }
        
        public String getRefreshToken() {
            return refreshToken;
        }
        
        public User getUser() {
            return user;
        }
    }
    
    // Модель данных пользователя
    public static class User {
        @SerializedName("id")
        private String id;
        
        @SerializedName("email")
        private String email;
        
        // Свойство для имени пользователя (может быть в метаданных)
        @SerializedName("user_metadata")
        private JsonObject metadata;
        
        @SerializedName("updated_at")
        private String updatedAt;
        
        public String getId() {
            return id;
        }
        
        public String getEmail() {
            return email;
        }
        
        public String getName() {
            try {
                // Проверяем наличие метаданных и имени в них
                if (metadata != null && metadata.has("name") && !metadata.get("name").isJsonNull()) {
                    return metadata.get("name").getAsString();
                }
                
                // Запасной вариант - берем часть email до @
                if (email != null && email.contains("@")) {
                    return email.split("@")[0];
                }
            } catch (Exception e) {
                Log.e("SupabaseClient", "Error getting user name from metadata: " + e.getMessage());
            }
            
            return email != null ? email.split("@")[0] : "User";
        }
        
        // Метод для отладки - выводит все метаданные пользователя
        public String getMetadataDebug() {
            if (metadata != null) {
                return metadata.toString();
            }
            return "No metadata";
        }
        
        public String getAvatarUrl() {
            try {
                // Проверяем наличие метаданных и URL аватарки в них
                if (metadata != null && metadata.has("avatar_url") && !metadata.get("avatar_url").isJsonNull()) {
                    return metadata.get("avatar_url").getAsString();
                }
            } catch (Exception e) {
                Log.e("SupabaseClient", "Error getting avatar URL from metadata: " + e.getMessage());
            }
            
            return null;
        }
    }

    // Извлекает более подробное сообщение об ошибке из ответа сервера
    private String parseErrorMessage(String responseBody, int statusCode) {
        // Если тело ответа пустое, возвращаем код статуса
        if (responseBody == null || responseBody.isEmpty()) {
            return "HTTP error " + statusCode;
        }
        
        try {
            // Пытаемся парсить JSON
            JsonElement element = JsonParser.parseString(responseBody);
            if (element.isJsonObject()) {
                JsonObject jsonObject = element.getAsJsonObject();
                
                // Проверяем различные поля с сообщением об ошибке
                if (jsonObject.has("error")) {
                    JsonElement error = jsonObject.get("error");
                    if (error.isJsonPrimitive()) {
                        return error.getAsString();
                    } else if (error.isJsonObject()) {
                        JsonObject errorObj = error.getAsJsonObject();
                        if (errorObj.has("message")) {
                            return errorObj.get("message").getAsString();
                        }
                    }
                }
                
                if (jsonObject.has("error_description")) {
                    return jsonObject.get("error_description").getAsString();
                }
                
                if (jsonObject.has("message")) {
                    return jsonObject.get("message").getAsString();
                }
                
                if (jsonObject.has("msg")) {
                    return jsonObject.get("msg").getAsString();
                }
            }
            
            // Если не удалось извлечь сообщение, возвращаем сам JSON
            return responseBody;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing error response", e);
            // Если не удалось распарсить JSON, возвращаем статус-код и тело ответа
            return "HTTP error " + statusCode + ": " + responseBody;
        }
    }

    // Интерфейс для обратного вызова добавления альбома
    public interface ReleaseCallback {
        void onSuccess(Release release);
        void onError(String errorMessage);
    }
    
    // Интерфейс для обратного вызова получения списка альбомов
    public interface ReleasesListCallback {
        void onSuccess(List<Release> releases);
        void onError(String errorMessage);
    }

    /**
     * Получает список альбомов пользователя из Supabase
     * 
     * @param userId ID пользователя
     * @param token токен доступа
     * @param callback обратный вызов с результатом операции
     */
    public void getUserReleases(String userId, String token, ReleasesListCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Формируем запрос
                String apiUrl = supabaseUrl + "/rest/v1/releases?user_id=eq." + userId + "&order=created_at.desc";
                
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .get()
                        .build();
                
                // Отправляем запрос
                Response response = client.newCall(request).execute();
                
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful() && !responseBody.isEmpty()) {
                    // Парсим ответ
                    Type listType = new TypeToken<List<Release>>(){}.getType();
                    List<Release> releases = gson.fromJson(responseBody, listType);
                    
                    if (releases != null) {
                        callback.onSuccess(releases);
                    } else {
                        callback.onError("Error parsing releases data");
                    }
                } else {
                    String errorMessage = parseErrorMessage(responseBody, response.code());
                    callback.onError("Failed to load releases: " + errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading releases", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Загружает изображение обложки альбома в Supabase Storage
     * и создает запись об альбоме в базе данных
     * 
     * @param title название альбома
     * @param artist исполнитель
     * @param releaseDate дата выпуска
     * @param coverUri URI изображения обложки
     * @param context контекст для доступа к файлам
     * @param userId ID пользователя, добавляющего альбом
     * @param token токен доступа пользователя
     * @param callback обратный вызов с результатом операции
     */
    public void addRelease(String title, String artist, String releaseDate, 
                          Uri coverUri, Context context, String userId, 
                          String token, ReleaseCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Сначала загружаем изображение
                String coverUrl = uploadImage(coverUri, context, token);
                
                if (coverUrl == null) {
                    callback.onError("Failed to upload image");
                    return;
                }
                
                // Затем создаем запись об альбоме
                createReleaseRecord(title, artist, releaseDate, coverUrl, userId, token, callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Error during release addition", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Загружает изображение в Supabase Storage
     * 
     * @param imageUri URI изображения
     * @param context контекст для доступа к файлам
     * @param token токен доступа
     * @return URL загруженного изображения или null в случае ошибки
     */
    private String uploadImage(Uri imageUri, Context context, String token) {
        try {
            // Получаем файл из URI
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "Cannot open input stream for image: " + imageUri);
                return null;
            }
            
            // Получаем имя файла
            String fileName = "cover_" + System.currentTimeMillis() + ".jpg";
            
            // Читаем изображение в байтовый массив
            byte[] imageData = readBytes(inputStream);
            inputStream.close();
            
            // Создаем запрос для загрузки файла
            String storageUrl = supabaseUrl + "/storage/v1/object/covers/" + fileName;
            
            RequestBody requestBody = RequestBody.create(imageData, MediaType.parse("image/jpeg"));
            
            Request request = new Request.Builder()
                    .url(storageUrl)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "image/jpeg")
                    .post(requestBody)
                    .build();
            
            // Отправляем запрос
            Response response = client.newCall(request).execute();
            
            if (!response.isSuccessful()) {
                Log.e(TAG, "Failed to upload image: " + response.code() + " - " + 
                      (response.body() != null ? response.body().string() : "No response body"));
                return null;
            }
            
            // Возвращаем URL загруженного изображения
            return supabaseUrl + "/storage/v1/object/covers/" + fileName;
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading image", e);
            return null;
        }
    }
    
    // Читает данные из InputStream в байтовый массив
    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        
        return byteBuffer.toByteArray();
    }
    
    // Создает запись об альбоме в базе данных
    private void createReleaseRecord(String title, String artist, String releaseDate, 
                                   String coverUrl, String userId, String token, 
                                   ReleaseCallback callback) {
        try {
            // Создаем JSON с данными альбома
            JsonObject releaseJson = new JsonObject();
            releaseJson.addProperty("title", title);
            releaseJson.addProperty("artist", artist);
            releaseJson.addProperty("release_date", releaseDate);
            releaseJson.addProperty("cover_url", coverUrl);
            releaseJson.addProperty("user_id", userId);
            
            // Формируем запрос
            String apiUrl = supabaseUrl + "/rest/v1/releases";
            
            RequestBody requestBody = RequestBody.create(releaseJson.toString(), JSON);
            
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .post(requestBody)
                    .build();
            
            // Отправляем запрос
            Response response = client.newCall(request).execute();
            
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (response.isSuccessful() && !responseBody.isEmpty()) {
                // Парсим ответ
                // Проверяем, является ли ответ массивом
                if (responseBody.startsWith("[")) {
                    // Если это массив, извлекаем первый элемент
                    Type listType = new TypeToken<List<Release>>(){}.getType();
                    List<Release> releases = gson.fromJson(responseBody, listType);
                    if (releases != null && !releases.isEmpty()) {
                        callback.onSuccess(releases.get(0));
                    } else {
                        callback.onError("Empty release data received");
                    }
                } else {
                    // Если это не массив, обрабатываем как раньше
                    Release release = gson.fromJson(responseBody, Release.class);
                    callback.onSuccess(release);
                }
            } else {
                String errorMessage = parseErrorMessage(responseBody, response.code());
                callback.onError("Failed to create release record: " + errorMessage);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating release record", e);
            callback.onError("Error: " + e.getMessage());
        }
    }
    
    // Модель данных альбома
    public static class Release {
        @SerializedName("id")
        private int id;
        
        @SerializedName("title")
        private String title;
        
        @SerializedName("artist")
        private String artist;
        
        @SerializedName("release_date")
        private String releaseDate;
        
        @SerializedName("cover_url")
        private String coverUrl;
        
        @SerializedName("user_id")
        private String userId;
        
        @SerializedName("created_at")
        private String createdAt;
        
        public int getId() {
            return id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getArtist() {
            return artist;
        }
        
        public String getReleaseDate() {
            return releaseDate;
        }
        
        public String getCoverUrl() {
            return coverUrl;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public String getCreatedAt() {
            return createdAt;
        }
    }

    /**
     * Обновляет аватарку пользователя
     * 
     * @param userId ID пользователя
     * @param avatarUri URI изображения аватарки
     * @param context контекст для доступа к файлам
     * @param token токен доступа
     * @param callback обратный вызов с результатом операции
     */
    public void updateUserAvatar(String userId, Uri avatarUri, Context context, String token, ProfileUpdateCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Проверяем, существует ли бакет 'avatars', если нет - нужно создать его в панели Supabase
                
                // Загружаем изображение в хранилище Supabase
                String fileName = "avatar_" + userId + "_" + System.currentTimeMillis() + ".jpg";
                String avatarUrl = uploadImageToStorage(avatarUri, context, token, "avatars", fileName);
                
                if (avatarUrl == null) {
                    callback.onError("Failed to upload avatar image");
                    return;
                }
                
                // Обновляем метаданные пользователя с ссылкой на аватарку
                JsonObject userMetadata = new JsonObject();
                // Сохраняем текущее имя пользователя, если оно есть
                String currentName = getUserNameFromToken(token);
                if (currentName != null && !currentName.isEmpty()) {
                    userMetadata.addProperty("name", currentName);
                }
                userMetadata.addProperty("avatar_url", avatarUrl);
                
                JsonObject requestJson = new JsonObject();
                requestJson.add("data", userMetadata);

                // Создаем запрос к Supabase API
                Request request = new Request.Builder()
                        .url(supabaseUrl + "/auth/v1/user")
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .put(RequestBody.create(requestJson.toString(), JSON))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    if (response.isSuccessful() && !responseBody.isEmpty()) {
                        // Парсим ответ и извлекаем обновленные данные пользователя
                        User updatedUser = gson.fromJson(responseBody, User.class);
                        
                        // Если ответ успешный, но данные пользователя не получены
                        if (updatedUser == null) {
                            User tempUser = new User() {
                                @Override
                                public String getId() {
                                    return userId;
                                }
                                
                                @Override
                                public String getAvatarUrl() {
                                    return avatarUrl;
                                }
                            };
                            callback.onSuccess(tempUser);
                        } else {
                            // Если метаданные не содержат avatar_url, добавляем его вручную
                            if (updatedUser.getAvatarUrl() == null) {
                                try {
                                    if (updatedUser.metadata == null) {
                                        updatedUser.metadata = new JsonObject();
                                    }
                                    updatedUser.metadata.addProperty("avatar_url", avatarUrl);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error updating avatar_url in metadata", e);
                                }
                            }
                            
                            callback.onSuccess(updatedUser);
                        }
                    } else {
                        String errorMessage = parseErrorMessage(responseBody, response.code());
                        callback.onError("Avatar update failed: " + errorMessage);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating avatar", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Загружает изображение в указанный бакет Supabase Storage
     * 
     * @param imageUri URI изображения
     * @param context контекст для доступа к файлам
     * @param token токен доступа
     * @param bucket название бакета
     * @param fileName имя файла
     * @return URL загруженного изображения или null в случае ошибки
     */
    private String uploadImageToStorage(Uri imageUri, Context context, String token, String bucket, String fileName) {
        try {
            // Получаем файл из URI
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "Cannot open input stream for image: " + imageUri);
                return null;
            }
            
            // Читаем изображение в байтовый массив
            byte[] imageData = readBytes(inputStream);
            inputStream.close();
            
            // Создаем запрос для загрузки файла
            String storageUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName;
            
            RequestBody requestBody = RequestBody.create(imageData, MediaType.parse("image/jpeg"));
            
            Request request = new Request.Builder()
                    .url(storageUrl)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "image/jpeg")
                    .post(requestBody)
                    .build();
            
            // Отправляем запрос
            Response response = client.newCall(request).execute();
            
            if (!response.isSuccessful()) {
                Log.e(TAG, "Failed to upload image: " + response.code() + " - " + 
                      (response.body() != null ? response.body().string() : "No response body"));
                return null;
            }
            
            // Возвращаем URL загруженного изображения
            return supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName;
            
        } catch (Exception e) {
            Log.e(TAG, "Error uploading image", e);
            return null;
        }
    }

    // Извлекает имя пользователя из JWT токена
    private String getUserNameFromToken(String token) {
        try {
            // JWT состоит из трех частей, разделенных точкой
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            
            // Декодируем payload (вторую часть токена)
            String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE));
            JsonObject jsonPayload = JsonParser.parseString(payload).getAsJsonObject();
            
            // Проверяем наличие метаданных пользователя в токене
            if (jsonPayload.has("user_metadata") && !jsonPayload.get("user_metadata").isJsonNull()) {
                JsonObject userMetadata = jsonPayload.getAsJsonObject("user_metadata");
                if (userMetadata.has("name") && !userMetadata.get("name").isJsonNull()) {
                    return userMetadata.get("name").getAsString();
                }
            }
            
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting name from token", e);
            return null;
        }
    }

    public void getReviews(String releaseId, String token, ReviewsCallback callback) {
        new Thread(() -> {
            try {
                // Сначала получаем информацию о релизе
                String releaseUrl = supabaseUrl + "/rest/v1/releases?id=eq." + releaseId;
                URL releaseApiUrl = new URL(releaseUrl);
                HttpURLConnection releaseConnection = (HttpURLConnection) releaseApiUrl.openConnection();
                releaseConnection.setRequestMethod("GET");
                releaseConnection.setRequestProperty("apikey", supabaseKey);
                releaseConnection.setRequestProperty("Authorization", "Bearer " + token);
                releaseConnection.setRequestProperty("Content-Type", "application/json");
                
                String releaseName = null;
                // Получаем информацию о релизе
                int releaseResponseCode = releaseConnection.getResponseCode();
                if (releaseResponseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader releaseReader = new BufferedReader(new InputStreamReader(releaseConnection.getInputStream()));
                    StringBuilder releaseResponse = new StringBuilder();
                    String releaseLine;
                    while ((releaseLine = releaseReader.readLine()) != null) {
                        releaseResponse.append(releaseLine);
                    }
                    releaseReader.close();
                    
                    String releaseResponseStr = releaseResponse.toString();
                    JSONArray releaseArray = new JSONArray(releaseResponseStr);
                    if (releaseArray.length() > 0) {
                        JSONObject releaseJson = releaseArray.getJSONObject(0);
                        String title = releaseJson.getString("title");
                        String artist = releaseJson.getString("artist");
                        releaseName = artist + " - " + title;
                        Log.d(TAG, "Got release info: " + releaseName);
                    }
                }
                
                // Теперь получаем рецензии
                String url = supabaseUrl + "/rest/v1/reviews?release_id=eq." + releaseId;
                Log.d(TAG, "Getting reviews for release: " + releaseId);
                
                URL apiUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("apikey", supabaseKey);
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Prefer", "return=representation");

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Reviews API response code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String responseStr = response.toString();
                    Log.d(TAG, "Reviews API raw response: " + responseStr);
                    
                    JSONArray jsonArray = new JSONArray(responseStr);
                    List<Review> reviews = new ArrayList<>();
                    
                    Log.d(TAG, "Got " + jsonArray.length() + " reviews");
                    
                    final String finalReleaseName = releaseName;
                    
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        
                        // Логируем все поля объекта
                        Log.d(TAG, "Review " + i + " full JSON: " + json.toString());
                        
                        String createdAt = null;
                        if (json.has("created_at") && !json.isNull("created_at")) {
                            createdAt = json.getString("created_at");
                            Log.d(TAG, "Review " + i + " created_at: " + createdAt);
                        } else {
                            Log.w(TAG, "Review " + i + " has no created_at field!");
                        }
                        
                        String updatedAt = null;
                        if (json.has("updated_at") && !json.isNull("updated_at")) {
                            updatedAt = json.getString("updated_at");
                            Log.d(TAG, "Review " + i + " updated_at: " + updatedAt);
                        }
                        
                        Review review = new Review(
                            json.getLong("id"),
                            json.getString("user_id"),
                            json.getString("user_name"),
                            json.optString("user_avatar_url", null),
                            json.getString("release_id"),
                            finalReleaseName,
                            (float) json.getDouble("rating"),
                            json.getString("text"),
                            createdAt,
                            updatedAt
                        );
                        
                        Log.d(TAG, "Review object created with date: " + review.getCreatedAt());
                        reviews.add(review);
                    }
                    
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(reviews));
                } else {
                    // Получаем сообщение об ошибке из ответа
                    String errorMessage = "";
                    try {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getErrorStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        errorMessage = response.toString();
                        Log.e(TAG, "Error response: " + errorMessage);
                    } catch (Exception e) {
                        Log.e(TAG, "Could not read error stream", e);
                    }
                    
                    final String finalErrorMessage = errorMessage;
                    new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onError("Failed to get reviews. Response code: " + responseCode + 
                                        (finalErrorMessage.isEmpty() ? "" : " - " + finalErrorMessage)));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting reviews", e);
                new Handler(Looper.getMainLooper()).post(() -> 
                    callback.onError("Error getting reviews: " + e.getMessage()));
            }
        }).start();
    }

    public void addReview(String releaseId, float rating, String text, String token, ReviewCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Сначала получаем информацию о релизе
                String releaseName = null;
                String releaseUrl = supabaseUrl + "/rest/v1/releases?id=eq." + releaseId;
                URL releaseApiUrl = new URL(releaseUrl);
                HttpURLConnection releaseConnection = (HttpURLConnection) releaseApiUrl.openConnection();
                releaseConnection.setRequestMethod("GET");
                releaseConnection.setRequestProperty("apikey", supabaseKey);
                releaseConnection.setRequestProperty("Authorization", "Bearer " + token);
                releaseConnection.setRequestProperty("Content-Type", "application/json");
                
                int releaseResponseCode = releaseConnection.getResponseCode();
                if (releaseResponseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader releaseReader = new BufferedReader(new InputStreamReader(releaseConnection.getInputStream()));
                    StringBuilder releaseResponse = new StringBuilder();
                    String releaseLine;
                    while ((releaseLine = releaseReader.readLine()) != null) {
                        releaseResponse.append(releaseLine);
                    }
                    releaseReader.close();
                    
                    String releaseResponseStr = releaseResponse.toString();
                    JSONArray releaseArray = new JSONArray(releaseResponseStr);
                    if (releaseArray.length() > 0) {
                        JSONObject releaseJson = releaseArray.getJSONObject(0);
                        String title = releaseJson.getString("title");
                        String artist = releaseJson.getString("artist");
                        releaseName = artist + " - " + title;
                        Log.d(TAG, "Got release info for new review: " + releaseName);
                    }
                }
                
                // Получаем текущего пользователя
                String userId = getCurrentUserId();
                String userName = getCurrentUserName();
                String userAvatarUrl = getCurrentUserAvatarUrl();
                
                // Создаем JSON с данными рецензии
                JsonObject reviewJson = new JsonObject();
                reviewJson.addProperty("user_id", userId);
                reviewJson.addProperty("user_name", userName);
                reviewJson.addProperty("user_avatar_url", userAvatarUrl);
                reviewJson.addProperty("release_id", releaseId);
                reviewJson.addProperty("rating", rating);
                reviewJson.addProperty("text", text);
                
                // Формируем запрос
                String apiUrl = supabaseUrl + "/rest/v1/reviews";
                
                RequestBody requestBody = RequestBody.create(reviewJson.toString(), JSON);
                
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=representation")
                        .post(requestBody)
                        .build();
                
                // Отправляем запрос
                Response response = client.newCall(request).execute();
                
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Review creation response: " + responseBody);
                
                if (response.isSuccessful() && !responseBody.isEmpty()) {
                    try {
                        // Проверяем, является ли ответ массивом
                        if (responseBody.trim().startsWith("[")) {
                            // Парсим ответ как массив и берем первый элемент
                            JSONArray jsonArray = new JSONArray(responseBody);
                            if (jsonArray.length() > 0) {
                                JSONObject responseJson = jsonArray.getJSONObject(0);
                                Log.d(TAG, "Review created successfully: " + responseBody);
                                
                                // Создаем объект Review на основе полученных данных
                                Review review = new Review(
                                    responseJson.getLong("id"),
                                    userId,
                                    userName,
                                    userAvatarUrl,
                                    releaseId,
                                    rating,
                                    text,
                                    responseJson.getString("created_at"),
                                    responseJson.optString("updated_at", null)
                                );
                                callback.onSuccess(review);
                            } else {
                                callback.onError("Empty response array");
                            }
                        } else {
                            // Старый код для обработки одиночного объекта
                            JSONObject responseJson = new JSONObject(responseBody);
                            Log.d(TAG, "Review created successfully: " + responseBody);
                            
                            // Создаем объект Review на основе полученных данных
                            Review review = new Review(
                                responseJson.getLong("id"),
                                userId,
                                userName,
                                userAvatarUrl,
                                releaseId,
                                rating,
                                text,
                                responseJson.getString("created_at"),
                                responseJson.optString("updated_at", null)
                            );
                            callback.onSuccess(review);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing review response", e);
                        callback.onError("Error parsing response: " + e.getMessage());
                    }
                } else {
                    String errorMessage = parseErrorMessage(responseBody, response.code());
                    callback.onError("Failed to create review: " + errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error creating review", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    // Получает ID текущего пользователя
    private String getCurrentUserId() {
        if (sessionManager == null) {
            Log.e(TAG, "SessionManager is not initialized");
            return null;
        }
        return sessionManager.getUserId();
    }
    
    // Получает имя текущего пользователя
    private String getCurrentUserName() {
        if (sessionManager == null) {
            Log.e(TAG, "SessionManager is not initialized");
            return null;
        }
        return sessionManager.getUserName();
    }
    
    // Получает URL аватарки текущего пользователя
    private String getCurrentUserAvatarUrl() {
        if (sessionManager == null) {
            Log.e(TAG, "SessionManager is not initialized");
            return null;
        }
        return sessionManager.getUserAvatarUrl();
    }

    // Добавляет новый сториз
    public void addStory(String text, Uri imageUri, Context context, String userId, String token, Date expiresAt, StoryCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Сначала загружаем изображение
                String imageUrl = uploadImageToStorage(imageUri, context, token, "stories", "story_" + System.currentTimeMillis() + ".jpg");
                
                if (imageUrl == null) {
                    callback.onError("Failed to upload image");
                    return;
                }
                
                // Затем создаем запись о сторизе
                createStoryRecord(text, imageUrl, userId, token, expiresAt, callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Error during story addition", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    // Создает запись о сторизе в базе данных
    private void createStoryRecord(String text, String imageUrl, String userId, String token, Date expiresAt, StoryCallback callback) {
        try {
            // Создаем JSON с данными сториза
            JsonObject storyJson = new JsonObject();
            storyJson.addProperty("text", text);
            storyJson.addProperty("image_url", imageUrl);
            storyJson.addProperty("user_id", userId);
            
            // Форматируем дату в ISO 8601
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            storyJson.addProperty("expires_at", sdf.format(expiresAt));
            
            // Формируем запрос
            String apiUrl = supabaseUrl + "/rest/v1/stories";
            
            RequestBody requestBody = RequestBody.create(storyJson.toString(), JSON);
            
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=representation")
                    .post(requestBody)
                    .build();
            
            // Отправляем запрос
            Response response = client.newCall(request).execute();
            
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (response.isSuccessful() && !responseBody.isEmpty()) {
                try {
                    // Проверяем, является ли ответ массивом
                    if (responseBody.trim().startsWith("[")) {
                        // Парсим ответ как массив и берем первый элемент
                        Type listType = new TypeToken<List<com.example.ratertune.models.Story>>(){}.getType();
                        List<com.example.ratertune.models.Story> stories = gson.fromJson(responseBody, listType);
                        if (stories != null && !stories.isEmpty()) {
                            callback.onSuccess(stories.get(0));
                        } else {
                            callback.onError("Empty response array");
                        }
                    } else {
                        // Парсим ответ как одиночный объект
                        com.example.ratertune.models.Story modelStory = gson.fromJson(responseBody, com.example.ratertune.models.Story.class);
                        callback.onSuccess(modelStory);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing story response", e);
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            } else {
                String errorMessage = parseErrorMessage(responseBody, response.code());
                callback.onError("Failed to create story record: " + errorMessage);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating story record", e);
            callback.onError("Error: " + e.getMessage());
        }
    }

    // Получает список сторизов пользователя
    public void getUserStories(String userId, String token, StoriesListCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Формируем запрос
                String apiUrl = supabaseUrl + "/rest/v1/stories?user_id=eq." + userId + "&expires_at=gt.now()";
                
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();
                
                // Отправляем запрос
                Response response = client.newCall(request).execute();
                
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful() && !responseBody.isEmpty()) {
                    // Парсим ответ
                    Type listType = new TypeToken<List<com.example.ratertune.models.Story>>(){}.getType();
                    List<com.example.ratertune.models.Story> stories = gson.fromJson(responseBody, listType);
                    callback.onSuccess(stories);
                } else {
                    String errorMessage = parseErrorMessage(responseBody, response.code());
                    callback.onError("Failed to load stories: " + errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading stories", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    public void getLatestReviews(String token, int limit, ReviewsCallback callback) {
        new Thread(() -> {
            try {
                String url = supabaseUrl + "/rest/v1/reviews?order=created_at.desc&limit=" + limit;
                Log.d(TAG, "Getting latest reviews, limit: " + limit);
                
                URL apiUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("apikey", supabaseKey);
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Prefer", "return=representation");

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Latest reviews API response code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String responseStr = response.toString();
                    Log.d(TAG, "Latest reviews API raw response: " + responseStr);
                    
                    JSONArray jsonArray = new JSONArray(responseStr);
                    List<Review> reviews = new ArrayList<>();
                    
                    Log.d(TAG, "Got " + jsonArray.length() + " latest reviews");
                    
                    // Для каждой рецензии загружаем информацию о релизе
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        
                        String createdAt = null;
                        if (json.has("created_at") && !json.isNull("created_at")) {
                            createdAt = json.getString("created_at");
                        }
                        
                        String updatedAt = null;
                        if (json.has("updated_at") && !json.isNull("updated_at")) {
                            updatedAt = json.getString("updated_at");
                        }
                        
                        String releaseId = json.getString("release_id");
                        String releaseName = null;
                        
                        // Получаем информацию о релизе
                        String releaseUrl = supabaseUrl + "/rest/v1/releases?id=eq." + releaseId;
                        URL releaseApiUrl = new URL(releaseUrl);
                        HttpURLConnection releaseConnection = (HttpURLConnection) releaseApiUrl.openConnection();
                        releaseConnection.setRequestMethod("GET");
                        releaseConnection.setRequestProperty("apikey", supabaseKey);
                        releaseConnection.setRequestProperty("Authorization", "Bearer " + token);
                        releaseConnection.setRequestProperty("Content-Type", "application/json");
                        
                        int releaseResponseCode = releaseConnection.getResponseCode();
                        if (releaseResponseCode == HttpURLConnection.HTTP_OK) {
                            BufferedReader releaseReader = new BufferedReader(new InputStreamReader(releaseConnection.getInputStream()));
                            StringBuilder releaseResponse = new StringBuilder();
                            String releaseLine;
                            while ((releaseLine = releaseReader.readLine()) != null) {
                                releaseResponse.append(releaseLine);
                            }
                            releaseReader.close();
                            
                            String releaseResponseStr = releaseResponse.toString();
                            JSONArray releaseArray = new JSONArray(releaseResponseStr);
                            if (releaseArray.length() > 0) {
                                JSONObject releaseJson = releaseArray.getJSONObject(0);
                                String title = releaseJson.getString("title");
                                String artist = releaseJson.getString("artist");
                                releaseName = artist + " - " + title;
                                Log.d(TAG, "Got release info for review " + i + ": " + releaseName);
                            }
                        }
                        
                        Review review = new Review(
                            json.getLong("id"),
                            json.getString("user_id"),
                            json.getString("user_name"),
                            json.optString("user_avatar_url", null),
                            json.getString("release_id"),
                            releaseName,
                            (float) json.getDouble("rating"),
                            json.getString("text"),
                            createdAt,
                            updatedAt
                        );
                        
                        reviews.add(review);
                    }
                    
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(reviews));
                } else {
                    String errorMessage = "";
                    try {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getErrorStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        errorMessage = response.toString();
                    } catch (Exception e) {
                        Log.e(TAG, "Could not read error stream", e);
                    }
                    
                    final String finalErrorMessage = errorMessage;
                    new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onError("Failed to get latest reviews. Response code: " + responseCode + 
                                        (finalErrorMessage.isEmpty() ? "" : " - " + finalErrorMessage)));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting latest reviews", e);
                new Handler(Looper.getMainLooper()).post(() -> 
                    callback.onError("Error getting latest reviews: " + e.getMessage()));
            }
        }).start();
    }

    // Получает список всех сторизов
    public void getAllStories(String token, StoriesListCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Формируем запрос - загружаем все сторизы, которые не истекли
                String apiUrl = supabaseUrl + "/rest/v1/stories?expires_at=gt.now()&order=created_at.desc";
                
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();
                
                // Отправляем запрос
                Response response = client.newCall(request).execute();
                
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful() && !responseBody.isEmpty()) {
                    // Парсим ответ
                    Type listType = new TypeToken<List<com.example.ratertune.models.Story>>(){}.getType();
                    List<com.example.ratertune.models.Story> stories = gson.fromJson(responseBody, listType);
                    callback.onSuccess(stories);
                } else {
                    String errorMessage = parseErrorMessage(responseBody, response.code());
                    callback.onError("Failed to load stories: " + errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading stories", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    // Получает список всех релизов из Supabase
    public void getAllReleases(String token, ReleasesListCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Формируем запрос на получение всех релизов
                String apiUrl = supabaseUrl + "/rest/v1/releases?order=created_at.desc";
                
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();
                
                // Отправляем запрос
                Response response = client.newCall(request).execute();
                
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful() && !responseBody.isEmpty()) {
                    // Парсим JSON
                    Type listType = new TypeToken<List<Release>>(){}.getType();
                    List<Release> releases = gson.fromJson(responseBody, listType);
                    callback.onSuccess(releases);
                } else {
                    String errorMessage = parseErrorMessage(responseBody, response.code());
                    callback.onError("Failed to load releases: " + errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading releases", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Получает список релизов за определенный период времени
     *
     * @param startDate дата начала периода в формате YYYY-MM-DD
     * @param endDate дата окончания периода в формате YYYY-MM-DD
     * @param token токен доступа
     * @param callback обратный вызов с результатом операции
     */
    public void getReleasesForDateRange(String startDate, String endDate, String token, ReleasesListCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Формируем запрос на получение релизов за указанный период
                String apiUrl = supabaseUrl + "/rest/v1/releases?release_date=gte." + startDate +
                               "&release_date=lte." + endDate + "&order=release_date.desc";
                
                Log.d(TAG, "Getting releases for date range: " + startDate + " to " + endDate);
                
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();
                
                // Отправляем запрос
                Response response = client.newCall(request).execute();
                
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful() && !responseBody.isEmpty()) {
                    // Парсим JSON
                    Type listType = new TypeToken<List<Release>>(){}.getType();
                    List<Release> releases = gson.fromJson(responseBody, listType);
                    callback.onSuccess(releases);
                } else {
                    String errorMessage = parseErrorMessage(responseBody, response.code());
                    callback.onError("Failed to load releases: " + errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading releases for date range", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    // Помечает сториз как просмотренный
    public void markStoryAsViewed(String storyId, String userId, String token, SimpleCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Проверяем, не помечен ли сториз уже как просмотренный
                String checkUrl = supabaseUrl + "/rest/v1/viewed_stories?story_id=eq." + storyId + "&user_id=eq." + userId;
                
                Request checkRequest = new Request.Builder()
                        .url(checkUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();
                
                Response checkResponse = client.newCall(checkRequest).execute();
                
                String checkResponseBody = checkResponse.body() != null ? checkResponse.body().string() : "";
                
                // Если сториз уже помечен как просмотренный, просто возвращаем успех
                if (checkResponse.isSuccessful() && !checkResponseBody.isEmpty() && !checkResponseBody.equals("[]")) {
                    callback.onSuccess();
                    return;
                }
                
                // Если сториз еще не помечен как просмотренный, создаем запись
                JsonObject recordJson = new JsonObject();
                recordJson.addProperty("story_id", storyId);
                recordJson.addProperty("user_id", userId);
                
                String apiUrl = supabaseUrl + "/rest/v1/viewed_stories";
                
                RequestBody requestBody = RequestBody.create(recordJson.toString(), JSON);
                
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build();
                
                Response response = client.newCall(request).execute();
                
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    String errorMessage = parseErrorMessage(response.body() != null ? response.body().string() : "", response.code());
                    Log.e(TAG, "Failed to mark story as viewed: " + errorMessage);
                    callback.onError("Failed to mark story as viewed: " + errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error marking story as viewed", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    // Проверяет, просмотрен ли сториз пользователем
    public void isStoryViewed(String storyId, String userId, String token, ViewedCheckCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                String apiUrl = supabaseUrl + "/rest/v1/viewed_stories?story_id=eq." + storyId + "&user_id=eq." + userId;
                
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();
                
                Response response = client.newCall(request).execute();
                
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful()) {
                    // Если ответ содержит данные (не пустой массив), сториз просмотрен
                    boolean isViewed = !responseBody.isEmpty() && !responseBody.equals("[]");
                    callback.onResult(isViewed);
                } else {
                    String errorMessage = parseErrorMessage(responseBody, response.code());
                    Log.e(TAG, "Failed to check if story is viewed: " + errorMessage);
                    callback.onError("Failed to check if story is viewed: " + errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error checking if story is viewed", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    // Получает список просмотренных сторизов для пользователя
    public void getViewedStoriesForUser(String userId, String token, ViewedStoriesCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                String apiUrl = supabaseUrl + "/rest/v1/viewed_stories?user_id=eq." + userId + "&select=story_id";
                
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();
                
                Response response = client.newCall(request).execute();
                
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful() && !responseBody.isEmpty()) {
                    try {
                        // Парсим ответ - список объектов с полем story_id
                        Type listType = new TypeToken<List<ViewedStory>>(){}.getType();
                        List<ViewedStory> viewedStories = gson.fromJson(responseBody, listType);
                        
                        // Преобразуем список объектов в список ID
                        List<String> storyIds = new ArrayList<>();
                        for (ViewedStory viewedStory : viewedStories) {
                            storyIds.add(viewedStory.getStoryId());
                        }
                        
                        callback.onSuccess(storyIds);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing viewed stories", e);
                        callback.onError("Error parsing viewed stories: " + e.getMessage());
                    }
                } else {
                    String errorMessage = parseErrorMessage(responseBody, response.code());
                    Log.e(TAG, "Failed to get viewed stories: " + errorMessage);
                    callback.onError("Failed to get viewed stories: " + errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting viewed stories", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
    
    // Класс для представления записи о просмотренном сторизе
    private static class ViewedStory {
        @SerializedName("story_id")
        private String storyId;
        
        public String getStoryId() {
            return storyId;
        }
    }
    
    // Интерфейс для обратного вызова при проверке просмотра сториза
    public interface ViewedCheckCallback {
        void onResult(boolean isViewed);
        void onError(String errorMessage);
    }
    
    // Интерфейс для обратного вызова при получении списка просмотренных сторизов
    public interface ViewedStoriesCallback {
        void onSuccess(List<String> storyIds);
        void onError(String errorMessage);
    }
    
    // Простой интерфейс для обратного вызова
    public interface SimpleCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    // Получает релиз по ID
    public void getRelease(String releaseId, String token, ReleaseCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Формируем запрос на получение релиза
                String apiUrl = supabaseUrl + "/rest/v1/releases?id=eq." + releaseId;
                
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();
                
                // Отправляем запрос
                Response response = client.newCall(request).execute();
                
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful() && !responseBody.isEmpty()) {
                    // Парсим JSON
                    Type listType = new TypeToken<List<Release>>(){}.getType();
                    List<Release> releases = gson.fromJson(responseBody, listType);
                    
                    if (releases != null && !releases.isEmpty()) {
                        callback.onSuccess(releases.get(0));
                    } else {
                        callback.onError("Релиз не найден");
                    }
                } else {
                    String errorMessage = parseErrorMessage(responseBody, response.code());
                    callback.onError("Failed to load release: " + errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading release", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    // Интерфейс для обратного вызова получения среднего рейтинга
    public interface AverageRatingCallback {
        void onSuccess(float averageRating, int reviewsCount);
        void onError(String errorMessage);
    }

    /**
     * Вычисляет средний рейтинг релиза на основе рецензий
     * 
     * @param releaseId ID релиза
     * @param token токен доступа
     * @param callback обратный вызов с результатом операции
     */
    public void calculateAverageRating(String releaseId, String token, AverageRatingCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }
        
        new Thread(() -> {
            try {
                // Формируем запрос на получение всех рецензий для релиза
                String apiUrl = supabaseUrl + "/rest/v1/reviews?release_id=eq." + releaseId;
                
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();
                
                // Отправляем запрос
                Response response = client.newCall(request).execute();
                
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful() && !responseBody.isEmpty()) {
                    // Парсим ответ
                    Type listType = new TypeToken<List<Review>>(){}.getType();
                    List<Review> reviews = gson.fromJson(responseBody, listType);
                    
                    if (reviews != null && !reviews.isEmpty()) {
                        // Вычисляем средний рейтинг
                        float sum = 0;
                        for (Review review : reviews) {
                            sum += review.getRating();
                        }
                        float averageRating = sum / reviews.size();
                        callback.onSuccess(averageRating, reviews.size());
                    } else {
                        // Если рецензий нет, рейтинг 0
                        callback.onSuccess(0, 0);
                    }
                } else {
                    String errorMessage = parseErrorMessage(responseBody, response.code());
                    callback.onError("Failed to load reviews: " + errorMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error calculating average rating", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    // Проверяет, поставлен ли лайк пользователем на рецензию
    public void isReviewLikedByUser(long reviewId, String token, LikeCheckCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }

        if (token == null || token.isEmpty()) {
            callback.onError("Authentication error: Token is missing");
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            callback.onError("Authentication error: User ID is missing");
            return;
        }

        new Thread(() -> {
            try {
                String queryUrl = supabaseUrl + "/rest/v1/review_likes?user_id=eq." + userId
                        + "&review_id=eq." + reviewId + "&select=id";

                Request request = new Request.Builder()
                        .url(queryUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonElement jsonElement = JsonParser.parseString(responseBody);
                        
                        if (jsonElement.isJsonArray()) {
                            boolean isLiked = jsonElement.getAsJsonArray().size() > 0;
                            callback.onSuccess(isLiked);
                        } else {
                            callback.onError("Invalid response format");
                        }
                    } else {
                        String errorMessage = "Error: " + response.code();
                        callback.onError(errorMessage);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking if review is liked", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    // Добавляет лайк к рецензии
    public void likeReview(long reviewId, String token, SimpleCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }

        if (token == null || token.isEmpty()) {
            callback.onError("Authentication error: Token is missing");
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            callback.onError("Authentication error: User ID is missing");
            return;
        }

        new Thread(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("user_id", userId);
                requestBody.addProperty("review_id", reviewId);

                Request request = new Request.Builder()
                        .url(supabaseUrl + "/rest/v1/review_likes")
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Prefer", "return=minimal")
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(requestBody.toString(), JSON))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        Log.e(TAG, "Error liking review: " + errorBody);
                        callback.onError("Error: " + response.code());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error liking review", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    // Удаляет лайк с рецензии
    public void unlikeReview(long reviewId, String token, SimpleCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }

        if (token == null || token.isEmpty()) {
            callback.onError("Authentication error: Token is missing");
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            callback.onError("Authentication error: User ID is missing");
            return;
        }

        new Thread(() -> {
            try {
                // Используем параметры запроса в URL для фильтрации записей, которые нужно удалить
                String queryUrl = supabaseUrl + "/rest/v1/review_likes?user_id=eq." + userId + "&review_id=eq." + reviewId;

                // Строим запрос с правильными заголовками
                Request request = new Request.Builder()
                        .url(queryUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=minimal")
                        .delete()  // Используем DELETE без тела запроса
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Successfully unliked review: " + reviewId + " with status " + response.code());
                        callback.onSuccess();
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        Log.e(TAG, "Error unliking review: " + errorBody + ", status: " + response.code());
                        callback.onError("Error: " + response.code() + " - " + errorBody);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error unliking review", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    // Получает количество лайков на рецензии
    public void getReviewLikesCount(long reviewId, String token, LikesCountCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }

        if (token == null || token.isEmpty()) {
            callback.onError("Authentication error: Token is missing");
            return;
        }

        new Thread(() -> {
            try {
                String queryUrl = supabaseUrl + "/rest/v1/review_likes?review_id=eq." + reviewId + "&select=id";

                Request request = new Request.Builder()
                        .url(queryUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonElement jsonElement = JsonParser.parseString(responseBody);
                        
                        if (jsonElement.isJsonArray()) {
                            int likesCount = jsonElement.getAsJsonArray().size();
                            callback.onSuccess(likesCount);
                        } else {
                            callback.onError("Invalid response format");
                        }
                    } else {
                        String errorMessage = "Error: " + response.code();
                        callback.onError(errorMessage);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting review likes count", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    // Интерфейс обратного вызова для проверки лайка
    public interface LikeCheckCallback {
        void onSuccess(boolean isLiked);
        void onError(String errorMessage);
    }

    // Интерфейс обратного вызова для получения количества лайков
    public interface LikesCountCallback {
        void onSuccess(int likesCount);
        void onError(String errorMessage);
    }

    public interface LikeStatusCallback {
        void onSuccess(boolean hasLiked);
        void onError(String errorMessage);
    }

    public interface PopularUsersCallback {
        void onSuccess(List<PopularUser> users);
        void onError(String errorMessage);
    }
    
    // Получает список популярных пользователей на основе количества лайков на их рецензии
    public void getPopularUsers(String token, int limit, PopularUsersCallback callback) {
        // Проверяем валидность конфигурации
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }

        if (token == null || token.isEmpty()) {
            callback.onError("Authentication error: Token is missing");
            return;
        }

        new Thread(() -> {
            try {
                // SQL запрос для получения популярных пользователей с агрегированием данных
                String query = "SELECT r.user_id, r.user_name, r.user_avatar_url, " +
                               "COUNT(DISTINCT rl.id) as likes_count, " +
                               "COUNT(DISTINCT r.id) as reviews_count " +
                               "FROM reviews r " +
                               "LEFT JOIN review_likes rl ON r.id = rl.review_id " +
                               "GROUP BY r.user_id, r.user_name, r.user_avatar_url " +
                               "ORDER BY likes_count DESC, reviews_count DESC " +
                               "LIMIT " + limit;

                String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                String url = supabaseUrl + "/rest/v1/rpc/get_popular_users?query=" + encodedQuery;

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        
                        // Альтернативный вариант запроса - напрямую через SQL функцию в Supabase
                        // Для этого нужно создать функцию get_popular_users в Supabase
                        List<PopularUser> users = parsePopularUsersFromResponse(responseBody);
                        
                        // Если не удалось распарсить через функцию, используем прямой запрос
                        if (users.isEmpty()) {
                            Log.w(TAG, "Failed to use SQL function, using direct query");
                            users = getPopularUsersDirectQuery(token, limit);
                        }
                        
                        callback.onSuccess(users);
                    } else {
                        String errorMessage = "Error: " + response.code();
                        if (response.body() != null) {
                            errorMessage += " - " + response.body().string();
                        }
                        
                        // Если RPC метод не существует, пробуем прямой запрос
                        Log.w(TAG, "RPC method failed: " + errorMessage);
                        List<PopularUser> users = getPopularUsersDirectQuery(token, limit);
                        callback.onSuccess(users);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting popular users", e);
                
                try {
                    // В случае ошибки пробуем прямой запрос
                    List<PopularUser> users = getPopularUsersDirectQuery(token, limit);
                    callback.onSuccess(users);
                } catch (Exception ex) {
                    Log.e(TAG, "Error in fallback query", ex);
                    callback.onError("Error: " + e.getMessage());
                }
            }
        }).start();
    }
    
    // Прямой запрос для получения популярных пользователей без использования RPC
    private List<PopularUser> getPopularUsersDirectQuery(String token, int limit) throws Exception {
        // Сначала получаем данные о рецензиях
        String reviewsUrl = supabaseUrl + "/rest/v1/reviews?select=id,user_id,user_name,user_avatar_url";
        Request reviewsRequest = new Request.Builder()
                .url(reviewsUrl)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + token)
                .get()
                .build();
                
        List<Review> reviews = new ArrayList<>();
        List<PopularUser> result = new ArrayList<>();
                
        try (Response reviewsResponse = client.newCall(reviewsRequest).execute()) {
            if (reviewsResponse.isSuccessful() && reviewsResponse.body() != null) {
                String reviewsBody = reviewsResponse.body().string();
                JSONArray reviewsArray = new JSONArray(reviewsBody);
                
                // Временное хранилище для подсчета лайков и рецензий для каждого пользователя
                Map<String, Integer> reviewsCountMap = new HashMap<>();
                Map<String, Integer> likesCountMap = new HashMap<>();
                Map<String, String> userNameMap = new HashMap<>();
                Map<String, String> userAvatarMap = new HashMap<>();
                
                // Собираем информацию о пользователях и количестве их рецензий
                for (int i = 0; i < reviewsArray.length(); i++) {
                    JSONObject reviewObj = reviewsArray.getJSONObject(i);
                    long reviewId = reviewObj.getLong("id");
                    String userId = reviewObj.getString("user_id");
                    String userName = reviewObj.getString("user_name");
                    String avatarUrl = reviewObj.optString("user_avatar_url", "");
                    
                    // Увеличиваем счетчик рецензий
                    reviewsCountMap.put(userId, reviewsCountMap.getOrDefault(userId, 0) + 1);
                    
                    // Сохраняем имя и аватар пользователя
                    userNameMap.put(userId, userName);
                    userAvatarMap.put(userId, avatarUrl);
                    
                    // Получаем количество лайков для этой рецензии
                    String likesUrl = supabaseUrl + "/rest/v1/review_likes?review_id=eq." + reviewId + "&select=id";
                    Request likesRequest = new Request.Builder()
                            .url(likesUrl)
                            .addHeader("apikey", supabaseKey)
                            .addHeader("Authorization", "Bearer " + token)
                            .get()
                            .build();
                            
                    try (Response likesResponse = client.newCall(likesRequest).execute()) {
                        if (likesResponse.isSuccessful() && likesResponse.body() != null) {
                            String likesBody = likesResponse.body().string();
                            JSONArray likesArray = new JSONArray(likesBody);
                            int likesCount = likesArray.length();
                            
                            // Добавляем количество лайков к общему счету пользователя
                            likesCountMap.put(userId, likesCountMap.getOrDefault(userId, 0) + likesCount);
                        }
                    }
                }
                
                // Создаем список популярных пользователей
                for (String userId : reviewsCountMap.keySet()) {
                    PopularUser user = new PopularUser(
                        userId,
                        userNameMap.getOrDefault(userId, "Пользователь"),
                        userAvatarMap.getOrDefault(userId, ""),
                        likesCountMap.getOrDefault(userId, 0),
                        reviewsCountMap.getOrDefault(userId, 0)
                    );
                    result.add(user);
                }
                
                // Сортируем по количеству лайков и затем по количеству рецензий
                Collections.sort(result, (u1, u2) -> {
                    if (u1.getLikesCount() != u2.getLikesCount()) {
                        return Integer.compare(u2.getLikesCount(), u1.getLikesCount());
                    } else {
                        return Integer.compare(u2.getReviewsCount(), u1.getReviewsCount());
                    }
                });
                
                // Ограничиваем количество результатов
                if (result.size() > limit) {
                    result = result.subList(0, limit);
                }
            }
        }
        
        return result;
    }
    
    // Парсит JSON ответ и создает список PopularUser
    private List<PopularUser> parsePopularUsersFromResponse(String responseBody) {
        List<PopularUser> users = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(responseBody);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                PopularUser user = new PopularUser(
                    json.getString("user_id"),
                    json.getString("user_name"),
                    json.optString("user_avatar_url", ""),
                    json.getInt("likes_count"),
                    json.getInt("reviews_count")
                );
                users.add(user);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing popular users", e);
        }
        return users;
    }

    public interface UserStatisticsCallback {
        void onSuccess(int likes, int reviews);
        void onError(String errorMessage);
    }

    // Получает статистику пользователя (количество лайков и рецензий)
    public void getUserStatistics(String userId, String token, UserStatisticsCallback callback) {
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }

        new Thread(() -> {
            try {
                // Получаем количество рецензий
                String reviewsCountUrl = supabaseUrl + "/rest/v1/reviews?user_id=eq." + userId + "&select=count";
                Request reviewsRequest = new Request.Builder()
                        .url(reviewsCountUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

                int reviewsCount = 0;
                try (Response response = client.newCall(reviewsRequest).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
                        if (!jsonArray.isEmpty()) {
                            JsonObject countObj = jsonArray.get(0).getAsJsonObject();
                            reviewsCount = countObj.get("count").getAsInt();
                        }
                    }
                }

                // Сначала получаем список ID рецензий этого пользователя
                String reviewsIdsUrl = supabaseUrl + "/rest/v1/reviews?user_id=eq." + userId + "&select=id";
                Request reviewsIdsRequest = new Request.Builder()
                        .url(reviewsIdsUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

                List<String> reviewIds = new ArrayList<>();
                try (Response response = client.newCall(reviewsIdsRequest).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonObject reviewObj = jsonArray.get(i).getAsJsonObject();
                            reviewIds.add(reviewObj.get("id").getAsString());
                        }
                    }
                }

                // Получаем общее количество лайков на все рецензии пользователя
                int likesCount = 0;
                if (!reviewIds.isEmpty()) {
                    StringBuilder inQuery = new StringBuilder();
                    for (int i = 0; i < reviewIds.size(); i++) {
                        if (i > 0) inQuery.append(",");
                        inQuery.append(reviewIds.get(i));
                    }
                    
                    String likesCountUrl = supabaseUrl + "/rest/v1/review_likes?review_id=in.(" + inQuery.toString() + ")&select=count";
                    Request likesRequest = new Request.Builder()
                            .url(likesCountUrl)
                            .addHeader("apikey", supabaseKey)
                            .addHeader("Authorization", "Bearer " + token)
                            .get()
                            .build();

                    try (Response response = client.newCall(likesRequest).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseBody = response.body().string();
                            JsonArray jsonArray = JsonParser.parseString(responseBody).getAsJsonArray();
                            if (!jsonArray.isEmpty()) {
                                JsonObject countObj = jsonArray.get(0).getAsJsonObject();
                                likesCount = countObj.get("count").getAsInt();
                            }
                        }
                    }
                }

                // Возвращаем результат
                final int finalLikesCount = likesCount;
                final int finalReviewsCount = reviewsCount;
                callback.onSuccess(finalLikesCount, finalReviewsCount);
            } catch (Exception e) {
                Log.e(TAG, "Error getting user statistics", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    // Получает рецензии, написанные пользователем
    public void getUserReviews(String userId, String token, ReviewsCallback callback) {
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }

        new Thread(() -> {
            try {
                // Получаем список рецензий пользователя
                String userReviewsUrl = supabaseUrl + "/rest/v1/reviews?user_id=eq." + userId + "&order=created_at.desc";

                Request request = new Request.Builder()
                        .url(userReviewsUrl)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

                List<Review> reviews = new ArrayList<>();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseBody);
                        Log.d(TAG, "Got " + jsonArray.length() + " reviews from response");
                        
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject json = jsonArray.getJSONObject(i);
                            
                            // Логируем все поля объекта
                            Log.d(TAG, "Review " + i + " full JSON: " + json.toString());
                            
                            String createdAt = null;
                            if (json.has("created_at") && !json.isNull("created_at")) {
                                createdAt = json.getString("created_at");
                            }
                            
                            String updatedAt = null;
                            if (json.has("updated_at") && !json.isNull("updated_at")) {
                                updatedAt = json.getString("updated_at");
                            }
                            
                            String releaseId = json.getString("release_id");
                            
                            // Теперь по каждому release_id получаем информацию о релизе
                            String releaseUrl = supabaseUrl + "/rest/v1/releases?id=eq." + releaseId;
                            Request releaseRequest = new Request.Builder()
                                    .url(releaseUrl)
                                    .addHeader("apikey", supabaseKey)
                                    .addHeader("Authorization", "Bearer " + token)
                                    .get()
                                    .build();
                            
                            String releaseName = null;
                            
                            try (Response releaseResponse = client.newCall(releaseRequest).execute()) {
                                if (releaseResponse.isSuccessful() && releaseResponse.body() != null) {
                                    String releaseResponseBody = releaseResponse.body().string();
                                    JSONArray releaseArray = new JSONArray(releaseResponseBody);
                                    
                                    if (releaseArray.length() > 0) {
                                        JSONObject releaseJson = releaseArray.getJSONObject(0);
                                        String title = releaseJson.getString("title");
                                        String artist = releaseJson.getString("artist");
                                        releaseName = artist + " - " + title;
                                    }
                                }
                            }
                            
                            Review review = new Review(
                                json.getLong("id"),
                                json.getString("user_id"),
                                json.optString("user_name", "Пользователь"),
                                json.optString("user_avatar_url", null),
                                json.getString("release_id"),
                                releaseName,
                                (float) json.getDouble("rating"),
                                json.getString("text"),
                                createdAt,
                                updatedAt
                            );
                            
                            reviews.add(review);
                        }
                        
                        callback.onSuccess(reviews);
                    } else {
                        String error = "Failed to get user reviews: " + 
                                (response.body() != null ? response.body().string() : "Unknown error");
                        Log.e(TAG, error);
                        callback.onError(error);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting user reviews", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }

    // Получает средний рейтинг и количество рецензий для конкретного релиза
    public void getReleaseAverageRating(String releaseId, String token, AverageRatingCallback callback) {
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }

        new Thread(() -> {
            try {
                String url = supabaseUrl + "/rest/v1/rpc/get_release_average_rating";
                
                JsonObject requestJson = new JsonObject();
                requestJson.addProperty("release_id_param", releaseId);
                
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(requestJson.toString(), JSON))
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                        
                        float avgRating = 0f;
                        int count = 0;
                        
                        if (jsonResponse.has("average_rating") && !jsonResponse.get("average_rating").isJsonNull()) {
                            avgRating = jsonResponse.get("average_rating").getAsFloat();
                        }
                        
                        if (jsonResponse.has("reviews_count") && !jsonResponse.get("reviews_count").isJsonNull()) {
                            count = jsonResponse.get("reviews_count").getAsInt();
                        }
                        
                        callback.onSuccess(avgRating, count);
                    } else {
                        // Если функции нет, используем обычный метод calculateAverageRating
                        calculateAverageRating(releaseId, token, callback);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calculating release average rating", e);
                // В случае ошибки пробуем использовать обычный метод
                calculateAverageRating(releaseId, token, callback);
            }
        }).start();
    }

    // Парсит JSON-ответ с рецензиями пользователя
    private List<Review> parseReviewsFromResponse(String responseBody) {
        List<Review> reviews = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(responseBody);
            Log.d(TAG, "Got " + jsonArray.length() + " reviews from response");
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                
                // Логируем все поля объекта
                Log.d(TAG, "Review " + i + " full JSON: " + json.toString());
                
                String createdAt = null;
                if (json.has("created_at") && !json.isNull("created_at")) {
                    createdAt = json.getString("created_at");
                }
                
                String updatedAt = null;
                if (json.has("updated_at") && !json.isNull("updated_at")) {
                    updatedAt = json.getString("updated_at");
                }
                
                // Получаем информацию о релизе из вложенного объекта
                String releaseName = null;
                String coverUrl = null;
                
                if (json.has("releases") && !json.isNull("releases")) {
                    JSONObject releaseJson = json.getJSONObject("releases");
                    if (releaseJson.has("title") && releaseJson.has("artist")) {
                        String title = releaseJson.getString("title");
                        String artist = releaseJson.getString("artist");
                        releaseName = artist + " - " + title;
                    }
                    
                    if (releaseJson.has("cover_url") && !releaseJson.isNull("cover_url")) {
                        coverUrl = releaseJson.getString("cover_url");
                    }
                }
                
                Review review = new Review(
                    json.getLong("id"),
                    json.getString("user_id"),
                    json.optString("user_name", "Пользователь"),
                    json.optString("user_avatar_url", null),
                    json.getString("release_id"),
                    releaseName,
                    (float) json.getDouble("rating"),
                    json.getString("text"),
                    createdAt,
                    updatedAt
                );
                
                reviews.add(review);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing reviews from response", e);
        }
        return reviews;
    }

    // Проверяет, поставил ли пользователь лайк рецензии
    public void getLikeStatus(String reviewId, String userId, String token, LikeStatusCallback callback) {
        if (!isConfigValid) {
            callback.onError("Configuration error: Supabase URL or Key is missing");
            return;
        }

        new Thread(() -> {
            try {
                String url = supabaseUrl + "/rest/v1/review_likes?review_id=eq." + reviewId + "&user_id=eq." + userId;
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONArray jsonArray = new JSONArray(responseBody);
                        boolean hasLiked = jsonArray.length() > 0;
                        callback.onSuccess(hasLiked);
                    } else {
                        String error = "Failed to check like status: " + 
                                (response.body() != null ? response.body().string() : "Unknown error");
                        Log.e(TAG, error);
                        callback.onError(error);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking like status", e);
                callback.onError("Error: " + e.getMessage());
            }
        }).start();
    }
}
