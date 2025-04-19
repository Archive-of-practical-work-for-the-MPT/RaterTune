package com.example.ratertune.models;

public class Release {
    private final String id;
    private final String title;
    private final String artist;
    private final String imageUrl;
    private float rating;
    private final String releaseDate;
    private int reviewsCount;

    public Release(String id, String title, String artist, String imageUrl, float rating, String releaseDate) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.releaseDate = releaseDate;
        this.reviewsCount = 0;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getReleaseDate() {
        return releaseDate;
    }
    
    public int getReviewsCount() {
        return reviewsCount;
    }
    
    public void setReviewsCount(int reviewsCount) {
        this.reviewsCount = reviewsCount;
    }
} 