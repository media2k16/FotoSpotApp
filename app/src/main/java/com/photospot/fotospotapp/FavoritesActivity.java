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

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FavoritesAdapter adapter;
    private final List<LocationModel> favoriteList = new ArrayList<>();
    private TextView emptyTextView;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

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

        // Beim Entfernen im neuen Schema löschen: favorites/{locationId_userId}
        adapter = new FavoritesAdapter(favoriteList, this, (position, location) -> {
            String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
            if (uid == null) return;
            String favDocId = location.getId() + "_" + uid;
            db.collection("favorites").document(favDocId)
                    .delete()
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Löschen fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        recyclerView.setAdapter(adapter);
        loadFavorites();
        setupSwipeToDelete();
    }

    /** Lädt Favoriten (Top-Level 'favorites') für den aktuellen Nutzer und danach die jeweiligen Locations (einzeln, robust). */
    private void loadFavorites() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Bitte anmelden", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        db.collection("favorites")
                .whereEqualTo("userId", uid) // wichtig für die Rules
                .get()
                .addOnSuccessListener(favSnap -> {
                    List<String> locationIds = new ArrayList<>();
                    for (DocumentSnapshot d : favSnap.getDocuments()) {
                        String locId = d.getString("locationId");
                        if (locId != null) locationIds.add(locId);
                    }
                    fetchLocationsByIdsSimple(locationIds);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Fehler beim Laden der Favoriten: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmptyIfNeeded();
                });
    }

    /** Robuste Variante: jede Location einzeln laden (kein whereIn, kein generisches Deserialisieren). */
    private void fetchLocationsByIdsSimple(List<String> ids) {
        favoriteList.clear();

        if (ids.isEmpty()) {
            adapter.notifyDataSetChanged();
            showEmptyIfNeeded();
            return;
        }

        List<com.google.android.gms.tasks.Task<?>> tasks = new ArrayList<>();
        for (String id : ids) {
            tasks.add(
                    db.collection("locations").document(id).get()
                            .addOnSuccessListener(d -> {
                                if (!d.exists()) return;

                                try {
                                    // 🔧 imageList sicher extrahieren (ohne generische Deserialisierung)
                                    List<String> imageList = new ArrayList<>();
                                    Object raw = d.get("imageList"); // kann null, List<?> oder was anderes sein
                                    if (raw instanceof List) {
                                        for (Object item : (List<?>) raw) {
                                            if (item != null) imageList.add(item.toString());
                                        }
                                    }

                                    LocationModel m = new LocationModel(
                                            d.getId(),
                                            imageList,                                // List<String> (sicher gebaut)
                                            d.getString("info"),
                                            d.getString("streetName"),
                                            d.getString("city"),
                                            d.getString("type")
                                    );
                                    favoriteList.add(m);
                                } catch (Exception ex) {
                                    // Falls ein Dokument unerwartete Felder hat: einfach überspringen und weitermachen
                                    ex.printStackTrace();
                                }
                            })
            );
        }

        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(done -> {
                    adapter.notifyDataSetChanged();
                    showEmptyIfNeeded();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Fehler beim Laden der Locations: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmptyIfNeeded();
                });
    }

    private void showEmptyIfNeeded() {
        emptyTextView.setVisibility(favoriteList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback =
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();
                        if (position < 0 || position >= favoriteList.size()) return;

                        LocationModel location = favoriteList.get(position);

                        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
                        if (uid == null) {
                            adapter.notifyItemChanged(position); // swipe rückgängig
                            return;
                        }
                        String favDocId = location.getId() + "_" + uid;

                        db.collection("favorites").document(favDocId)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    adapter.removeItem(position);
                                    showEmptyIfNeeded();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(FavoritesActivity.this, "Löschen fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    adapter.notifyItemChanged(position); // swipe rückgängig
                                });
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