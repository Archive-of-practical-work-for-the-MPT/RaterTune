package com.example.ratertune.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Класс для управления сессией пользователя
 * Сохраняет и восстанавливает данные авторизации
 */
public class SessionManager {
    private static final String PREF_NAME = "RaterTuneSession";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_REFRESH_TOKEN = "refreshToken";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_AVATAR_URL = "userAvatarUrl";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     * Сохраняет данные пользователя после успешной авторизации
     */
    public void createLoginSession(String userId, String email, String name, String accessToken, String refreshToken, String avatarUrl) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_USER_AVATAR_URL, avatarUrl);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.commit();
    }

    /**
     * Сохраняет данные пользователя после успешной авторизации (без аватарки)
     */
    public void createLoginSession(String userId, String email, String name, String accessToken, String refreshToken) {
        createLoginSession(userId, email, name, accessToken, refreshToken, null);
    }

    /**
     * Проверяет, авторизован ли пользователь
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Получает ID пользователя
     */
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    /**
     * Получает email пользователя
     */
    public String getUserEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Получает имя пользователя
     */
    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, null);
    }

    /**
     * Обновляет имя пользователя
     */
    public void updateUserName(String newName) {
        editor.putString(KEY_USER_NAME, newName);
        editor.commit();
    }

    /**
     * Получает токен доступа
     */
    public String getAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * Получает токен обновления
     */
    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    /**
     * Получает URL аватарки пользователя
     */
    public String getUserAvatarUrl() {
        return sharedPreferences.getString(KEY_USER_AVATAR_URL, null);
    }

    /**
     * Обновляет URL аватарки пользователя
     */
    public void updateUserAvatarUrl(String avatarUrl) {
        editor.putString(KEY_USER_AVATAR_URL, avatarUrl);
        editor.commit();
    }

    /**
     * Очищает данные сессии при выходе
     */
    public void logout() {
        editor.clear();
        editor.commit();
    }
} 