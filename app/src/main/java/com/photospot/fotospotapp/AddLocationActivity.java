package com.photospot.fotospotapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

public class AddLocationActivity extends AppCompatActivity {

    private EditText cityInput, streetInput, noteInput, latitudeInput, longitudeInput;
    private Button sendButton, getLocationButton;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_location);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        cityInput = findViewById(R.id.cityField);
        streetInput = findViewById(R.id.streetField);
        noteInput = findViewById(R.id.infoField);
        latitudeInput = findViewById(R.id.latitudeField);
        longitudeInput = findViewById(R.id.longitudeField);
        sendButton = findViewById(R.id.sendButton);
        getLocationButton = findViewById(R.id.getLocationButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Aktuelle Koordinaten holen (letzter bekannter Standort)
        getLocationButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(location -> {
                            if (location != null) {
                                latitudeInput.setText(String.valueOf(location.getLatitude()));
                                longitudeInput.setText(String.valueOf(location.getLongitude()));
                            } else {
                                Toast.makeText(this, "Standort nicht verfügbar", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE
                );
            }
        });

        // Spot senden
        sendButton.setOnClickListener(v -> {
            // Hintergründe zurücksetzen
            resetFieldBackgrounds();

            String city = cityInput.getText().toString().trim();
            String street = streetInput.getText().toString().trim();
            String note = noteInput.getText().toString().trim();
            String latitudeStr = latitudeInput.getText().toString().trim();
            String longitudeStr = longitudeInput.getText().toString().trim();

            boolean hasError = false;

            if (city.isEmpty()) { cityInput.setBackgroundResource(R.drawable.input_error_background); hasError = true; }
            if (street.isEmpty()) { streetInput.setBackgroundResource(R.drawable.input_error_background); hasError = true; }
            if (note.isEmpty()) { noteInput.setBackgroundResource(R.drawable.input_error_background); hasError = true; }
            if (latitudeStr.isEmpty()) { latitudeInput.setBackgroundResource(R.drawable.input_error_background); hasError = true; }
            if (longitudeStr.isEmpty()) { longitudeInput.setBackgroundResource(R.drawable.input_error_background); hasError = true; }

            if (hasError) {
                Toast.makeText(this, "Bitte fülle alle Felder aus", Toast.LENGTH_SHORT).show();
                return;
            }

            // Koordinaten prüfen
            double lat, lng;
            try {
                lat = Double.parseDouble(latitudeStr.replace(',', '.'));
                lng = Double.parseDouble(longitudeStr.replace(',', '.'));
            } catch (NumberFormatException ex) {
                latitudeInput.setBackgroundResource(R.drawable.input_error_background);
                longitudeInput.setBackgroundResource(R.drawable.input_error_background);
                Toast.makeText(this, "Koordinaten sind ungültig", Toast.LENGTH_SHORT).show();
                return;
            }

            // User-Daten (Null-safe)
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String userEmail = currentUser != null && currentUser.getEmail() != null
                    ? currentUser.getEmail() : "nicht übermittelt";
            String username = currentUser != null && currentUser.getDisplayName() != null
                    ? currentUser.getDisplayName() : "nicht übermittelt";

            // Payload aufbauen (Strings für Kompatibilität zur Function)
            Map<String, Object> data = new HashMap<>();
            data.put("city", city);
            data.put("street", street);
            data.put("note", note);
            data.put("latitude", String.valueOf(lat));
            data.put("longitude", String.valueOf(lng));
            data.put("email", userEmail);
            data.put("username", username);

            Log.d("DATA_TEST", "city=" + city + ", street=" + street + ", note=" + note +
                    ", email=" + userEmail + ", username=" + username +
                    ", lat=" + lat + ", lng=" + lng);

            // Button gegen Doppelklick sperren
            sendButton.setEnabled(false);

            FirebaseFunctions.getInstance("us-central1")
                    .getHttpsCallable("sendLocationEmail")
                    .call(data)
                    .addOnSuccessListener(result -> {
                        Toast.makeText(this, "Vielen Dank für deinen Spot! Wir melden uns.", Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Fehler beim Senden der E-Mail: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        sendButton.setEnabled(true);
                    });
        });
    }

    private void resetFieldBackgrounds() {
        cityInput.setBackgroundResource(R.drawable.input_background);
        streetInput.setBackgroundResource(R.drawable.input_background);
        noteInput.setBackgroundResource(R.drawable.input_background);
        latitudeInput.setBackgroundResource(R.drawable.input_background);
        longitudeInput.setBackgroundResource(R.drawable.input_background);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationButton.performClick();
            } else {
                Toast.makeText(this, "Standortberechtigung abgelehnt", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
