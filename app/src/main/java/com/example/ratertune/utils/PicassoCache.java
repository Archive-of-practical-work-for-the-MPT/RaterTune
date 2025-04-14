package com.example.ratertune.utils;

import android.content.Context;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.OkHttp3Downloader;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class PicassoCache {
    private static Picasso instance = null;

    public static Picasso getInstance(Context context) {
        if (instance == null) {
            // Создаем кэш для изображений (50MB)
            File cacheFile = new File(context.getCacheDir(), "picasso-cache");
            Cache cache = new Cache(cacheFile, 50 * 1024 * 1024);

            // Настраиваем OkHttpClient с кэшем
            OkHttpClient client = new OkHttpClient.Builder()
                    .cache(cache)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Создаем Picasso с настроенным OkHttpClient
            instance = new Picasso.Builder(context)
                    .downloader(new OkHttp3Downloader(client))
                    .indicatorsEnabled(false) // Отключаем индикаторы загрузки
                    .loggingEnabled(false) // Отключаем логирование
                    .build();

            // Устанавливаем глобальный инстанс
            Picasso.setSingletonInstance(instance);
        }
        return instance;
    }

    public static void preloadImage(Context context, String imageUrl) {
        Picasso picasso = getInstance(context);
        picasso.load(imageUrl)
                .fetch(new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        // Изображение успешно загружено в кэш
                    }

                    @Override
                    public void onError(Exception e) {
                        // Ошибка загрузки изображения
                    }
                });
    }
} 