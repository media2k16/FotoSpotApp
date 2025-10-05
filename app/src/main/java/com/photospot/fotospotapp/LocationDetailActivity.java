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
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.List;

public class LocationDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView streetNameText;
    private TextView locationInfo;
    private TextView locationType;
    private ViewPager2 imageSlider;
    private Button openMapButton;
    private String fullStreetName = null;

    private boolean alreadyFavorited = false;
    private boolean alreadyLiked = false;

    private String likeDocId;
    private String locationDocId;
    private int currentLikes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_detail);

        MobileAds.initialize(this, initializationStatus -> {});
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
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser().getUid();

        locationDocId = getIntent().getStringExtra("locationId");

        if (locationDocId == null || locationDocId.isEmpty()) {
            Toast.makeText(this, "Keine Location-ID übergeben", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("locations").document(locationDocId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        likeDocId = locationDocId + "_" + userId;
                        currentLikes = doc.getLong("likes") != null ? doc.getLong("likes").intValue() : 0;

                        likeCount.setText(String.valueOf(currentLikes));

                        String street = doc.getString("streetName");
                        String info = doc.getString("info");
                        String type = doc.getString("type");
                        String city = doc.getString("city");

                        fullStreetName = street + ", " + city;

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

                        db.collection("likes").document(likeDocId).get()
                                .addOnSuccessListener(likeDoc -> {
                                    alreadyLiked = likeDoc.exists();
                                    updateLikeIcon(likeIcon, alreadyLiked);
                                });

                        db.collection("favorites")
                                .document(userId)
                                .collection("locations")
                                .document(locationDocId)
                                .get()
                                .addOnSuccessListener(snapshot1 -> {
                                    alreadyFavorited = snapshot1.exists();
                                    updateFavoriteIcon(favoriteIcon, alreadyFavorited);
                                });

                        likeIcon.setOnClickListener(v -> handleLikeToggle(likeIcon, likeCount, userId));
                        favoriteIcon.setOnClickListener(v -> handleFavoriteToggle(favoriteIcon, userId));

                    } else {
                        Toast.makeText(this, "Location nicht gefunden", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Fehler beim Laden", Toast.LENGTH_SHORT).show());

        openMapButton.setOnClickListener(v -> {
            if (fullStreetName != null) {
                String uri = "https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(fullStreetName);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                startActivity(intent);
            }
        });
    }

    private void handleLikeToggle(ImageView likeIcon, TextView likeCount, String userId) {
        if (alreadyLiked) {
            int newLikes = Math.max(currentLikes - 1, 0);
            db.collection("locations").document(locationDocId)
                    .update("likes", newLikes)
                    .addOnSuccessListener(unused -> {
                        db.collection("likes").document(likeDocId).delete();
                        alreadyLiked = false;
                        currentLikes = newLikes;
                        likeCount.setText(String.valueOf(currentLikes));
                        updateLikeIcon(likeIcon, false);
                    });
        } else {
            int newLikes = currentLikes + 1;
            db.collection("locations").document(locationDocId)
                    .update("likes", newLikes)
                    .addOnSuccessListener(unused -> {
                        Like like = new Like(userId, locationDocId);
                        db.collection("likes").document(likeDocId).set(like);
                        alreadyLiked = true;
                        currentLikes = newLikes;
                        likeCount.setText(String.valueOf(currentLikes));
                        updateLikeIcon(likeIcon, true);
                    });
        }
    }

    private void handleFavoriteToggle(ImageView favoriteIcon, String userId) {
        if (alreadyFavorited) {
            db.collection("favorites")
                    .document(userId)
                    .collection("locations")
                    .document(locationDocId)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        alreadyFavorited = false;
                        updateFavoriteIcon(favoriteIcon, false);
                        Toast.makeText(this, "Aus Favoriten entfernt", Toast.LENGTH_SHORT).show();
                    });
        } else {
            String street = streetNameText.getText().toString();
            String info = locationInfo.getText().toString();
            String type = locationType.getText().toString().replace("Geeignet für: ", "");
            String city = fullStreetName != null && fullStreetName.contains(",") ? fullStreetName.split(",")[1].trim() : "";

            LocationModel locationModel = new LocationModel(
                    locationDocId,
                    null,
                    info,
                    street,
                    city,
                    type
            );

            db.collection("favorites")
                    .document(userId)
                    .collection("locations")
                    .document(locationDocId)
                    .set(locationModel)
                    .addOnSuccessListener(unused -> {
                        alreadyFavorited = true;
                        updateFavoriteIcon(favoriteIcon, true);
                        Toast.makeText(this, "Zu Favoriten hinzugefügt", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateLikeIcon(ImageView icon, boolean liked) {
        icon.setImageResource(liked ? R.drawable.ic_like_filled : R.drawable.ic_like_border);
    }

    private void updateFavoriteIcon(ImageView icon, boolean favorited) {
        icon.setImageResource(favorited ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
    }
}
