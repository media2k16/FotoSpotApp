package com.photospot.fotospotapp;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FavoritesAdapter adapter;
    private List<LocationModel> favoriteList = new ArrayList<>();
    private TextView emptyTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(0, topInset, 0, 0);
            return insets;
        });


        recyclerView = findViewById(R.id.favoritesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        emptyTextView = findViewById(R.id.emptyTextView);

        adapter = new FavoritesAdapter(favoriteList, this, (position, location) -> {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore.getInstance()
                    .collection("favorites")
                    .document(userId)
                    .collection("locations")
                    .document(location.getId())
                    .delete();
        });

        recyclerView.setAdapter(adapter);
        loadFavorites();
        setupSwipeToDelete();
    }

    private void loadFavorites() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("favorites")
                .document(userId)
                .collection("locations")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    favoriteList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        LocationModel location = doc.toObject(LocationModel.class);

                        // 🟢 Hier setzen wir die ID manuell
                        location.setId(doc.getId());

                        favoriteList.add(location);
                    }
                    adapter.notifyDataSetChanged();

                    // Sichtbarkeit aktualisieren
                    if (favoriteList.isEmpty()) {
                        emptyTextView.setVisibility(View.VISIBLE);
                    } else {
                        emptyTextView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler beim Laden der Favoriten", Toast.LENGTH_SHORT).show());
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                adapter.removeItem(position);

                if (favoriteList.isEmpty()) {
                    emptyTextView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                Paint paint = new Paint();
                paint.setColor(ContextCompat.getColor(FavoritesActivity.this, R.color.swipe_delete_red));
                Drawable icon = ContextCompat.getDrawable(FavoritesActivity.this, R.drawable.ic_delete);

                if (dX != 0 && icon != null) {
                    float left, right;
                    View itemView = viewHolder.itemView;
                    if (dX > 0) {
                        left = itemView.getLeft();
                        right = itemView.getLeft() + dX;
                    } else {
                        left = itemView.getRight() + dX;
                        right = itemView.getRight();
                    }
                    c.drawRect(left, itemView.getTop(), right, itemView.getBottom(), paint);

                    int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconBottom = iconTop + icon.getIntrinsicHeight();

                    int iconLeft, iconRight;
                    if (dX > 0) {
                        iconLeft = itemView.getLeft() + iconMargin;
                        iconRight = iconLeft + icon.getIntrinsicWidth();
                    } else {
                        iconRight = itemView.getRight() - iconMargin;
                        iconLeft = iconRight - icon.getIntrinsicWidth();
                    }
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    icon.draw(c);
                }
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }
}
