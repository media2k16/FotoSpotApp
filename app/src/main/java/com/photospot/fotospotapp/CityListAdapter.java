package com.photospot.fotospotapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

public class CityListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CITY = 1;

    private final List<Object> items = new ArrayList<>();
    private final OnCityClickListener listener;

    public interface OnCityClickListener {
        void onCityClick(String cityName);
    }

    public CityListAdapter(List<String> cities, OnCityClickListener listener) {
        this.listener = listener;
        updateCities(cities); // direkt initialisieren
    }

    // 🔁 Wird von der Suchfunktion aufgerufen
    public void updateCities(List<String> newCities) {
        items.clear();

        Map<String, List<String>> grouped = new TreeMap<>();
        for (String city : newCities) {
            if (city != null && !city.trim().isEmpty()) {
                String letter = city.substring(0, 1).toUpperCase(Locale.ROOT);
                grouped.computeIfAbsent(letter, k -> new ArrayList<>()).add(city);
            }
        }

        for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
            items.add(entry.getKey());           // Header
            Collections.sort(entry.getValue());  // Sicherheitshalber sortieren
            items.addAll(entry.getValue());      // Cities
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        return (item instanceof String && ((String) item).length() == 1)
                ? TYPE_HEADER : TYPE_CITY;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_city_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_city_name, parent, false);
            return new CityViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).headerText.setText((String) item);
        } else if (holder instanceof CityViewHolder) {
            String city = (String) item;
            ((CityViewHolder) holder).cityName.setText(city);
            holder.itemView.setOnClickListener(v -> listener.onCityClick(city));
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerText;

        HeaderViewHolder(View view) {
            super(view);
            headerText = view.findViewById(R.id.headerText);
        }
    }

    static class CityViewHolder extends RecyclerView.ViewHolder {
        TextView cityName;

        CityViewHolder(View view) {
            super(view);
            cityName = view.findViewById(R.id.cityName);
        }
    }
}
