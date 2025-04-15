package com.example.ratertune.models;

import com.google.gson.annotations.SerializedName;

public class Story {
    @SerializedName("id")
    private String id;
    
    @SerializedName("text")
    private String text;
    
    @SerializedName("image_url")
    private String imageUrl;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("expires_at")
    private String expiresAt;
    
    // Локальное поле для отслеживания просмотра, не сериализуется
    private boolean viewed = false;
    
    public Story() {
        // Пустой конструктор для Gson
    }
    
    public Story(String id, String text, String imageUrl, String userId, String createdAt, String expiresAt) {
        this.id = id;
        this.text = text;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public boolean isViewed() {
        return viewed;
    }
    
    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }
} 