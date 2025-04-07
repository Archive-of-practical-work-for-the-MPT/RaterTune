package com.example.ratertune;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class SupabaseClient {
    private static final String TAG = "SupabaseClient";
    private static SupabaseClient instance;
    
    private final String supabaseUrl;
    private final String supabaseKey;
    private final SupabaseAuthApi supabaseAuthApi;
    private String accessToken;
    
    private SupabaseClient() {
        // Получаем значения из Config
        this.supabaseUrl = Config.getSupabaseUrl();
        this.supabaseKey = Config.getSupabaseKey();
        
        Log.d(TAG, "Используем URL из env.properties: " + this.supabaseUrl);
        
        // Создаем HTTP клиент с логированием
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    // Добавляем необходимые заголовки для Supabase
                    okhttp3.Request original = chain.request();
                    okhttp3.Request request = original.newBuilder()
                            .header("apikey", supabaseKey)
                            .header("Content-Type", "application/json")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                .addInterceptor(loggingInterceptor)
                .build();
        
        // Создаем Gson
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        
        // Создаем Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(supabaseUrl + "/auth/v1/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();
        
        // Создаем API интерфейс
        supabaseAuthApi = retrofit.create(SupabaseAuthApi.class);
    }
    
    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }
    
    // Интерфейс для коллбэков
    public interface AuthCallback {
        void onSuccess();
        void onError(String errorMessage);
    }
    
    // Интерфейс для Retrofit API
    private interface SupabaseAuthApi {
        @POST("signup")
        Call<AuthResponse> signUp(@Body SignUpRequest request);
        
        @POST("token?grant_type=password")
        Call<AuthResponse> signIn(@Body SignInRequest request);
    }
    
    // Модель для запроса регистрации
    private static class SignUpRequest {
        private final String email;
        private final String password;
        
        public SignUpRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
    
    // Модель для запроса входа
    private static class SignInRequest {
        private final String email;
        private final String password;
        
        public SignInRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
    
    // Модель для ответа сервера
    private static class AuthResponse {
        @SerializedName("access_token")
        private String accessToken;
        
        @SerializedName("refresh_token")
        private String refreshToken;
        
        @SerializedName("error")
        private String error;
        
        @SerializedName("error_description")
        private String errorDescription;
    }
    
    // Метод для регистрации
    public void signUp(String email, String password, AuthCallback callback) {
        SignUpRequest request = new SignUpRequest(email, password);
        supabaseAuthApi.signUp(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    if (authResponse.error != null) {
                        callback.onError(authResponse.errorDescription != null ? 
                                authResponse.errorDescription : authResponse.error);
                        return;
                    }
                    accessToken = authResponse.accessToken;
                    callback.onSuccess();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                        callback.onError("Ошибка: " + response.code() + " " + errorBody);
                    } catch (IOException e) {
                        callback.onError("Ошибка: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "Sign up failed", t);
                callback.onError("Ошибка соединения: " + t.getMessage());
            }
        });
    }
    
    // Метод для входа
    public void signIn(String email, String password, AuthCallback callback) {
        SignInRequest request = new SignInRequest(email, password);
        supabaseAuthApi.signIn(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse authResponse = response.body();
                    if (authResponse.error != null) {
                        callback.onError(authResponse.errorDescription != null ? 
                                authResponse.errorDescription : authResponse.error);
                        return;
                    }
                    accessToken = authResponse.accessToken;
                    callback.onSuccess();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? 
                                response.errorBody().string() : "Unknown error";
                        callback.onError("Ошибка: " + response.code() + " " + errorBody);
                    } catch (IOException e) {
                        callback.onError("Ошибка: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Log.e(TAG, "Sign in failed", t);
                callback.onError("Ошибка соединения: " + t.getMessage());
            }
        });
    }
    
    // Получение токена доступа
    public String getAccessToken() {
        return accessToken;
    }
    
    // Метод для выхода
    public void signOut(AuthCallback callback) {
        // Просто очищаем токен на клиенте
        accessToken = null;
        callback.onSuccess();
    }
} 