package com.example.ratertune.models;

import android.util.Log;
import com.google.gson.annotations.SerializedName;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Review {
    private static final String TAG = "Review";
    
    @SerializedName("id")
    private String id;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("user_name")
    private String userName;
    
    @SerializedName("user_avatar_url")
    private String userAvatarUrl;
    
    @SerializedName("release_id")
    private String releaseId;
    
    @SerializedName("rating")
    private float rating;
    
    @SerializedName("text")
    private String text;
    
    @SerializedName("created_at")
    private String createdAt;
    
    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("release_name")
    private String releaseName;

    private static final SimpleDateFormat[] inputFormats = new SimpleDateFormat[] {
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'", Locale.getDefault()),
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
    };
    
    private static final SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    public Review() {
        // Default constructor required for Gson
    }

    public Review(String id, String userId, String userName, String userAvatarUrl, String releaseId, float rating, String text, String createdAt, String updatedAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.releaseId = releaseId;
        this.rating = rating;
        this.text = text;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        
        Log.d(TAG, "Created review with date: " + createdAt);
    }

    public Review(String id, String userId, String userName, String userAvatarUrl, String releaseId, String releaseName, float rating, String text, String createdAt, String updatedAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.releaseId = releaseId;
        this.releaseName = releaseName;
        this.rating = rating;
        this.text = text;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        
        Log.d(TAG, "Created review with date: " + createdAt);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(String releaseId) {
        this.releaseId = releaseId;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    public float getRating() { return rating; }

    public void setRating(float rating) {
        this.rating = rating;
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

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFormattedDate() {
        if (createdAt == null || createdAt.isEmpty()) {
            Log.e(TAG, "createdAt is null or empty");
            return "Дата неизвестна";
        }
        
        try {
            Log.d(TAG, "Trying to format date: " + createdAt);
            Date date = null;
            
            for (SimpleDateFormat format : inputFormats) {
                try {
                    date = format.parse(createdAt);
                    if (date != null) {
                        Log.d(TAG, "Successfully parsed with format: " + format.toPattern());
                        break;
                    }
                } catch (ParseException e) {
                    // Continue to the next format
                }
            }
            
            if (date != null) {
                String formattedDate = outputFormat.format(date);
                Log.d(TAG, "Formatted date: " + formattedDate);
                return formattedDate;
            } else {
                Log.e(TAG, "Could not parse date: " + createdAt);
                return createdAt; // Return raw date if parsing fails
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + e.getMessage(), e);
            return createdAt; // Return raw date on error
        }
    }
} 