package com.photospot.fotospotapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UsernameActivity extends AppCompatActivity {

    private EditText usernameInput;
    private Button confirmButton;
    private FirebaseFirestore db;

    private String uid, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_username);

        usernameInput = findViewById(R.id.usernameInput);
        confirmButton = findViewById(R.id.confirmButton);
        db = FirebaseFirestore.getInstance();

        uid = getIntent().getStringExtra("uid");
        email = getIntent().getStringExtra("email");

        confirmButton.setOnClickListener(v -> saveUsername());
    }

    private void saveUsername() {
        String username = usernameInput.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(this, "Benutzername darf nicht leer sein", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prüfen, ob Username bereits existiert
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Toast.makeText(this, "Benutzername ist bereits vergeben", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("username", username);
                    userMap.put("email", email);
                    userMap.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("users")
                            .document(uid)
                            .set(userMap)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Benutzername gespeichert", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, CityListActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler beim Prüfen: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onBackPressed() {
        FirebaseAuth.getInstance().signOut();
        super.onBackPressed();
    }
}