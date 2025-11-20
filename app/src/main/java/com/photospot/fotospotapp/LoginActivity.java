package com.photospot.fotospotapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private Button loginButton, registerButton;
    private TextView forgotPasswordText;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        setContentView(R.layout.activity_login);

        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        forgotPasswordText = findViewById(R.id.tvForgotPassword);

        Button googleSignInCustomButton = findViewById(R.id.googleSignInCustomButton);
        googleSignInCustomButton.setOnClickListener(v -> signInWithGoogle());

        loginButton.setOnClickListener(v -> loginUser());
        registerButton.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mGoogleSignInClient.signOut();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            checkUserSetup(currentUser.getUid(), currentUser.getEmail());
        }
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Bitte E-Mail und Passwort eingeben ☝️", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        checkUserSetup(currentUser.getUid(), currentUser.getEmail());
                    } else {
                        Toast.makeText(this, "Login fehlgeschlagen ❌", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserSetup(String uid, String email) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        createUserDocument(uid, email);
                    } else if (!snapshot.contains("username")) {
                        goToUsername(uid, email);
                    } else {
                        goToApp();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Fehler beim Benutzer-Check: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createUserDocument(String uid, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("username", "");
        user.put("profileImageUrl", "");
        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> goToUsername(uid, email))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler beim Nutzer anlegen: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void goToUsername(String uid, String email) {
        Intent intent = new Intent(this, UsernameActivity.class);
        intent.putExtra("uid", uid);
        intent.putExtra("email", email);
        startActivity(intent);
        finish();
    }

    private void goToApp() {
        startActivity(new Intent(this, ChangelogActivity.class));
        finish();
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Passwort zurücksetzen");

        final EditText input = new EditText(this);
        input.setHint("Gib deine E-Mail ein");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Zurücksetzen", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "E-Mail zum Zurücksetzen wurde gesendet", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Fehler: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Bitte E-Mail eingeben", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google Anmeldung fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        checkUserSetup(user.getUid(), user.getEmail());
                    } else {
                        Toast.makeText(this, "Firebase Auth fehlgeschlagen", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}