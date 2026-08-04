package com.teapp.util;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Consulta Open-Meteo y traduce la medición a lo que ve el participante: una
 * frase corta y un pictograma de ARASAAC. No se muestran grados, porque la
 * temperatura en cifras no le dice nada a quien todavía no lee números.
 *
 * El mapeo es el mismo que usa la versión web en WeatherService.
 */
public class WeatherHelper {

    public interface WeatherCallback {
        /**
         * @param frase         texto para el participante, por ejemplo "Afuera hace frío"
         * @param pictogramaUrl PNG de ARASAAC que acompaña a la frase
         */
        void onResult(String frase, String pictogramaUrl);
        void onError();
    }

    /** Pictogramas de ARASAAC verificados contra su CDN. */
    private static final int PICTO_SOL      = 2798;
    private static final int PICTO_CALOR    = 35561;
    private static final int PICTO_FRIO     = 4652;
    private static final int PICTO_LLUVIA   = 7148;
    private static final int PICTO_NUBLADO  = 2882;
    private static final int PICTO_NIEVE    = 7172;
    private static final int PICTO_TORMENTA = 34892;
    private static final int PICTO_NIEBLA   = 35049;

    /** Debajo de esto se siente frío; por encima del otro, calor. */
    private static final int FRIO_HASTA  = 12;
    private static final int CALOR_DESDE = 27;

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void fetch(double lat, double lon, WeatherCallback cb) {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat
                + "&longitude=" + lon
                + "&current=temperature_2m,weathercode&timezone=auto";

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(cb::onError);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JSONObject json = new JSONObject(body);
                    JSONObject current = json.getJSONObject("current");
                    int temp = (int) Math.round(current.getDouble("temperature_2m"));
                    int code = current.getInt("weathercode");

                    String frase = describirFrase(code, temp);
                    String urlPicto = urlPictograma(describirPictograma(code, temp));
                    mainHandler.post(() -> cb.onResult(frase, urlPicto));
                } catch (Exception e) {
                    mainHandler.post(cb::onError);
                }
            }
        });
    }

    /**
     * Lo que llueve o nieva manda sobre la temperatura: si está lloviendo, eso es
     * lo que hay que abrigar o llevar, no si hace uno o dos grados de más.
     */
    private static String describirFrase(int code, int temp) {
        if (code >= 95)               return "Afuera hay tormenta";
        if (code >= 71 && code <= 77) return "Afuera está nevando";
        if (code >= 51 && code <= 82) return "Afuera está lloviendo";
        if (code >= 45 && code <= 48) return "Afuera hay niebla";
        if (temp <= FRIO_HASTA)       return "Afuera hace frío";
        if (temp >= CALOR_DESDE)      return "Afuera hace calor";
        return code == 0 ? "Afuera está soleado" : "Afuera está nublado";
    }

    private static int describirPictograma(int code, int temp) {
        if (code >= 95)               return PICTO_TORMENTA;
        if (code >= 71 && code <= 77) return PICTO_NIEVE;
        if (code >= 51 && code <= 82) return PICTO_LLUVIA;
        if (code >= 45 && code <= 48) return PICTO_NIEBLA;
        if (temp <= FRIO_HASTA)       return PICTO_FRIO;
        if (temp >= CALOR_DESDE)      return PICTO_CALOR;
        return code == 0 ? PICTO_SOL : PICTO_NUBLADO;
    }

    private static String urlPictograma(int id) {
        return "https://static.arasaac.org/pictograms/" + id + "/" + id + "_500.png";
    }
}
