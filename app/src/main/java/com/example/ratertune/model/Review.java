package com.example.ratertune.model;

public class Review {
    private String id;
    private String userId;
    private String userName;
    private String userAvatarUrl;
    private String releaseId;
    private String releaseTitle;
    private String releaseArtist;
    private String title;
    private String text;
    private float rating;
    private String date;

    public Review(String id, String userId, String userName, String userAvatarUrl, 
                 String releaseId, String releaseTitle, String releaseArtist,
                 String title, String text, float rating, String date) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.releaseId = releaseId;
        this.releaseTitle = releaseTitle;
        this.releaseArtist = releaseArtist;
        this.title = title;
        this.text = text;
        this.rating = rating;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public String getReleaseTitle() {
        return releaseTitle;
    }

    public String getReleaseArtist() {
        return releaseArtist;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public float getRating() {
        return rating;
    }

    public String getDate() {
        return date;
    }
} 