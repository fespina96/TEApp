package com.teapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.teapp.api.ApiClient;
import com.teapp.databinding.ActivityForgotPasswordBinding;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        binding.btnEnviar.setOnClickListener(v -> enviar());

        binding.btnBackLogin.setOnClickListener(v -> volverAlInicio());
        binding.btnVolverInicio.setOnClickListener(v -> volverAlInicio());
    }

    private void volverAlInicio() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void enviar() {
        String email = binding.etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            binding.tilEmail.setError("Ingresá tu correo.");
            return;
        }
        binding.tilEmail.setError(null);
        setLoading(true);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        ApiClient.getInstance(this).getApi()
                .forgotPassword(body)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        setLoading(false);
                        mostrarExito();
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        setLoading(false);
                        mostrarExito();
                    }
                });
    }

    private void mostrarExito() {
        binding.layoutForm.setVisibility(View.GONE);
        binding.layoutExito.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnEnviar.setEnabled(!loading);
    }
}
