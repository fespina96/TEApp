package com.teapp.util;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * Pinta los avatares circulares de participantes y usuarios.
 *
 * Existe porque el error era fácil de repetir: llamar a setBackgroundColor() sobre
 * una vista cuyo fondo es @drawable/circle_avatar reemplaza el óvalo por un color
 * plano, y el avatar deja de ser redondo. Hay que teñir el drawable, no pisarlo.
 */
public final class AvatarUtils {

    private static final int COLOR_POR_DEFECTO = Color.parseColor("#A8D8EA");

    private AvatarUtils() {}

    /**
     * @param vista    la que tiene @drawable/circle_avatar de fondo
     * @param colorHex color en formato #RRGGBB; si es nulo o inválido se usa el celeste de marca
     */
    public static void pintarCirculo(View vista, String colorHex) {
        vista.setBackgroundTintList(ColorStateList.valueOf(aColor(colorHex)));
    }

    /**
     * Dibuja el avatar en sus tres formas posibles, en este orden:
     * un emoji del catálogo, una foto subida, o las iniciales sobre el color.
     *
     * @param tvTexto   TextView con @drawable/circle_avatar de fondo
     * @param ivFoto    ImageView superpuesto, para las fotos
     * @param iniciales lo que se muestra cuando no hay ni emoji ni foto
     */
    public static void mostrarAvatar(TextView tvTexto, ImageView ivFoto,
                                     String avatarBase64, String colorHex, String iniciales) {
        AvatarEmoji.Contenido delCatalogo = AvatarEmoji.leer(avatarBase64);

        if (delCatalogo != null) {
            // Se dibuja nativamente: Glide no sabe decodificar el SVG del catálogo.
            tvTexto.setText(delCatalogo.emoji);
            pintarCirculo(tvTexto, delCatalogo.colorHex);
            tvTexto.setVisibility(View.VISIBLE);
            ivFoto.setVisibility(View.GONE);
            return;
        }

        if (avatarBase64 != null && !avatarBase64.isEmpty()) {
            Glide.with(ivFoto.getContext()).load(avatarBase64).circleCrop().into(ivFoto);
            ivFoto.setVisibility(View.VISIBLE);
            tvTexto.setVisibility(View.GONE);
            return;
        }

        tvTexto.setText(iniciales);
        pintarCirculo(tvTexto, colorHex);
        tvTexto.setVisibility(View.VISIBLE);
        ivFoto.setVisibility(View.GONE);
    }

    private static int aColor(String colorHex) {
        if (colorHex == null || colorHex.isEmpty()) return COLOR_POR_DEFECTO;
        try {
            return Color.parseColor(colorHex);
        } catch (IllegalArgumentException e) {
            return COLOR_POR_DEFECTO;
        }
    }
}
