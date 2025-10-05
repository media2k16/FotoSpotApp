package com.photospot.fotospotapp;

import com.google.firebase.firestore.DocumentSnapshot;

public class Suggestion {
    public String id;
    public String email;
    public String username;
    public String city;
    public String street;
    public String note;
    public String latitude;
    public String longitude;
    public String status;

    public static Suggestion from(DocumentSnapshot d) {
        Suggestion s = new Suggestion();
        s.email = d.getString("email");
        s.username = d.getString("username");
        s.city = d.getString("city");
        s.street = d.getString("street");
        s.note = d.getString("note");
        s.latitude = str(d.get("latitude"));
        s.longitude = str(d.get("longitude"));
        s.status = d.getString("status");
        return s;
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
}