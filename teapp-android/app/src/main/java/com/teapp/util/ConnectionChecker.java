package com.teapp.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.teapp.R;
import com.teapp.api.ApiClient;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConnectionChecker {

    private final AppCompatActivity activity;
    private View overlay;
    private Button btnReintentar;
    private ProgressBar progress;

    public ConnectionChecker(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void check() {
        ApiClient.getInstance(activity).getApi().health().enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    hideOverlay();
                } else {
                    showOverlay();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                showOverlay();
            }
        });
    }

    private void showOverlay() {
        if (overlay == null) {
            ViewGroup root = (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);
            overlay = LayoutInflater.from(activity).inflate(R.layout.overlay_no_connection, root, false);
            root.addView(overlay);
            btnReintentar = overlay.findViewById(R.id.btn_reintentar);
            progress = overlay.findViewById(R.id.progress_reintentar);
            btnReintentar.setOnClickListener(v -> retry());
        }
        overlay.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
        btnReintentar.setEnabled(true);
    }

    private void hideOverlay() {
        if (overlay != null) {
            overlay.setVisibility(View.GONE);
        }
    }

    private void retry() {
        btnReintentar.setEnabled(false);
        progress.setVisibility(View.VISIBLE);

        ApiClient.getInstance(activity).getApi().health().enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                progress.setVisibility(View.GONE);
                btnReintentar.setEnabled(true);
                if (response.isSuccessful()) {
                    hideOverlay();
                    activity.recreate();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                progress.setVisibility(View.GONE);
                btnReintentar.setEnabled(true);
            }
        });
    }
}
