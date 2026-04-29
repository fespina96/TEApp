package com.teapp.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.teapp.R;
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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.recuperar_contrasena);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.btnEnviar.setOnClickListener(v -> enviar());
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
                        binding.tvExito.setVisibility(View.VISIBLE);
                        binding.btnEnviar.setEnabled(false);
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Si el correo existe, recibirás el enlace.", Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(ForgotPasswordActivity.this,
                                R.string.error_red, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnEnviar.setEnabled(!loading);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
