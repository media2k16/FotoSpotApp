package com.photospot.fotospotapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class StreetListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ArrayAdapter<String> adapter;
    private List<String> streetList;
    private String cityName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street_list);
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


        cityName = getIntent().getStringExtra("cityName");

        if (cityName == null) {
            finish();
            return;
        }

        // Titel setzen
        TextView title = findViewById(R.id.streetTitle);
        title.setText("Straßen in " + cityName);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        ListView listView = findViewById(R.id.streetListView);
        streetList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.list_item_street, R.id.streetItemText, streetList);
        listView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // 📥 Straßen aus Firestore laden
        db.collection("locations")
                .whereEqualTo("city", cityName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    streetList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String street = doc.getString("streetName");
                        if (street != null && !streetList.contains(street)) {
                            streetList.add(street);
                        }
                    }
                    streetList.sort(String::compareToIgnoreCase);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler beim Laden der Daten", Toast.LENGTH_SHORT).show());

        // 👉 Straße auswählen → passende Location-ID laden
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedStreet = streetList.get(position);

            db.collection("locations")
                    .whereEqualTo("city", cityName)
                    .whereEqualTo("streetName", selectedStreet)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            String locationId = snapshot.getDocuments().get(0).getId();

                            Intent intent = new Intent(StreetListActivity.this, LocationDetailActivity.class);
                            intent.putExtra("locationId", locationId);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Keine passende Location gefunden", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Fehler beim Laden der Location-ID", Toast.LENGTH_SHORT).show());
        });
    }
}