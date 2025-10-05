package com.photospot.fotospotapp;

public class Favorite {
    private String userId;
    private String locationId;

    public Favorite() {
        // Leerer Konstruktor für Firebase
    }

    public Favorite(String userId, String locationId) {
        this.userId = userId;
        this.locationId = locationId;
    }

    public String getUserId() {
        return userId;
    }

    public String getLocationId() {
        return locationId;
    }
}
