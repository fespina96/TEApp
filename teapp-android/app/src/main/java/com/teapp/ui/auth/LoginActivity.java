package com.teapp.ui.auth;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityLoginBinding;
import com.teapp.model.AuthResponse;
import com.teapp.model.LoginRequest;
import com.teapp.model.RegisterRequest;
import com.teapp.ui.dashboard.DashboardActivity;
import com.teapp.ui.therapist.TherapistDashboardActivity;
import com.teapp.util.PrefsManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private PrefsManager prefs;
    private ApiService api;
    private boolean modoRegistro = false;
    private String fechaNacApiFormat = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        prefs = new PrefsManager(this);
        api   = ApiClient.getInstance(this).getApi();

        // Pill tabs
        binding.tabLogin.setOnClickListener(v -> setTab(false));
        binding.tabRegister.setOnClickListener(v -> setTab(true));

        binding.btnOlvide.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        binding.etFechaNac.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (dp, y, m, d) -> {
                fechaNacApiFormat = String.format("%04d-%02d-%02d", y, m + 1, d);
                binding.etFechaNac.setText(String.format("%02d/%02d/%04d", d, m + 1, y));
            }, c.get(Calendar.YEAR) - 25, c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        binding.btnSubmit.setOnClickListener(v -> onSubmit());
    }

    private void setTab(boolean registro) {
        modoRegistro = registro;
        // Estilo pill tabs
        if (registro) {
            binding.tabLogin.setBackground(null);
            binding.tabLogin.setTextColor(getResources().getColor(R.color.text_secondary));
            binding.tabLogin.setTypeface(null, Typeface.NORMAL);
            binding.tabRegister.setBackgroundResource(R.drawable.bg_pill_tab_active);
            binding.tabRegister.setTextColor(getResources().getColor(R.color.on_primary));
            binding.tabRegister.setTypeface(null, Typeface.BOLD);
        } else {
            binding.tabLogin.setBackgroundResource(R.drawable.bg_pill_tab_active);
            binding.tabLogin.setTextColor(getResources().getColor(R.color.on_primary));
            binding.tabLogin.setTypeface(null, Typeface.BOLD);
            binding.tabRegister.setBackground(null);
            binding.tabRegister.setTextColor(getResources().getColor(R.color.text_secondary));
            binding.tabRegister.setTypeface(null, Typeface.NORMAL);
        }
        actualizarFormulario();
    }

    private void actualizarFormulario() {
        binding.tilNombre.setVisibility(modoRegistro ? View.VISIBLE : View.GONE);
        binding.tilFechaNac.setVisibility(modoRegistro ? View.VISIBLE : View.GONE);
        binding.radioGroup.setVisibility(modoRegistro ? View.VISIBLE : View.GONE);
        binding.btnOlvide.setVisibility(modoRegistro ? View.GONE : View.VISIBLE);
        binding.switchRecordar.setVisibility(modoRegistro ? View.GONE : View.VISIBLE);
        binding.btnSubmit.setText(getString(modoRegistro ? R.string.registrarse : R.string.iniciar_sesion));
    }

    private void onSubmit() {
        String email = binding.etEmail.getText().toString().trim();
        String pass  = binding.etPassword.getText().toString().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, R.string.campos_requeridos, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (modoRegistro) {
            String nombre = binding.etNombre.getText().toString().trim();
            String rol    = binding.rbPadre.isChecked() ? "PARENT" : "THERAPIST";
            if (nombre.isEmpty() || fechaNacApiFormat.isEmpty()) {
                Toast.makeText(this, R.string.campos_requeridos, Toast.LENGTH_SHORT).show();
                setLoading(false);
                return;
            }
            api.register(new RegisterRequest(email, pass, nombre, fechaNacApiFormat, rol))
                    .enqueue(authCallback);
        } else {
            api.login(new LoginRequest(email, pass)).enqueue(authCallback);
        }
    }

    private final Callback<AuthResponse> authCallback = new Callback<AuthResponse>() {
        @Override
        public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
            setLoading(false);
            if (response.isSuccessful() && response.body() != null) {
                AuthResponse body = response.body();
                prefs.saveToken(body.token);
                prefs.saveUser(body.id, body.email, body.fullName, body.role, body.inviteCode);
                prefs.saveRememberDevice(!modoRegistro && binding.switchRecordar.isChecked());
                Class<?> dest = "THERAPIST".equals(body.role)
                        ? TherapistDashboardActivity.class
                        : DashboardActivity.class;
                startActivity(new Intent(LoginActivity.this, dest));
                finish();
            } else {
                Toast.makeText(LoginActivity.this,
                        modoRegistro ? "Error al registrarse. Verificá los datos."
                                     : "Credenciales incorrectas.",
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onFailure(Call<AuthResponse> call, Throwable t) {
            setLoading(false);
            Toast.makeText(LoginActivity.this, R.string.error_red, Toast.LENGTH_LONG).show();
        }
    };

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSubmit.setEnabled(!loading);
    }
}
