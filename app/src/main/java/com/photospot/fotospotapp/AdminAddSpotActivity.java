package com.photospot.fotospotapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AdminAddSpotActivity extends AppCompatActivity {

    private EditText cityField, streetField, infoField, latitudeField, longitudeField;
    private Spinner typeSpinner;
    private Button saveButton, getLocationButton;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_spot);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        // Views
        cityField = findViewById(R.id.cityField);
        streetField = findViewById(R.id.streetField);
        infoField = findViewById(R.id.infoField);
        latitudeField = findViewById(R.id.latitudeField);
        longitudeField = findViewById(R.id.longitudeField);
        typeSpinner = findViewById(R.id.typeSpinner);   // <-- Feld, nicht lokale Variable!
        saveButton = findViewById(R.id.sendButton);
        getLocationButton = findViewById(R.id.getLocationButton);

        // Firebase
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Spinner befüllen
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.location_types,                   // z.B. ["Automotive","Portrait","Allgemein"]
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        int def = adapter.getPosition("Automotive");
        if (def >= 0) typeSpinner.setSelection(def);

        // Standort holen
        getLocationButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE
                );
            } else {
                getCurrentLocation();
            }
        });

        // Speichern
        saveButton.setOnClickListener(v -> {
            String city = cityField.getText().toString().trim();
            String street = streetField.getText().toString().trim();
            String info = infoField.getText().toString().trim();
            String type = String.valueOf(typeSpinner.getSelectedItem());
            String latitudeStr = latitudeField.getText().toString().trim();
            String longitudeStr = longitudeField.getText().toString().trim();

            if (city.isEmpty() || street.isEmpty() || info.isEmpty() ||
                    latitudeStr.isEmpty() || longitudeStr.isEmpty()) {
                Toast.makeText(this, "Bitte alle Felder ausfüllen", Toast.LENGTH_SHORT).show();
                return;
            }

            double latitude, longitude;
            try {
                latitude = Double.parseDouble(latitudeStr.replace(',', '.'));
                longitude = Double.parseDouble(longitudeStr.replace(',', '.'));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Ungültige Koordinaten", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> location = new HashMap<>();
            location.put("city", city);
            location.put("streetName", street);
            location.put("info", info);
            location.put("type", type);
            location.put("latitude", latitude);
            location.put("longitude", longitude);
            location.put("likes", 0);
            location.put("createdAt", FieldValue.serverTimestamp());

            db.collection("locations").add(location)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Spot erfolgreich hinzugefügt", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Fehler beim Speichern: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void getCurrentLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            latitudeField.setText(String.valueOf(location.getLatitude()));
                            longitudeField.setText(String.valueOf(location.getLongitude()));
                        } else {
                            Toast.makeText(this, "Standort konnte nicht abgerufen werden", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (SecurityException ignored) {
            // Falls Berechtigung doch fehlt
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Standortberechtigung abgelehnt", Toast.LENGTH_SHORT).show();
            }
        }
    }
}