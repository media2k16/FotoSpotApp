package com.photospot.fotospotapp;

import com.photospot.fotospotapp.BuildConfig;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int GOOGLE_REAUTH_REQUEST = 9001;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private ImageView profileImage, toggleCurrentPasswordVisibility, toggleNewPasswordVisibility, toggleConfirmPasswordVisibility;
    private TextView emailText, usernameText, likesCount;
    private EditText currentPasswordInput, newPasswordInput, confirmPasswordInput;
    private Button savePasswordBtn, deleteImageBtn, uploadImageBtn, deleteAccountBtn;
    private ProgressBar uploadProgressBar;
    private Uri imageUri;

    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        googleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build());

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        profileImage = findViewById(R.id.profileImage);
        usernameText = findViewById(R.id.usernameText);
        emailText = findViewById(R.id.emailText);
        likesCount = findViewById(R.id.likesCount);
        uploadImageBtn = findViewById(R.id.uploadImageBtn);
        deleteImageBtn = findViewById(R.id.deleteImageBtn);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        currentPasswordInput = findViewById(R.id.currentPasswordInput);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        savePasswordBtn = findViewById(R.id.savePasswordBtn);
        toggleCurrentPasswordVisibility = findViewById(R.id.toggleCurrentPasswordVisibility);
        toggleNewPasswordVisibility = findViewById(R.id.toggleNewPasswordVisibility);
        toggleConfirmPasswordVisibility = findViewById(R.id.toggleConfirmPasswordVisibility);
        deleteAccountBtn = findViewById(R.id.deleteAccountBtn);

        emailText.setText(user.getEmail());

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    String username = snapshot.getString("username");
                    usernameText.setText(username != null && !username.isEmpty() ? username : "Benutzername nicht gesetzt");
                });

        db.collection("likes")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> likesCount.setText("Likes: " + querySnapshot.size()))
                .addOnFailureListener(e -> likesCount.setText("Anzahl geliketer Locations: 0"));

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String imageUrl = doc.getString("profileImageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(this).load(imageUrl).circleCrop().into(profileImage);
                    }
                });

        uploadImageBtn.setOnClickListener(v -> showImagePickerDialog());
        profileImage.setOnClickListener(v -> showImagePickerDialog());
        deleteImageBtn.setOnClickListener(v -> deleteProfileImage());
        savePasswordBtn.setOnClickListener(v -> changePassword());

        toggleCurrentPasswordVisibility.setOnClickListener(v -> togglePasswordVisibility(currentPasswordInput));
        toggleNewPasswordVisibility.setOnClickListener(v -> togglePasswordVisibility(newPasswordInput));
        toggleConfirmPasswordVisibility.setOnClickListener(v -> togglePasswordVisibility(confirmPasswordInput));

        deleteAccountBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Account wirklich löschen?")
                    .setMessage("Bist du sicher, dass du deinen Account dauerhaft löschen willst? Das kann nicht rückgängig gemacht werden.")
                    .setPositiveButton("Ja, löschen \uD83E\uDD7A", (dialog, which) -> {
                        if (user == null) return;

                        boolean isGoogleUser = false;
                        for (UserInfo info : user.getProviderData()) {
                            if ("google.com".equals(info.getProviderId())) {
                                isGoogleUser = true;
                                break;
                            }
                        }

                        if (isGoogleUser) {
                            startGoogleReauthentication();
                        } else {
                            showDeleteAccountDialog();
                        }
                    })
                    .setNegativeButton("Abbrechen", null)
                    .show();
        });

    }



    private void startGoogleReauthentication() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, GOOGLE_REAUTH_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GOOGLE_REAUTH_REQUEST) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account == null) throw new ApiException(new Status(13));
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                FirebaseUser user = mAuth.getCurrentUser();
                user.reauthenticate(credential)
                        .addOnSuccessListener(aVoid -> deleteAccount())
                        .addOnFailureListener(e -> Toast.makeText(this, "Google Reauth fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Toast.makeText(this, "Fehler bei Google Sign-In", Toast.LENGTH_SHORT).show();
            }
        }

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                imageUri = data.getData();
                uploadImageToFirebase();
            } else if (requestCode == CAMERA_REQUEST && data != null && data.getExtras() != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                Uri tempUri = getImageUriFromBitmap(photo);
                if (tempUri != null) {
                    imageUri = tempUri;
                    uploadImageToFirebase();
                }
            }
        }
    }

    private void deleteAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        StorageReference ref = storage.getReference().child("profileImages").child(userId + ".jpg");

        // 🔹 Erst Firestore löschen
        db.collection("users").document(userId).delete()
                .addOnSuccessListener(aVoid -> {

                    // 🔹 Versuche Profilbild zu löschen (aber Account wird in jedem Fall gelöscht)
                    ref.delete()
                            .addOnSuccessListener(aVoid1 -> {
                                // Bild erfolgreich gelöscht → lösche Account
                                proceedToDeleteUser();
                            })
                            .addOnFailureListener(e -> {
                                // Datei nicht vorhanden? Egal → Account trotzdem löschen
                                if (e.getMessage() != null && e.getMessage().contains("Object does not exist")) {
                                    proceedToDeleteUser(); // <<< Account löschen auch wenn kein Bild
                                } else {
                                    Toast.makeText(this, "Fehler beim Löschen des Bildes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Fehler beim Löschen des Benutzerprofils", Toast.LENGTH_SHORT).show());
    }
    private void proceedToDeleteUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        user.delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Account gelöscht", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Fehler beim Account-Löschen: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private void showImagePickerDialog() {
        String[] options = {"Foto aufnehmen", "Aus Galerie wählen"};
        new AlertDialog.Builder(this)
                .setTitle("Profilbild wählen")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "ProfilePic", null);
        return Uri.parse(path);
    }

    private void uploadImageToFirebase() {
        if (imageUri == null) return;
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        StorageReference ref = storage.getReference().child("profileImages").child(user.getUid() + ".jpg");

        uploadProgressBar.setVisibility(View.VISIBLE);
        profileImage.setVisibility(View.INVISIBLE);

        // Versuchen, Metadaten abzurufen (existiert die Datei?)
        ref.getMetadata()
                .addOnSuccessListener(metadata -> {
                    // Datei existiert → löschen und dann hochladen
                    ref.delete()
                            .addOnSuccessListener(aVoid -> uploadNewImage(ref, user))
                            .addOnFailureListener(e -> {
                                // Fehler beim Löschen → trotzdem weiter mit Upload
                                uploadNewImage(ref, user);
                            });
                })
                .addOnFailureListener(e -> {
                    // Datei existiert NICHT → direkt hochladen
                    uploadNewImage(ref, user);
                });
    }


    private void uploadNewImage(StorageReference ref, FirebaseUser user) {
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    Map<String, Object> update = new HashMap<>();
                    update.put("profileImageUrl", uri.toString());

                    db.collection("users").document(user.getUid()).update(update)
                            .addOnSuccessListener(aVoid -> {
                                Glide.with(this).load(uri).circleCrop().into(profileImage);
                                profileImage.setVisibility(View.VISIBLE);
                                Toast.makeText(this, "Profilbild aktualisiert 😄", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Fehler beim Speichern", Toast.LENGTH_SHORT).show());

                    uploadProgressBar.setVisibility(View.GONE);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Fehler beim Hochladen", Toast.LENGTH_SHORT).show();
                    uploadProgressBar.setVisibility(View.GONE);
                    profileImage.setVisibility(View.VISIBLE);
                });
    }
    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_account, null);
        EditText passwordInput = dialogView.findViewById(R.id.deletePasswordInput);

        builder.setView(dialogView)
                .setTitle("Account löschen")
                .setPositiveButton("Löschen", (dialog, which) -> {
                    String password = passwordInput.getText().toString().trim();
                    FirebaseUser user = mAuth.getCurrentUser();

                    if (password.isEmpty()) {
                        Toast.makeText(this, "Bitte Passwort eingeben", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
                    user.reauthenticate(credential)
                            .addOnSuccessListener(aVoid -> deleteAccount())
                            .addOnFailureListener(e -> Toast.makeText(this, "Passwort falsch oder Re-Auth fehlgeschlagen", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void deleteProfileImage() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        StorageReference ref = storage.getReference().child("profileImages").child(user.getUid() + ".jpg");
        ref.delete()
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> update = new HashMap<>();
                    update.put("profileImageUrl", "");
                    db.collection("users").document(user.getUid()).update(update)
                            .addOnSuccessListener(unused -> {
                                profileImage.setImageResource(R.drawable.ic_profile_placeholder);
                                Toast.makeText(this, "Du hast dein Profilbild entfernt 😔", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Du hast das Profilbild schon gelöscht 😅", Toast.LENGTH_SHORT).show());
    }

    private void changePassword() {
        String currentPass = currentPasswordInput.getText().toString().trim();
        String newPass = newPasswordInput.getText().toString().trim();
        String confirmPass = confirmPasswordInput.getText().toString().trim();

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Bitte alle Felder ausfüllen", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Passwörter stimmen nicht überein", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6) {
            Toast.makeText(this, "Passwort muss mindestens 6 Zeichen lang sein", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);
        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> user.updatePassword(newPass)
                        .addOnSuccessListener(unused -> Toast.makeText(this, "Passwort aktualisiert", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Fehler beim Aktualisieren", Toast.LENGTH_SHORT).show()))
                .addOnFailureListener(e -> Toast.makeText(this, "Re-Authentifizierung fehlgeschlagen", Toast.LENGTH_SHORT).show());
    }

    private void togglePasswordVisibility(EditText passwordField) {
        int inputType = passwordField.getInputType();
        if ((inputType & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        }
        passwordField.setSelection(passwordField.getText().length());
    }

}
