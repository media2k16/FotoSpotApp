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
                .whereEqualTo("source", "app")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    progress.setVisibility(View.GONE);
                    if (err != null) {
                        Toast.makeText(this, "Fehler: " + err.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        try {
                            Suggestion s = Suggestion.from(dc.getDocument());
                            // Skippe alte/inkomplette Einträge ohne Bild
                            if (s.imageUrl == null || s.imageUrl.isEmpty()) {
                                continue;
                            }
                            s.id = dc.getDocument().getId();

                            switch (dc.getType()) {
                                case ADDED:
                                    items.add(dc.getNewIndex(), s);
                                    adapter.notifyItemInserted(dc.getNewIndex());
                                    break;

                                case MODIFIED:
                                    // Falls sich die Position ändert, sauber verschieben
                                    if (dc.getOldIndex() == dc.getNewIndex()) {
                                        items.set(dc.getOldIndex(), s);
                                        adapter.notifyItemChanged(dc.getOldIndex());
                                    } else {
                                        items.remove(dc.getOldIndex());
                                        items.add(dc.getNewIndex(), s);
                                        adapter.notifyItemMoved(dc.getOldIndex(), dc.getNewIndex());
                                        adapter.notifyItemChanged(dc.getNewIndex());
                                    }
                                    break;

                                case REMOVED:
                                    items.remove(dc.getOldIndex());
                                    adapter.notifyItemRemoved(dc.getOldIndex());
                                    break;
                            }
                        } catch (Exception e) {
                            android.util.Log.e("ADMIN", "Skip invalid doc: " + dc.getDocument().getId(), e);
                        }
                    }
                });
    }

    @Override
    public void onApprove(@NonNull Suggestion s, int position) {
        // 1) Saubere, stabile ID aus Stadt+Straße erzeugen
        String baseId = (s.city + "_" + s.street)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        DocumentReference locRef = db.collection("locations").document(baseId);

        HashMap<String, Object> data = new HashMap<>();
        data.put("streetName", s.street);
        data.put("city", s.city);
        data.put("info", s.note);
        data.put("latitude", s.latitude);
        data.put("longitude", s.longitude);
        data.put("likes", 0);
        data.put("createdAt", com.google.firebase.Timestamp.now());

        // Bild: sowohl imageUrl (alt) als auch imageList (neu) speichern
        data.put("imageUrl", s.imageUrl);
        java.util.List<String> list = new java.util.ArrayList<>();
        if (s.imageUrl != null && !s.imageUrl.isEmpty()) list.add(s.imageUrl);
        data.put("imageList", list);

        // Typ übernehmen (Fallback "Allgemein")
        data.put("type", (s.type != null && !s.type.isEmpty()) ? s.type : "Allgemein");

        // 2) Falls ID schon existiert (Duplikat), Timestamp anhängen
        locRef.get().addOnSuccessListener(doc -> {
            DocumentReference target = doc.exists()
                    ? db.collection("locations").document(baseId + "_" + System.currentTimeMillis())
                    : locRef;

            target.set(data).addOnSuccessListener(unused -> {
                db.collection("suggestions").document(s.id)
                        .update("status", "approved",
                                "approvedAt", com.google.firebase.Timestamp.now(),
                                "locationId", target.getId())
                        .addOnSuccessListener(v -> Toast.makeText(this, "Vorschlag bestätigt", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Update fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }).addOnFailureListener(e ->
                    Toast.makeText(this, "Location anlegen fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Prüfung auf Duplikat fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    @Override
    public void onReject(@NonNull Suggestion s, int position) {
        // Optional: Bild aus Storage löschen
        if (s.imageUrl != null && !s.imageUrl.isEmpty()) {
            com.google.firebase.storage.FirebaseStorage.getInstance()
                    .getReferenceFromUrl(s.imageUrl).delete();
        }

        db.collection("suggestions").document(s.id)
                .update("status", "rejected", "rejectedAt", Timestamp.now())
                .addOnSuccessListener(unused -> Toast.makeText(this, "Abgelehnt", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}