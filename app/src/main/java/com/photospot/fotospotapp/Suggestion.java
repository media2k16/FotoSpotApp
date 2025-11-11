package com.photospot.fotospotapp;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class Suggestion {
    public String id;
    public String city;
    public String street;
    public String note;
    public double latitude;
    public double longitude;
    public String imageUrl;
    public String submittedByEmail;
    public Timestamp createdAt;

    public String type;

    public static Suggestion from(DocumentSnapshot d) {
        Suggestion s = new Suggestion();
        s.city = d.getString("city");
        // Schreibweise absichern: streetName bevorzugt, sonst street
        String streetName = d.getString("streetName");
        s.street = streetName != null ? streetName : d.getString("street");
        s.note = d.getString("info");
        s.imageUrl = d.getString("imageUrl");
        s.submittedByEmail = d.getString("submittedByEmail");
        s.createdAt = d.getTimestamp("createdAt");
        s.type = d.getString("type");

        // --- robustes Parsen für latitude/longitude ---
        s.latitude  = readDoubleFlexible(d.get("latitude"), d.get("lat"));
        s.longitude = readDoubleFlexible(d.get("longitude"), d.get("lng"));

        return s;
    }

    private static double readDoubleFlexible(Object primary, Object fallback) {
        Double v = coerceToDouble(primary);
        if (v != null) return v;
        v = coerceToDouble(fallback);
        if (v != null) return v;
        return 0d; // Notfall-Default – App crasht nicht mehr
    }

    private static Double coerceToDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Double) return (Double) o;
        if (o instanceof Float)  return ((Float) o).doubleValue();
        if (o instanceof Long)   return ((Long) o).doubleValue();
        if (o instanceof Integer)return ((Integer) o).doubleValue();
        if (o instanceof String) {
            try { return Double.parseDouble(((String) o).replace(',', '.')); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }
}