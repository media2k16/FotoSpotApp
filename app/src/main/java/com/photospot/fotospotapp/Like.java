package com.photospot.fotospotapp;

public class Like {
    public String userId;
    public String locationId;

    public Like() {
        // Default-Konstruktor benötigt von Firestore
    }

    public Like(String userId, String locationId) {
        this.userId = userId;
        this.locationId = locationId;
    }
}
