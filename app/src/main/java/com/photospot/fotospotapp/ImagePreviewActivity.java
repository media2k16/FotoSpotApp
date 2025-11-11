package com.photospot.fotospotapp;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        ImageView iv = findViewById(R.id.ivFull);
        String url = getIntent().getStringExtra("url");

        Glide.with(this).load(url).into(iv);

        // Tap to close
        iv.setOnClickListener(v -> finish());
    }
}