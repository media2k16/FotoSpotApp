package com.photospot.fotospotapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView adminAccessBtn;

    // 🔁 Neu für In-App-Kauf
    private BillingClient billingClient;
    private final String PRODUCT_ID = "remove_ads"; // muss exakt wie in Play Console sein!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        TextView profile = findViewById(R.id.profile);
        TextView addLocation = findViewById(R.id.addLocation);
        TextView removeAds = findViewById(R.id.removeAds); // <- neu: Button in XML
        TextView reportBug = findViewById(R.id.reportBug);
        TextView logout = findViewById(R.id.logout);
        TextView openFavorites = findViewById(R.id.openFavorites);
        adminAccessBtn = findViewById(R.id.adminAccessBtn);
        adminAccessBtn.setVisibility(View.GONE); // Standardmäßig versteckt

        // Wenn Nutzer eingeloggt, Adminstatus prüfen
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkIfAdmin(currentUser.getEmail());
        }

        // BillingClient initialisieren
        setupBillingClient();

        // Klick-Events
        profile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        addLocation.setOnClickListener(v -> startActivity(new Intent(this, AddLocationActivity.class)));
        openFavorites.setOnClickListener(v -> startActivity(new Intent(this, FavoritesActivity.class)));

        // 🚫 Werbung entfernen (In-App-Kauf starten)
        removeAds.setOnClickListener(v -> launchPurchaseFlow());

        //Fehler melden
        reportBug.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"bugreport@fotospotz.de"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Fehler melden - FotoSpotz");
            startActivity(Intent.createChooser(emailIntent, "Fehler melden"));
        });

        TextView changelogItem = findViewById(R.id.changelogItem);
        changelogItem.setOnClickListener(v -> {
            Intent i = new Intent(SettingsActivity.this, ChangelogActivity.class);
            i.putExtra("launched_from_settings", true);
            startActivity(i);
        });

        //Logout
        logout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        adminAccessBtn.setOnClickListener(v -> showAdminLoginDialog());
    }

    // Prüfe ob E-Mail in Firestore als Admin eingetragen ist
    private void checkIfAdmin(String email) {
        db.collection("admins").document(email).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    if ("admin".equals(role)) {
                        adminAccessBtn.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler beim Prüfen des Adminstatus", Toast.LENGTH_SHORT).show()
                );
    }

    // Passwortdialog anzeigen
    private void showAdminLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔐 Admin-Login");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText input = new EditText(this);
        input.setHint("Admin-Passwort eingeben");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setBackgroundResource(android.R.drawable.edit_text);
        input.setPadding(20, 20, 20, 20);
        layout.addView(input);

        final TextView toggleVisibility = new TextView(this);
        toggleVisibility.setText("👁️ Passwort anzeigen");
        toggleVisibility.setPadding(20, 10, 20, 20);
        toggleVisibility.setTextSize(14);
        toggleVisibility.setOnClickListener(v -> {
            if (input.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleVisibility.setText("🚫👁️ Passwort verbergen");
            } else {
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleVisibility.setText("👁️ Passwort anzeigen");
            }
            input.setSelection(input.getText().length());
        });
        layout.addView(toggleVisibility);

        builder.setView(layout);

        builder.setPositiveButton("Login", (dialog, which) -> {
            String enteredPassword = input.getText().toString().trim();
            if (enteredPassword.equals("##FoSpW0401!")) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
            } else {
                Toast.makeText(this, "❌ Falsches Passwort", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // 🔁 Neu: BillingClient Setup
    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .setListener((billingResult, purchases) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                        for (Purchase purchase : purchases) {
                            if (purchase.getProducts().contains(PRODUCT_ID)) {
                                handlePurchase(purchase);
                            }
                        }
                    }
                })
                .enablePendingPurchases()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                // ready to go
            }

            @Override
            public void onBillingServiceDisconnected() {
                // reconnect logic
            }
        });
    }

    private void launchPurchaseFlow() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());

        billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder().setProductList(productList).build(),
                (billingResult, productDetailsList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && !productDetailsList.isEmpty()) {
                        ProductDetails productDetails = productDetailsList.get(0);
                        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                                List.of(BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(productDetails)
                                        .build());

                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                .setProductDetailsParamsList(productDetailsParamsList)
                                .build();

                        billingClient.launchBillingFlow(this, billingFlowParams);
                    }
                }
        );
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgeParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();

                billingClient.acknowledgePurchase(acknowledgeParams, billingResult -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        setProUser();
                    }
                });
            } else {
                setProUser();
            }
        }
    }

    private void setProUser() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("isProUser", true).apply();

        Toast.makeText(this, "✅ Werbung erfolgreich entfernt!", Toast.LENGTH_LONG).show();
    }
}