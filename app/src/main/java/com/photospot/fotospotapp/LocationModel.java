package com.photospot.fotospotapp;

import java.util.List;

public class LocationModel {
    private String id;
    private String image; // wird teilweise noch gebraucht (ältere Strukturen)
    private List<String> imageList; // 🔹 neue Variante für mehrere Bilder
    private String info;
    private String streetName;
    private String city;
    private String type;

    // 🔹 Leerer Konstruktor (Pflicht für Firestore)
    public LocationModel() {}

    // 🔹 Konstruktor für einzelne Bilder (alt)
    public LocationModel(String id, String image, String info, String streetName, String city, String type) {
        this.id = id;
        this.image = image;
        this.info = info;
        this.streetName = streetName;
        this.city = city;
        this.type = type;
    }

    // 🔹 Konstruktor für mehrere Bilder (neu)
    public LocationModel(String id, List<String> imageList, String info, String streetName, String city, String type) {
        this.id = id;
        this.imageList = imageList;
        this.info = info;
        this.streetName = streetName;
        this.city = city;
        this.type = type;
    }

    // 🔹 Getter & Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getImageList() {
        return imageList;
    }

    public void setImageList(List<String> imageList) {
        this.imageList = imageList;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}