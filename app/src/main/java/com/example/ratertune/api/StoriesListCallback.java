package com.example.ratertune.api;

import com.example.ratertune.models.Story;
import java.util.List;

/**
 * Интерфейс для обратного вызова при загрузке списка сторизов
 */
public interface StoriesListCallback {
    void onSuccess(List<Story> stories);
    void onError(String errorMessage);
} 