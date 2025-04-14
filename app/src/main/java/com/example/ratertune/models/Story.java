package com.example.ratertune.models;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class Story {
    @SerializedName("id")
    private int id;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("image_url")
    private String imageUrl;
    
    @SerializedName("text")
    private String text;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("expires_at")
    private String expiresAt;
    
    private String userName;
    
    private String userAvatarUrl;
    
    public int getId() {
        return id;
    }
    
    public void setId(String id) {
        try {
            this.id = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            this.id = 0;
        }
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        if (createdAt != null) {
            this.createdAt = createdAt.toString();
        }
    }
    
    public String getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }
    
    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }
}