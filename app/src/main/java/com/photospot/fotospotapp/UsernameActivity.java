package com.photospot.fotospotapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
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

        confirmButton.setOnClickListener(v -> {
            String username = usernameInput.getText().toString().trim();

            if (username.isEmpty()) {
                Toast.makeText(this, "Benutzername darf nicht leer sein", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(this, "Benutzername ist bereits vergeben", Toast.LENGTH_SHORT).show();
                        } else {
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("username", username);
                            userMap.put("email", email);

                            db.collection("users").document(uid)
                                    .set(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Benutzername gespeichert", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(UsernameActivity.this, CityListActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
        });
    }

    @Override
    public void onBackPressed() {
        FirebaseAuth.getInstance().signOut(); // Nutzer automatisch abmelden
        super.onBackPressed(); // Normales Zurückverhalten
    }
}
