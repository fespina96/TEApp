package com.teapp.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.teapp.R;
import com.teapp.model.ActivityItem;


import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {

    public interface Listener {
        void onSelect(ActivityItem activity);
    }

    private final List<ActivityItem> items;
    private final Listener listener;

    public ActivityAdapter(List<ActivityItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ActivityItem act = items.get(position);
        h.tvName.setText(act.name);
        h.tvCategory.setText(categoryLabel(act.category));

        if (act.color != null) {
            try {
                h.card.setCardBackgroundColor(Color.parseColor(act.color));
            } catch (Exception ignored) {}
        }

        if (act.pictogramUrl != null) {
            Glide.with(h.imgPictogram.getContext())
                    .load(act.pictogramUrl)
                    .placeholder(R.drawable.ic_activity_placeholder)
                    .into(h.imgPictogram);
            h.imgPictogram.setVisibility(View.VISIBLE);
        } else {
            h.imgPictogram.setVisibility(View.GONE);
        }

        h.card.setOnClickListener(v -> listener.onSelect(act));
    }

    private String categoryLabel(String cat) {
        if (cat == null) return "";
        switch (cat) {
            case "HYGIENE":       return "Higiene";
            case "MEAL":          return "Comidas";
            case "EDUCATION":     return "Educación";
            case "PLAY":          return "Juego";
            case "THERAPY":       return "Terapia";
            case "REST":          return "Descanso";
            case "OUTDOOR":       return "Aire libre";
            case "SPECIAL_EVENT": return "Evento especial";
            default:              return "Personalizada";
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvName, tvCategory;
        ImageView imgPictogram;

        ViewHolder(View v) {
            super(v);
            card         = v.findViewById(R.id.card);
            tvName       = v.findViewById(R.id.tv_name);
            tvCategory   = v.findViewById(R.id.tv_category);
            imgPictogram = v.findViewById(R.id.img_pictogram);
        }
    }
}
