package com.photospot.fotospotapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView streetNameText;
    private TextView locationInfo;
    private TextView locationType;
    private ViewPager2 imageSlider;
    private Button openMapButton;

    private String fullStreetName = null;
    private String locationDocId;
    private String currentUid;

    private boolean alreadyFavorited = false;
    private boolean alreadyLiked = false;

    private ListenerRegistration likeCountRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_detail);

        // ✅ AdMob + Testgerät initialisieren
        AdmobHelper.initAdmob(this);

        // ✅ Banner laden
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        streetNameText = findViewById(R.id.streetNameText);
        locationInfo = findViewById(R.id.locationInfo);
        locationType = findViewById(R.id.locationType);
        imageSlider = findViewById(R.id.imageSlider);
        openMapButton = findViewById(R.id.openMapButton);
        TextView cityText = findViewById(R.id.city);
        ImageView likeIcon = findViewById(R.id.likeIcon);
        TextView likeCount = findViewById(R.id.likeCount);
        ImageView favoriteIcon = findViewById(R.id.favoriteIcon);
        DotsIndicator dotsIndicator = findViewById(R.id.dotsIndicator);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Bitte zuerst anmelden", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUid = auth.getCurrentUser().getUid();

        locationDocId = getIntent().getStringExtra("locationId");
        if (locationDocId == null || locationDocId.isEmpty()) {
            Toast.makeText(this, "Keine Location-ID übergeben", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // --- Location laden ---
        db.collection("locations").document(locationDocId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Location nicht gefunden", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String street = doc.getString("streetName");
                    String info = doc.getString("info");
                    String type = doc.getString("type");
                    String city = doc.getString("city");
                    fullStreetName = (street != null ? street : "") + (city != null ? (", " + city) : "");

                    streetNameText.setText(street != null ? street : "Unbekannte Straße");
                    locationInfo.setText(info != null ? info : "Keine Beschreibung verfügbar");
                    locationType.setText("Geeignet für: " + (type != null ? type : "Allgemein"));
                    cityText.setText(city != null ? city : "Unbekannte Stadt");

                    List<String> imageUrls = (List<String>) doc.get("imageList");
                    if (imageUrls != null && !imageUrls.isEmpty()) {
                        ImageSliderAdapter adapter = new ImageSliderAdapter(this, imageUrls);
                        imageSlider.setAdapter(adapter);
                        dotsIndicator.setViewPager2(imageSlider);
                    }

                    // --- Initialen Like/Fav-Status laden ---
                    loadLikeState(likeIcon);
                    loadFavoriteState(favoriteIcon);

                    // --- Live Like-Zähler beobachten ---
                    observeLikeCount(likeCount);

                    // --- Click Handler ---
                    likeIcon.setOnClickListener(v -> toggleLike(likeIcon));
                    favoriteIcon.setOnClickListener(v -> toggleFavorite(favoriteIcon));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Fehler beim Laden: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        // --- Maps öffnen ---
        openMapButton.setOnClickListener(v -> {
            if (fullStreetName != null) {
                String uri = "https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(fullStreetName);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
            }
        });
    }

    /* ==================== FAVORITES ==================== */

    private String favDocId() {
        return locationDocId + "_" + currentUid;
    }

    private void loadFavoriteState(ImageView favoriteIcon) {
        db.collection("favorites").document(favDocId()).get()
                .addOnSuccessListener(snap -> {
                    alreadyFavorited = snap.exists();
                    updateFavoriteIcon(favoriteIcon, alreadyFavorited);
                })
                .addOnFailureListener(e -> {/* optional anzeigen */});
    }

    private void toggleFavorite(ImageView favoriteIcon) {
        DocumentReference favRef = db.collection("favorites").document(favDocId());

        favRef.get().addOnSuccessListener(snap -> {
            if (snap.exists()) {
                // Entfernen
                favRef.delete()
                        .addOnSuccessListener(unused -> {
                            alreadyFavorited = false;
                            updateFavoriteIcon(favoriteIcon, false);
                            Toast.makeText(this, "Aus Favoriten entfernt", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Fav delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                // Hinzufügen
                Map<String, Object> data = new HashMap<>();
                data.put("locationId", locationDocId);
                data.put("streetName", streetNameText.getText().toString());
                data.put("userId", currentUid);

                favRef.set(data)
                        .addOnSuccessListener(unused -> {
                            alreadyFavorited = true;
                            updateFavoriteIcon(favoriteIcon, true);
                            Toast.makeText(this, "Zu Favoriten hinzugefügt", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Fav set: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Fav get: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /* ==================== LIKES ==================== */

    private String likeDocId() {
        return locationDocId + "_" + currentUid;
    }

    private void loadLikeState(ImageView likeIcon) {
        db.collection("likes").document(likeDocId()).get()
                .addOnSuccessListener(snap -> {
                    alreadyLiked = snap.exists();
                    updateLikeIcon(likeIcon, alreadyLiked);
                })
                .addOnFailureListener(e -> {/* optional anzeigen */});
    }

    private void toggleLike(ImageView likeIcon) {
        DocumentReference likeRef = db.collection("likes").document(likeDocId());

        likeRef.get().addOnSuccessListener(snap -> {
            if (snap.exists()) {
                likeRef.delete()
                        .addOnSuccessListener(unused -> {
                            alreadyLiked = false;
                            updateLikeIcon(likeIcon, false);
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Like delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("locationId", locationDocId);
                data.put("userId", currentUid);

                likeRef.set(data)
                        .addOnSuccessListener(unused -> {
                            alreadyLiked = true;
                            updateLikeIcon(likeIcon, true);
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Like set: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Like get: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** Zählt Likes live, ohne lokale Hoch-/Runterzählung */
    private void observeLikeCount(TextView likeCount) {
        // Falls bereits aktiv, erst entfernen
        if (likeCountRegistration != null) likeCountRegistration.remove();

        likeCountRegistration = db.collection("likes")
                .whereEqualTo("locationId", locationDocId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) return;
                    likeCount.setText(String.valueOf(snap.size()));
                });
    }

    /* ==================== UI Helpers ==================== */

    private void updateLikeIcon(ImageView icon, boolean liked) {
        icon.setImageResource(liked ? R.drawable.ic_like_filled : R.drawable.ic_like_border);
    }

    private void updateFavoriteIcon(ImageView icon, boolean favorited) {
        icon.setImageResource(favorited ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (likeCountRegistration != null) {
            likeCountRegistration.remove();
            likeCountRegistration = null;
        }
    }
}