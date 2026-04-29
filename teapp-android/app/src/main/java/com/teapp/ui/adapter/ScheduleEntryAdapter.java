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
import com.teapp.model.ScheduleEntry;


import java.util.List;

public class ScheduleEntryAdapter extends RecyclerView.Adapter<ScheduleEntryAdapter.ViewHolder> {

    public interface Listener {
        void onEntryClick(ScheduleEntry entry);
        void onEntryLongClick(ScheduleEntry entry);
    }

    private final List<ScheduleEntry> items;
    private final Listener listener;

    public ScheduleEntryAdapter(List<ScheduleEntry> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_entry, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ScheduleEntry entry = items.get(position);

        h.tvName.setText(entry.activity != null ? entry.activity.name : "—");

        if (entry.activity != null && entry.activity.color != null) {
            try {
                h.card.setCardBackgroundColor(Color.parseColor(entry.activity.color));
            } catch (Exception ignored) {}
        }

        // Pictogram
        if (entry.activity != null && entry.activity.pictogramUrl != null) {
            Glide.with(h.imgPictogram.getContext())
                    .load(entry.activity.pictogramUrl)
                    .placeholder(R.drawable.ic_activity_placeholder)
                    .into(h.imgPictogram);
            h.imgPictogram.setVisibility(View.VISIBLE);
        } else {
            h.imgPictogram.setVisibility(View.GONE);
        }

        // Duration
        if (entry.durationMinutes != null && entry.durationMinutes > 0) {
            h.tvDuration.setText(entry.durationMinutes + " min");
            h.tvDuration.setVisibility(View.VISIBLE);
        } else {
            h.tvDuration.setVisibility(View.GONE);
        }

        // Completed state
        boolean completed = entry.isCompletedToday();
        h.ivCompleted.setVisibility(completed ? View.VISIBLE : View.GONE);
        h.card.setAlpha(completed ? 0.65f : 1f);

        h.card.setOnClickListener(v -> listener.onEntryClick(entry));
        h.card.setOnLongClickListener(v -> { listener.onEntryLongClick(entry); return true; });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvName, tvDuration;
        ImageView imgPictogram, ivCompleted;

        ViewHolder(View v) {
            super(v);
            card         = v.findViewById(R.id.card);
            tvName       = v.findViewById(R.id.tv_name);
            tvDuration   = v.findViewById(R.id.tv_duration);
            imgPictogram = v.findViewById(R.id.img_pictogram);
            ivCompleted  = v.findViewById(R.id.iv_completed);
        }
    }
}
