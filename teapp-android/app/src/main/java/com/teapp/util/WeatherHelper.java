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

public class WeatherHelper {

    public interface WeatherCallback {
        void onResult(String emoji, String label, int tempC);
        void onError();
    }

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
                    double temp = current.getDouble("temperature_2m");
                    int code = current.getInt("weathercode");
                    String[] info = codeToInfo(code);
                    mainHandler.post(() -> cb.onResult(info[0], info[1], (int) Math.round(temp)));
                } catch (Exception e) {
                    mainHandler.post(cb::onError);
                }
            }
        });
    }

    private static String[] codeToInfo(int code) {
        if (code == 0)     return new String[]{"☀️", "Despejado"};
        if (code <= 3)     return new String[]{"🌤️", "Algo de nubes"};
        if (code <= 48)    return new String[]{"🌫️", "Niebla"};
        if (code <= 55)    return new String[]{"🌦️", "Llovizna"};
        if (code <= 65)    return new String[]{"🌧️", "Lluvia"};
        if (code <= 77)    return new String[]{"❄️", "Nieve"};
        if (code <= 82)    return new String[]{"🌦️", "Chubascos"};
        return new String[]{"⛈️", "Tormenta"};
    }
}
