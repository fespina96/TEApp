package com.teapp.util;

import android.view.View;
import android.widget.TextView;

/**
 * Dibuja el icono de una actividad cuando no tiene pictograma propio.
 *
 * El campo iconName guarda un nombre de Material Icons ("bathtub", "restaurant"),
 * que es lo que la versión web pinta con &lt;mat-icon&gt;. Acá se consigue lo mismo
 * escribiendo ese nombre en un TextView con la fuente Material Icons: la ligadura
 * de la fuente lo convierte en el dibujo. Así el mismo dato de la base se ve igual
 * en las dos plataformas.
 */
public final class IconoActividad {

    /** El que usa la web cuando la actividad no trae ninguno. */
    private static final String POR_DEFECTO = "star";

    private IconoActividad() {}

    /**
     * @param tvIcono  TextView con style="@style/IconoMaterial" y fondo circle_avatar
     * @param iconName nombre del icono de Material; si viene vacío se usa una estrella
     * @param colorHex color de la actividad, para el círculo de atrás
     */
    public static void pintar(TextView tvIcono, String iconName, String colorHex) {
        tvIcono.setText(iconName == null || iconName.isEmpty() ? POR_DEFECTO : iconName);
        AvatarUtils.pintarCirculo(tvIcono, colorHex);
        tvIcono.setVisibility(View.VISIBLE);
    }

    /** Cuando hay pictograma, el icono estorba. */
    public static void ocultar(TextView tvIcono) {
        tvIcono.setVisibility(View.GONE);
    }
}
