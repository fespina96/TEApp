package com.teapp.ui.adapter;

import android.graphics.Color;
import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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


import java.util.Collections;
import java.util.List;

public class ScheduleEntryAdapter extends RecyclerView.Adapter<ScheduleEntryAdapter.ViewHolder> {

    public interface Listener {
        void onEntryClick(ScheduleEntry entry);
        void onEntryLongClick(ScheduleEntry entry);
        /** Se dispara al tocar el asa: el fragment le pide al ItemTouchHelper que arranque. */
        default void onStartDrag(RecyclerView.ViewHolder holder) {}
    }

    private final List<ScheduleEntry> items;
    private final Listener listener;
    private boolean reordenable = false;

    public ScheduleEntryAdapter(List<ScheduleEntry> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    /** Muestra el asa de arrastre. Sólo en las agendas que se pueden editar. */
    public void setReordenable(boolean reordenable) {
        this.reordenable = reordenable;
        notifyDataSetChanged();
    }

    /** Reacomoda la lista mientras se arrastra; el orden se persiste al soltar. */
    public void moverItem(int desde, int hasta) {
        if (desde < 0 || hasta < 0 || desde >= items.size() || hasta >= items.size()) return;
        Collections.swap(items, desde, hasta);
        notifyItemMoved(desde, hasta);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_entry, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint("ClickableViewAccessibility")
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

        h.ivDrag.setVisibility(reordenable ? View.VISIBLE : View.GONE);
        h.ivDrag.setOnTouchListener((v, ev) -> {
            if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) listener.onStartDrag(h);
            return false;
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvName, tvDuration;
        ImageView imgPictogram, ivCompleted, ivDrag;

        ViewHolder(View v) {
            super(v);
            card         = v.findViewById(R.id.card);
            tvName       = v.findViewById(R.id.tv_name);
            tvDuration   = v.findViewById(R.id.tv_duration);
            imgPictogram = v.findViewById(R.id.img_pictogram);
            ivCompleted  = v.findViewById(R.id.iv_completed);
            ivDrag       = v.findViewById(R.id.iv_drag);
        }
    }
}
