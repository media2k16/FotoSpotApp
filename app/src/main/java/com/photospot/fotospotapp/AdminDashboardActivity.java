package com.photospot.fotospotapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdminDashboardActivity extends AppCompatActivity {

    private Button addSpotButton;
    private Button reviewSuggestionsButton; // NEU

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        addSpotButton = findViewById(R.id.addSpotButton);
        addSpotButton.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminAddSpotActivity.class));
        });

        reviewSuggestionsButton = findViewById(R.id.reviewSuggestionsButton);
        reviewSuggestionsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminSuggestionsActivity.class));
        });
    }
}