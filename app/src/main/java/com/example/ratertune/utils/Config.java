package com.example.ratertune.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

// Класс для загрузки конфигурации из файла .env.properties
public class Config {
    private static final String TAG = "Config";
    private static final String ENV_FILE = ".env.properties";
    private static final Map<String, String> config = new HashMap<>();
    private static boolean isInitialized = false;

    /**
     * Инициализирует конфигурацию из файла .env.properties
     * @param context Контекст приложения
     */
    public static void init(Context context) {
        if (isInitialized) return;

        try {
            AssetManager assetManager = context.getAssets();
            // Проверяем список файлов в директории assets
            String[] files = assetManager.list("");
            boolean fileFound = false;

            assert files != null;
            for (String file : files) {
                Log.d(TAG, "Found asset file: " + file);
                if (ENV_FILE.equals(file)) {
                    fileFound = true;
                    break;
                }
            }
            
            if (!fileFound) {
                Log.e(TAG, "File " + ENV_FILE + " not found in assets directory");
                // Если точный файл не найден, ищем любой файл с похожим именем
                for (String file : files) {
                    if (file.contains("env") || file.contains("properties")) {
                        Log.d(TAG, "Found similar file: " + file + ", trying to use it instead");
                        InputStream inputStream = assetManager.open(file);
                        loadConfigFromStream(inputStream);
                        return;
                    }
                }
                return;
            }
            
            InputStream inputStream = assetManager.open(ENV_FILE);
            loadConfigFromStream(inputStream);
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading configuration: " + e.getMessage(), e);
        }
    }
    
    // Загружает конфигурацию из входного потока
    private static void loadConfigFromStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        int lineCount = 0;

        while ((line = reader.readLine()) != null) {
            lineCount++;
            // Пропускаем комментарии и пустые строки
            if (line.startsWith("#") || line.trim().isEmpty()) continue;

            // Разбираем строку на ключ и значение
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                config.put(key, value);
                Log.d(TAG, "Loaded config: " + key + "=*******");
            } else {
                Log.w(TAG, "Invalid line format at line " + lineCount + ": " + line);
            }
        }

        reader.close();
        isInitialized = true;
        Log.d(TAG, "Configuration loaded successfully, found " + config.size() + " properties");
    }

    /**
     * Получает значение по ключу
     * @param key Ключ
     * @return Значение или null, если ключ не найден
     */
    public static String get(String key) {
        String value = config.get(key);
        if (value == null) {
            Log.w(TAG, "Configuration key not found: " + key);
        }
        return value;
    }

    /**
     * Получает значение по ключу с значением по умолчанию
     * @param key Ключ
     * @param defaultValue Значение по умолчанию
     * @return Значение или defaultValue, если ключ не найден
     */
    public static String get(String key, String defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }
} 