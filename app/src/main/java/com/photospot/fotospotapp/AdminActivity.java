package com.photospot.fotospotapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Arrays;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    ListView suggestionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }


        suggestionList = findViewById(R.id.suggestionListView);
        List<String> suggestions = Arrays.asList(
            "Düsseldorf - Rheinuferpromenade",
            "Wuppertal - Schwebebahnbrücke"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, suggestions);
        suggestionList.setAdapter(adapter);

        suggestionList.setOnItemClickListener((parent, view, position, id) -> {
            String selected = suggestions.get(position);
            showAdminOptions(selected);
        });
    }

    private void showAdminOptions(String location) {
        new AlertDialog.Builder(this)
            .setTitle("Location prüfen")
            .setMessage("Was möchtest du mit:\n\n" + location + "\nmachen?")
            .setPositiveButton("Freigeben", (dialog, which) -> {
                Toast.makeText(this, "Freigegeben!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Löschen", (dialog, which) -> {
                Toast.makeText(this, "Gelöscht!", Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Abbrechen", null)
            .show();
    }
}