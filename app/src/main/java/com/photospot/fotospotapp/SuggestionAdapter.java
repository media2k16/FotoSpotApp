package com.photospot.fotospotapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.VH> {

    public interface SuggestionActionListener {
        void onApprove(@NonNull Suggestion s, int position);
        void onReject(@NonNull Suggestion s, int position);
    }

    private final List<Suggestion> items;
    private final SuggestionActionListener listener;

    public SuggestionAdapter(List<Suggestion> items, SuggestionActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_suggestion, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Suggestion s = items.get(pos);
        h.city.setText(s.city != null ? s.city : "–");
        h.street.setText(s.street != null ? s.street : "–");
        h.note.setText(s.note != null ? s.note : "–");
        h.coords.setText(s.latitude + ", " + s.longitude);
        h.user.setText((s.username != null && !s.username.isEmpty()) ? s.username : (s.email != null ? s.email : "Unbekannt"));

        h.btnApprove.setOnClickListener(v -> listener.onApprove(s, h.getAdapterPosition()));
        h.btnReject.setOnClickListener(v -> listener.onReject(s, h.getAdapterPosition()));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView city, street, note, coords, user;
        Button btnApprove, btnReject;
        VH(@NonNull View v) {
            super(v);
            city = v.findViewById(R.id.tvCity);
            street = v.findViewById(R.id.tvStreet);
            note = v.findViewById(R.id.tvNote);
            coords = v.findViewById(R.id.tvCoords);
            user = v.findViewById(R.id.tvUser);
            btnApprove = v.findViewById(R.id.btnApprove);
            btnReject  = v.findViewById(R.id.btnReject);
        }
    }
}