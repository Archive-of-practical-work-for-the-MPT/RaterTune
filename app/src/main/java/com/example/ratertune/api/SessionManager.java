package com.example.ratertune.api;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "RaterTuneSession";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_ACCESS_TOKEN = "accessToken";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_AVATAR_URL = "userAvatarUrl";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveUserSession(String userId, String accessToken, String userName, String avatarUrl) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_AVATAR_URL, avatarUrl);
        editor.apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    public String getAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, null);
    }

    public String getUserAvatarUrl() {
        return sharedPreferences.getString(KEY_USER_AVATAR_URL, null);
    }

    public boolean isLoggedIn() {
        return getUserId() != null && getAccessToken() != null;
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
} 