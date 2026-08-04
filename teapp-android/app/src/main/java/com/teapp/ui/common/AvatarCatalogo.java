package com.teapp.ui.common;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teapp.R;
import com.teapp.util.AvatarUtils;

/**
 * Catálogo de avatares predefinidos, compartido por el formulario de participante
 * y el perfil del usuario.
 *
 * Los emoji y los colores son los mismos que ofrece la versión web en
 * AvatarPickerDialogComponent, y el avatar se guarda en el mismo formato, así que
 * lo elegido en una plataforma se ve en la otra.
 */
public final class AvatarCatalogo {

    /** Cada opción es {emoji, color de fondo}. */
    public static final String[][] OPCIONES = {
            {"\uD83E\uDD8B", "#C9B8E8"}, {"\uD83C\uDF1F", "#FAF0BE"},
            {"\uD83D\uDE80", "#A8D8EA"}, {"\uD83D\uDC31", "#F9D8C0"},
            {"\uD83D\uDC36", "#FAF0BE"}, {"\uD83E\uDD81", "#F9D8C0"},
            {"\uD83D\uDC38", "#B8E0C8"}, {"\uD83E\uDD84", "#C9B8E8"},
            {"\uD83C\uDF08", "#A8D8EA"}, {"\uD83C\uDF3A", "#F9D8C0"},
            {"\uD83C\uDF3B", "#FAF0BE"}, {"\u2B50", "#FAF0BE"},
            {"\uD83C\uDFA8", "#C9B8E8"}, {"\uD83C\uDFB5", "#A8E0DA"},
            {"\u26BD", "#B8E0C8"}, {"\uD83E\uDDE9", "#A8D8EA"},
            {"\uD83C\uDFCA", "#A8E0DA"}, {"\uD83C\uDF19", "#C9B8E8"},
            {"\u2600\uFE0F", "#FAF0BE"}, {"\uD83D\uDC3B", "#F9D8C0"}
    };

    public interface AlElegir {
        /** @param avatar par {emoji, color} del catálogo */
        void elegido(String[] avatar);
    }

    private AvatarCatalogo() {}

    /** Abre la grilla de avatares y avisa cuál se eligió. */
    public static void mostrar(Context contexto, AlElegir alElegir) {
        View vista = LayoutInflater.from(contexto).inflate(R.layout.dialog_avatar_catalogo, null);
        RecyclerView rv = vista.findViewById(R.id.rv_avatares);

        AlertDialog dialogo = new AlertDialog.Builder(contexto)
                .setTitle(R.string.elegir_avatar)
                .setView(vista)
                .setNegativeButton(R.string.cancelar, null)
                .create();

        rv.setLayoutManager(new GridLayoutManager(contexto, 4));
        rv.setAdapter(new Adaptador(avatar -> {
            alElegir.elegido(avatar);
            dialogo.dismiss();
        }));
        dialogo.show();
    }

    private static class Adaptador extends RecyclerView.Adapter<Adaptador.VH> {
        private final AlElegir listener;

        Adaptador(AlElegir listener) { this.listener = listener; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_avatar_catalogo, parent, false));
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            String[] avatar = OPCIONES[pos];
            h.tvEmoji.setText(avatar[0]);
            AvatarUtils.pintarCirculo(h.tvEmoji, avatar[1]);
            h.itemView.setOnClickListener(v -> listener.elegido(avatar));
        }

        @Override public int getItemCount() { return OPCIONES.length; }

        static class VH extends RecyclerView.ViewHolder {
            final TextView tvEmoji;
            VH(View v) { super(v); tvEmoji = v.findViewById(R.id.tv_emoji); }
        }
    }
}
