package com.photospot.fotospotapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AddLocationActivity extends AppCompatActivity {

    private EditText cityInput, streetInput, noteInput, latitudeInput, longitudeInput;
    private Button sendButton, getLocationButton, btnPickImage, btnRemoveImage;
    private ImageView ivPreview;
    private ProgressBar progress;
    private Spinner typeSpinner;

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Uri imageUri = null;
    private ActivityResultLauncher<String> pickImageLauncher;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final String TAG = "AddLocation";

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

        // === UI Referenzen ===
        cityInput = findViewById(R.id.cityField);
        streetInput = findViewById(R.id.streetField);
        noteInput = findViewById(R.id.infoField);
        latitudeInput = findViewById(R.id.latitudeField);
        longitudeInput = findViewById(R.id.longitudeField);
        sendButton = findViewById(R.id.sendButton);
        getLocationButton = findViewById(R.id.getLocationButton);
        btnPickImage = findViewById(R.id.btnPickImage);
        btnRemoveImage = findViewById(R.id.btnRemoveImage);
        ivPreview = findViewById(R.id.ivPreview);
        progress = findViewById(R.id.progress);
        typeSpinner = findViewById(R.id.typeSpinner);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // === Spinner befüllen (mit Guard gegen fehlenden View) ===
        if (typeSpinner != null) {
            ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.location_types,                      // -> res/values/strings.xml
                    android.R.layout.simple_spinner_item
            );
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            typeSpinner.setAdapter(spinnerAdapter);
            int defaultPos = spinnerAdapter.getPosition("Automotive");
            if (defaultPos >= 0) typeSpinner.setSelection(defaultPos);
        } else {
            Log.e(TAG, "typeSpinner ist NULL – fehlt im Layout activity_add_location.xml?");
            Toast.makeText(this, "Hinweis: Typ-Dropdown nicht im Layout gefunden. Es wird 'Automotive' verwendet.", Toast.LENGTH_LONG).show();
        }

        // === Image Picker (mit sicherer Vorschau) ===
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;
                        try {
                            ivPreview.setImageBitmap(loadScaledBitmap(uri, 1024));
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Fehler beim Laden des Bilds", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        btnPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnRemoveImage.setOnClickListener(v -> {
            imageUri = null;
            ivPreview.setImageDrawable(null);
            Toast.makeText(this, "Bild entfernt", Toast.LENGTH_SHORT).show();
        });

        // === Standort holen ===
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

        // === Spot absenden ===
        sendButton.setOnClickListener(v -> {
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
            if (imageUri == null) { Toast.makeText(this, "Bitte ein Bild auswählen (Pflichtfeld).", Toast.LENGTH_SHORT).show(); hasError = true; }

            if (hasError) {
                Toast.makeText(this, "Bitte fülle alle Felder aus.", Toast.LENGTH_SHORT).show();
                return;
            }

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

            // === Check: Eingeloggt? ===
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u == null) {
                Toast.makeText(this, "Bitte zuerst einloggen – Upload erfordert Login.", Toast.LENGTH_LONG).show();
                Log.e("UPLOAD", "❌ Kein User eingeloggt!");
                return;
            } else {
                Log.d("UPLOAD", "✅ Eingeloggt als: " + u.getEmail() + " (uid=" + u.getUid() + ")");
            }

            setLoading(true);

            // === Datei-Name & Pfad ===
            String fileCity = city.replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileStreet = street.replaceAll("[^a-zA-Z0-9_-]", "_");
            String path = "suggestions/" + fileCity + "/" + fileStreet + "_" + System.currentTimeMillis() + ".jpg";

            StorageReference ref = storage.getReference().child(path);
            Log.d("UPLOAD", "📁 Pfad: " + ref.toString());

            // === Upload starten (Original-Datei) ===
            ref.putFile(imageUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return ref.getDownloadUrl();
                    })
                    .addOnSuccessListener(downloadUri -> {
                        // Typ aus Spinner holen (fallback, falls Spinner fehlt)
                        String type = "Automotive";
                        if (typeSpinner != null && typeSpinner.getSelectedItem() != null) {
                            type = String.valueOf(typeSpinner.getSelectedItem()).trim();
                            if (type.isEmpty()) type = "Automotive";
                        }

                        // === Firestore speichern ===
                        Map<String, Object> suggestion = new HashMap<>();
                        suggestion.put("city", city);
                        suggestion.put("streetName", street);
                        suggestion.put("info", note);
                        suggestion.put("latitude", lat);
                        suggestion.put("longitude", lng);
                        suggestion.put("imageUrl", downloadUri.toString());
                        suggestion.put("status", "open");
                        suggestion.put("createdAt", FieldValue.serverTimestamp());
                        suggestion.put("submittedByEmail", u.getEmail());
                        suggestion.put("submittedByName", u.getDisplayName() != null ? u.getDisplayName() : "nicht übermittelt");
                        suggestion.put("source", "app");
                        suggestion.put("type", type); // wichtig

                        db.collection("suggestions").add(suggestion)
                                .addOnSuccessListener(docRef -> {
                                    // Optional: E-Mail-Function
                                    Map<String, Object> emailData = new HashMap<>();
                                    emailData.put("city", city);
                                    emailData.put("street", street);
                                    emailData.put("note", note);
                                    emailData.put("latitude", String.valueOf(lat));
                                    emailData.put("longitude", String.valueOf(lng));
                                    emailData.put("email", u.getEmail());
                                    emailData.put("username", u.getDisplayName());
                                    emailData.put("imageUrl", downloadUri.toString());

                                    FirebaseFunctions.getInstance("us-central1")
                                            .getHttpsCallable("sendLocationEmail")
                                            .call(emailData)
                                            .addOnCompleteListener(task2 -> {
                                                setLoading(false);
                                                Toast.makeText(this, "Danke! Dein Spot ist in Prüfung 🙂", Toast.LENGTH_LONG).show();
                                                finish();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    Toast.makeText(this, "Fehler beim Speichern: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Bild-Upload fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(!loading);
        btnPickImage.setEnabled(!loading);
        btnRemoveImage.setEnabled(!loading);
        getLocationButton.setEnabled(!loading);
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

    // ====== Hilfsfunktion: großes Bild speicherschonend für die Vorschau laden ======
    private Bitmap loadScaledBitmap(Uri uri, int maxSize) throws java.io.IOException {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;

        InputStream is = getContentResolver().openInputStream(uri);
        BitmapFactory.decodeStream(is, null, opts);
        if (is != null) is.close();

        int scale = 1;
        while (opts.outWidth / scale > maxSize || opts.outHeight / scale > maxSize) {
            scale *= 2;
        }

        opts.inSampleSize = scale;
        opts.inJustDecodeBounds = false;
        is = getContentResolver().openInputStream(uri);
        Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
        if (is != null) is.close();

        return bmp;
    }
}