package com.example.ratertune.api;

import com.example.ratertune.models.Story;

public interface StoryCallback {
    void onSuccess(Story story);
    void onError(String errorMessage);
} 