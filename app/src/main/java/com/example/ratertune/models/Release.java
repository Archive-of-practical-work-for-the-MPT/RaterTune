package com.example.ratertune.models;

public class Release {
    private final String id;
    private final String title;
    private final String artist;
    private final String imageUrl;
    private final float rating;
    private final String releaseDate;

    public Release(String id, String title, String artist, String imageUrl, float rating, String releaseDate) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.releaseDate = releaseDate;
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

    public String getReleaseDate() {
        return releaseDate;
    }
} 