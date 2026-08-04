package com.teapp.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.teapp.R;
import com.teapp.model.Child;
import com.teapp.util.AvatarUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

public class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.ViewHolder> {

    public interface Listener {
        void onAgenda(Child child);
        void onEdit(Child child);
        void onDelete(Child child);
    }

    private final List<Child> items;
    private final Listener listener;

    public ChildAdapter(List<Child> items, Listener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Child child = items.get(position);

        h.tvName.setText(child.name);

        // Edad calculada
        String edad = calcularEdad(child.dateOfBirth);
        h.tvAge.setText(edad);

        // Avatar: emoji del catálogo, foto subida, o iniciales sobre el color
        AvatarUtils.mostrarAvatar(h.tvInitials, h.imgAvatar,
                child.avatarBase64, child.avatarColor, child.getInitials());

        h.btnAgenda.setOnClickListener(v -> listener.onAgenda(child));
        h.btnEdit.setOnClickListener(v -> listener.onEdit(child));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(child));
    }

    private String calcularEdad(String dateOfBirth) {
        if (dateOfBirth == null || dateOfBirth.isEmpty()) return "";
        try {
            LocalDate nacimiento = LocalDate.parse(dateOfBirth);
            int edad = Period.between(nacimiento, LocalDate.now()).getYears();
            return edad + " años";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitials, tvName, tvAge;
        ImageView imgAvatar;
        Button btnAgenda;
        ImageButton btnEdit, btnDelete;

        ViewHolder(View v) {
            super(v);
            tvInitials = v.findViewById(R.id.tv_initials);
            imgAvatar  = v.findViewById(R.id.img_avatar);
            tvName     = v.findViewById(R.id.tv_name);
            tvAge      = v.findViewById(R.id.tv_age);
            btnAgenda  = v.findViewById(R.id.btn_agenda);
            btnEdit    = v.findViewById(R.id.btn_edit);
            btnDelete  = v.findViewById(R.id.btn_delete);
        }
    }
}
