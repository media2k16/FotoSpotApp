package com.photospot.fotospotapp;

public class FavoriteLocation {
    public String streetName;
    public String fullAddress;

    public FavoriteLocation() {} // leere Konstruktor für Firestore

    public FavoriteLocation(String streetName, String fullAddress) {
        this.streetName = streetName;
        this.fullAddress = fullAddress;
    }
}
