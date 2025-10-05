package com.photospot.fotospotapp;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class MyLocationMarker implements com.google.maps.android.clustering.ClusterItem {

    private final LatLng position;
    private final String title;
    private final String locationId;

    public MyLocationMarker(double lat, double lng, String title, String locationId) {
        this.position = new LatLng(lat, lng);
        this.title = title;
        this.locationId = locationId;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getSnippet() {
        return null;
    }

    public String getLocationId() {
        return locationId;
    }
}
