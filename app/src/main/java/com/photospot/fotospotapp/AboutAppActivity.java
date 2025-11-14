package com.photospot.fotospotapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AboutAppActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });

        // Überschrift / Version (optional)
        TextView tvVersion = findViewById(R.id.tvVersion);
        tvVersion.setText("FotoSpotz\nVersion 1.0.0 Beta"); // kannst du später dynamisch machen

        LinearLayout itemImpressum   = findViewById(R.id.itemImpressum);
        LinearLayout itemDatenschutz = findViewById(R.id.itemDatenschutz);
        LinearLayout itemAGB         = findViewById(R.id.itemAGB);

        itemImpressum.setOnClickListener(v ->
                openUrl("https://fotospotz.de/impressum"));

        itemDatenschutz.setOnClickListener(v ->
                openUrl("https://fotospotz.de/datenschutz"));

        itemAGB.setOnClickListener(v ->
                openUrl("https://fotospotz.de/agb"));
    }

    private void openUrl(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(i);
    }
}