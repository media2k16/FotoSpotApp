package com.photospot.fotospotapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdminSuggestionsActivity extends AppCompatActivity implements SuggestionAdapter.SuggestionActionListener {

    private RecyclerView recycler;
    private ProgressBar progress;
    private FirebaseFirestore db;
    private SuggestionAdapter adapter;
    private final List<Suggestion> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_suggestions);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        recycler = findViewById(R.id.recyclerSuggestions);
        progress = findViewById(R.id.progressSuggestions);

        adapter = new SuggestionAdapter(items, this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Live-Query: offene Vorschläge
        progress.setVisibility(View.VISIBLE);
        db.collection("suggestions")
                .whereEqualTo("status", "open")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    progress.setVisibility(View.GONE);
                    if (err != null) {
                        Toast.makeText(this, "Fehler: " + err.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        String id = dc.getDocument().getId();
                        Suggestion s = Suggestion.from(dc.getDocument());
                        s.id = id;

                        switch (dc.getType()) {
                            case ADDED:
                                items.add(dc.getNewIndex(), s);
                                adapter.notifyItemInserted(dc.getNewIndex());
                                break;
                            case MODIFIED:
                                items.set(dc.getOldIndex(), s);
                                adapter.notifyItemChanged(dc.getOldIndex());
                                break;
                            case REMOVED:
                                items.remove(dc.getOldIndex());
                                adapter.notifyItemRemoved(dc.getOldIndex());
                                break;
                        }
                    }
                });
    }

    @Override
    public void onApprove(@NonNull Suggestion s, int position) {
        // Minimal: Bei “Approve” -> in "locations" übernehmen + Vorschlag auf "approved"
        DocumentReference locRef = db.collection("locations").document();
        // Passe dein Locations-Schema hier an:
        HashMap<String, Object> data = new HashMap<>();
        data.put("streetName", s.street);
        data.put("city", s.city);
        data.put("info", s.note);
        data.put("type", "Allgemein"); // ggf. aus Vorschlag ergänzen
        data.put("latitude", s.latitude);
        data.put("longitude", s.longitude);
        data.put("likes", 0);
        data.put("createdAt", Timestamp.now());
        data.put("imageList", java.util.Collections.emptyList()); // optional

        locRef.set(data).addOnSuccessListener(unused -> {
            db.collection("suggestions").document(s.id)
                    .update("status", "approved", "approvedAt", Timestamp.now(), "locationId", locRef.getId())
                    .addOnSuccessListener(v -> {
                        Toast.makeText(this, "Vorschlag bestätigt", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Update fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Location anlegen fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    @Override
    public void onReject(@NonNull Suggestion s, int position) {
        db.collection("suggestions").document(s.id)
                .update("status", "rejected", "rejectedAt", Timestamp.now())
                .addOnSuccessListener(unused -> Toast.makeText(this, "Abgelehnt", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}