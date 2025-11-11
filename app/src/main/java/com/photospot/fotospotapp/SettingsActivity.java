package com.photospot.fotospotapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView adminAccessBtn;

    // Billing (aus deiner Version)
    private BillingClient billingClient;
    private final String PRODUCT_ID = "remove_ads"; // exakt wie in Play Console!
    private boolean isAdmin = false; // <- NEU: Admin-Flag

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
        TextView removeAds = findViewById(R.id.removeAds);
        TextView reportBug = findViewById(R.id.reportBug);
        TextView logout = findViewById(R.id.logout);
        TextView openFavorites = findViewById(R.id.openFavorites);
        TextView changelogItem = findViewById(R.id.changelogItem);
        adminAccessBtn = findViewById(R.id.adminAccessBtn);
        adminAccessBtn.setVisibility(View.GONE); // standardmäßig versteckt

        // Adminstatus prüfen (nur wenn eingeloggt)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            checkIfAdmin(currentUser.getEmail());
        }

        // Billing vorbereiten
        setupBillingClient();

        // Klick-Events
        profile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        addLocation.setOnClickListener(v -> startActivity(new Intent(this, AddLocationActivity.class)));
        openFavorites.setOnClickListener(v -> startActivity(new Intent(this, FavoritesActivity.class)));
        removeAds.setOnClickListener(v -> launchPurchaseFlow());

        // Fehler melden (mit Fallback)
        reportBug.setOnClickListener(v -> {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"bugreport@fotospotz.de"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Fehler melden - FotoSpotz");
            if (emailIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(emailIntent, "Fehler melden"));
            } else {
                Toast.makeText(this, "Kein E-Mail-Client installiert", Toast.LENGTH_SHORT).show();
            }
        });

        changelogItem.setOnClickListener(v -> {
            Intent i = new Intent(SettingsActivity.this, ChangelogActivity.class);
            i.putExtra("launched_from_settings", true);
            startActivity(i);
        });

        // Logout
        logout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // 🔐 Admin-Button: kein Passwort mehr – Rolle prüfen und öffnen
        adminAccessBtn.setOnClickListener(v -> {
            if (!isAdmin) {
                Toast.makeText(this, "Kein Admin-Zugriff", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser u = mAuth.getCurrentUser();
            if (u == null || u.getEmail() == null) {
                Toast.makeText(this, "Bitte erneut anmelden", Toast.LENGTH_SHORT).show();
                return;
            }
            // bei Klick sicherheitshalber frisch gegen Firestore prüfen
            db.collection("admins").document(u.getEmail()).get()
                    .addOnSuccessListener(doc -> {
                        String role = doc.getString("role");
                        if ("admin".equals(role)) {
                            startActivity(new Intent(this, AdminDashboardActivity.class));
                        } else {
                            Toast.makeText(this, "Kein Admin-Zugriff", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Fehler beim Prüfen des Adminstatus", Toast.LENGTH_SHORT).show()
                    );
        });
    }

    /** Prüft die Adminrolle in Firestore und blendet den Button ein/aus. */
    private void checkIfAdmin(String email) {
        db.collection("admins").document(email).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String role = documentSnapshot.getString("role");
                    isAdmin = "admin".equals(role);
                    adminAccessBtn.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler beim Prüfen des Adminstatus", Toast.LENGTH_SHORT).show()
                );
    }

    /* ==================== Billing (unverändert + deine Logik) ==================== */

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
                // Optional: Käufe beim Start prüfen (Sync des Pro-Status)
                billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build(),
                        (result, purchases) -> {
                            boolean pro = false;
                            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                                for (Purchase p : purchases) {
                                    if (p.getProducts().contains(PRODUCT_ID)
                                            && p.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                        pro = true; break;
                                    }
                                }
                            }
                            getSharedPreferences("user_prefs", MODE_PRIVATE)
                                    .edit().putBoolean("isProUser", pro).apply();
                        }
                );
            }

            @Override
            public void onBillingServiceDisconnected() {
                // optional: reconnect-Strategie
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
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && productDetailsList != null && !productDetailsList.isEmpty()) {
                        ProductDetails productDetails = productDetailsList.get(0);
                        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                                java.util.Collections.singletonList(
                                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                                .setProductDetails(productDetails)
                                                .build()
                                );

                        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                                .setProductDetailsParamsList(productDetailsParamsList)
                                .build();

                        billingClient.launchBillingFlow(this, billingFlowParams);
                    } else {
                        Toast.makeText(this, "Kauf nicht verfügbar", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();
                billingClient.acknowledgePurchase(params, result -> {
                    if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
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