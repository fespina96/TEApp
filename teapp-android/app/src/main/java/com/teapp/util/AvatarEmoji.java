package com.teapp.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Los avatares del catálogo se guardan como un SVG en data URI: un círculo de
 * color con un emoji encima. Glide no sabe decodificar SVG.
 *
 * En vez de sumar una librería de SVG, acá se leen las dos piezas que interesan
 * —el emoji y el color— y la vista los dibuja de forma nativa. El formato lo
 * genera AvatarPickerDialogComponent en la versión web; si cambia allá, hay que
 * acompañarlo acá.
 */
public final class AvatarEmoji {

    private static final String PREFIJO = "data:image/svg+xml;base64,";

    private static final Pattern COLOR = Pattern.compile("<circle[^>]*fill=\"([^\"]+)\"");
    private static final Pattern TEXTO = Pattern.compile("<text[^>]*>([^<]+)</text>");

    /** El emoji y el color de fondo de un avatar del catálogo. */
    public static final class Contenido {
        public final String emoji;
        public final String colorHex;

        Contenido(String emoji, String colorHex) {
            this.emoji = emoji;
            this.colorHex = colorHex;
        }
    }

    private AvatarEmoji() {}

    /** @return true si el avatar es uno del catálogo y no una foto subida */
    public static boolean esDelCatalogo(String avatarBase64) {
        return avatarBase64 != null && avatarBase64.startsWith(PREFIJO);
    }

    /**
     * @return el emoji y el color, o null si no es un avatar del catálogo o el
     *         contenido no tiene la forma esperada
     */
    public static Contenido leer(String avatarBase64) {
        if (!esDelCatalogo(avatarBase64)) return null;
        try {
            byte[] crudo = Base64.decode(avatarBase64.substring(PREFIJO.length()), Base64.DEFAULT);
            String svg = new String(crudo, StandardCharsets.UTF_8);

            Matcher mColor = COLOR.matcher(svg);
            Matcher mTexto = TEXTO.matcher(svg);
            if (!mColor.find() || !mTexto.find()) return null;

            String emoji = mTexto.group(1).trim();
            return emoji.isEmpty() ? null : new Contenido(emoji, mColor.group(1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Arma el mismo data URI que la web, para que el avatar elegido acá se vea allá. */
    public static String aDataUri(String emoji, String colorHex) {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\">"
                + "<circle cx=\"50\" cy=\"50\" r=\"50\" fill=\"" + colorHex + "\"/>"
                + "<text x=\"50\" y=\"68\" font-size=\"52\" text-anchor=\"middle\">" + emoji + "</text>"
                + "</svg>";
        return PREFIJO + Base64.encodeToString(svg.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }
}
