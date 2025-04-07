package com.example.ratertune;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Config {
    private static final String TAG = "Config";
    private static Map<String, String> envVariables = new HashMap<>();
    private static boolean isInitialized = false;

    public static void init(Context context) {
        if (isInitialized) {
            return;
        }

        try {
            // Открываем env.properties файл из assets вместо .env
            Log.d(TAG, "Пытаемся открыть env.properties файл из assets");
            InputStream inputStream = context.getAssets().open("env.properties");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            
            Log.d(TAG, "Файл env.properties найден, начинаем чтение");
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                Log.d(TAG, "Прочитана строка: " + line);
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        // Удаляем кавычки, если они есть
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        envVariables.put(key, value);
                        Log.d(TAG, "Добавлена переменная: " + key + " = " + value);
                    }
                }
            }
            
            reader.close();
            isInitialized = true;
            Log.d(TAG, "Загрузка env.properties файла завершена успешно");
        } catch (IOException e) {
            Log.e(TAG, "Ошибка загрузки env.properties файла: " + e.getMessage(), e);
            // Устанавливаем дефолтные значения, если не удалось загрузить
            envVariables.put("SUPABASE_URL", "https://your-project-id.supabase.co");
            envVariables.put("SUPABASE_KEY", "your-supabase-anon-key");
        }
        
        // Выводим загруженные значения для проверки
        Log.i(TAG, "SUPABASE_URL = " + getSupabaseUrl());
        Log.i(TAG, "SUPABASE_KEY = " + getSupabaseKey());
    }

    public static String getSupabaseUrl() {
        return envVariables.getOrDefault("SUPABASE_URL", "https://your-project-id.supabase.co");
    }

    public static String getSupabaseKey() {
        return envVariables.getOrDefault("SUPABASE_KEY", "your-supabase-anon-key");
    }
} 