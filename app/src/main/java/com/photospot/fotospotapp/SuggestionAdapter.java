package com.photospot.fotospotapp;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import java.util.Locale;

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
    public void onBindViewHolder(@NonNull VH h, int position) {
        Suggestion s = items.get(position);

        h.tvCity.setText(s.city != null ? s.city : "-");
        h.tvStreet.setText(s.street != null ? s.street : "-");
        h.tvNote.setText(s.note != null ? s.note : "");
        h.tvCoords.setText(String.format(Locale.getDefault(), "%.6f, %.6f", s.latitude, s.longitude));
        h.tvUser.setText(s.submittedByEmail != null ? ("eingereicht von " + s.submittedByEmail) : "");

        Glide.with(h.ivThumbSmall.getContext())
                .load(s.imageUrl)
                .into(h.ivThumbSmall);

        h.ivThumbSmall.setOnClickListener(v -> {
            if (s.imageUrl != null && !s.imageUrl.isEmpty()) {
                Intent intent = new Intent(v.getContext(), ImagePreviewActivity.class);
                intent.putExtra("url", s.imageUrl);
                v.getContext().startActivity(intent);
            }
        });

        h.btnApprove.setOnClickListener(v -> {
            h.btnApprove.setEnabled(false); h.btnReject.setEnabled(false);
            listener.onApprove(s, position);
        });

        h.btnReject.setOnClickListener(v -> {
            h.btnApprove.setEnabled(false); h.btnReject.setEnabled(false);
            listener.onReject(s, position);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCity, tvStreet, tvNote, tvCoords, tvUser;
        ImageView ivThumbSmall;
        Button btnApprove, btnReject;

        VH(@NonNull View itemView) {
            super(itemView);
            tvCity = itemView.findViewById(R.id.tvCity);
            tvStreet = itemView.findViewById(R.id.tvStreet);
            tvNote = itemView.findViewById(R.id.tvNote);
            tvCoords = itemView.findViewById(R.id.tvCoords);
            tvUser = itemView.findViewById(R.id.tvUser);
            ivThumbSmall = itemView.findViewById(R.id.ivThumbSmall);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}