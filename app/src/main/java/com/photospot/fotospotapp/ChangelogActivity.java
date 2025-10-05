package com.photospot.fotospotapp;

import static android.content.Intent.getIntent;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class ChangelogActivity extends AppCompatActivity {

    private TextView changelogText;
    private Button btnContinue;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changelog);

        // Safe Insets
        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        // Aus Settings geöffnet?
        boolean fromSettings = getIntent().getBooleanExtra("launched_from_settings", false);

        // Login-Check
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Falls jemand hier landet ohne Login → zurück zur LoginActivity
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }, 200);
            return;
        }

        changelogText = findViewById(R.id.changelogText);
        btnContinue = findViewById(R.id.btnContinue);
        db = FirebaseFirestore.getInstance();

        // Button-Verhalten abhängig von Herkunft
        if (fromSettings) {
            btnContinue.setText("Schließen");
            btnContinue.setOnClickListener(v -> finish());
        } else {
            btnContinue.setText("Weiter");
            btnContinue.setOnClickListener(v -> {
                Intent intent = new Intent(ChangelogActivity.this, CityListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish(); // Zurück zum Changelog verhindern
            });
        }

        loadChangelogFromFirestore();
    }

    private void loadChangelogFromFirestore() {
        db.collection("changelog")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder changelogBuilder = new StringBuilder();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String version = doc.getString("versionName");
                        @SuppressWarnings("unchecked")
                        List<String> entries = (List<String>) doc.get("entries");

                        changelogBuilder.append("📦 Version ").append(version != null ? version : "?").append(":\n");
                        if (entries != null) {
                            for (String entry : entries) {
                                changelogBuilder.append("• ").append(entry).append("\n");
                            }
                        }
                        changelogBuilder.append("\n");
                    }
                    changelogText.setText(changelogBuilder.toString());
                })
                .addOnFailureListener(e -> changelogText.setText("Fehler beim Laden des Changelogs"));
    }
}
