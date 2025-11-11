package com.photospot.fotospotapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

    private final List<LocationModel> favorites;
    private final Context context;
    private final OnDeleteClickListener deleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int position, LocationModel location);
    }

    public FavoritesAdapter(List<LocationModel> favorites, Context context, OnDeleteClickListener deleteClickListener) {
        this.favorites = favorites;
        this.context = context;
        this.deleteClickListener = deleteClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocationModel location = favorites.get(position);

        // Titel/Untertitel
        String street = location.getStreetName();
        holder.title.setText(street != null && !street.isEmpty() ? street : "Unbekannte Straße");
        holder.subtitle.setText(location.getInfo() != null ? location.getInfo() : "");

        // Bild laden: aus imageList das erste Bild verwenden
        List<String> imgs = location.getImageList();
        if (imgs != null && !imgs.isEmpty() && imgs.get(0) != null && !imgs.get(0).isEmpty()) {
            Glide.with(context).load(imgs.get(0)).into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.sample_location); // Fallback
        }

        // Item-Klick -> Detailseite mit locationId
        holder.itemView.setOnClickListener(v -> {
            String id = location.getId();
            if (id != null && !id.isEmpty()) {
                Intent intent = new Intent(context, LocationDetailActivity.class);
                intent.putExtra("locationId", id);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Keine Location-ID übergeben", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return favorites.size();
    }

    public void removeItem(int position) {
        if (position < 0 || position >= favorites.size()) return;
        LocationModel location = favorites.get(position);
        favorites.remove(position);
        notifyItemRemoved(position);
        if (deleteClickListener != null) {
            deleteClickListener.onDeleteClick(position, location);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        ImageView image;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.favoriteStreet);
            subtitle = itemView.findViewById(R.id.favoriteInfo);
            image = itemView.findViewById(R.id.favoriteImage);
        }
    }
}