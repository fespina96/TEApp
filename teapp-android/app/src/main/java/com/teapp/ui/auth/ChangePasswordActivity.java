package com.teapp.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityChangePasswordBinding;
import com.teapp.model.ChangePasswordRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;
    private ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.cambiar_contrasena);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        api = ApiClient.getInstance(this).getApi();
        binding.btnGuardar.setOnClickListener(v -> onGuardar());
    }

    private void onGuardar() {
        String actual    = binding.etActual.getText().toString().trim();
        String nueva     = binding.etNueva.getText().toString().trim();
        String confirmar = binding.etConfirmar.getText().toString().trim();

        if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
            Toast.makeText(this, R.string.campos_requeridos, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!nueva.equals(confirmar)) {
            binding.tilConfirmar.setError("Las contraseñas no coinciden");
            return;
        }
        if (nueva.length() < 8) {
            binding.tilNueva.setError("Mínimo 8 caracteres");
            return;
        }
        binding.tilConfirmar.setError(null);
        binding.tilNueva.setError(null);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnGuardar.setEnabled(false);

        api.changePassword(new ChangePasswordRequest(actual, nueva))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnGuardar.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(ChangePasswordActivity.this,
                                    "Contraseña actualizada correctamente.", Toast.LENGTH_SHORT).show();
                            finish();
                        } else if (response.code() == 400) {
                            binding.tilActual.setError("Contraseña actual incorrecta");
                        } else {
                            Toast.makeText(ChangePasswordActivity.this,
                                    "Error al cambiar la contraseña.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnGuardar.setEnabled(true);
                        Toast.makeText(ChangePasswordActivity.this,
                                R.string.error_red, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
