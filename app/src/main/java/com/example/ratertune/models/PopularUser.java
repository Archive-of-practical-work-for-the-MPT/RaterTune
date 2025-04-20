package com.example.ratertune.models;

import com.google.gson.annotations.SerializedName;

public class PopularUser {
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("user_name")
    private String userName;
    
    @SerializedName("user_avatar_url")
    private String avatarUrl;
    
    @SerializedName("likes_count")
    private int likesCount;
    
    @SerializedName("reviews_count")
    private int reviewsCount;
    
    public PopularUser(String userId, String userName, String avatarUrl, int likesCount, int reviewsCount) {
        this.userId = userId;
        this.userName = userName;
        this.avatarUrl = avatarUrl;
        this.likesCount = likesCount;
        this.reviewsCount = reviewsCount;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public int getLikesCount() {
        return likesCount;
    }
    
    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }
    
    public int getReviewsCount() {
        return reviewsCount;
    }
    
    public void setReviewsCount(int reviewsCount) {
        this.reviewsCount = reviewsCount;
    }
}
