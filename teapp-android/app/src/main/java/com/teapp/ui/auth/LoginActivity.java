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
            DatePickerDialog selector = new DatePickerDialog(this, (dp, y, m, d) -> {
                fechaNacApiFormat = String.format("%04d-%02d-%02d", y, m + 1, d);
                binding.etFechaNac.setText(String.format("%02d/%02d/%04d", d, m + 1, y));
                // La fecha se completa desde el diálogo, no escribiendo, así que hay
                // que reevaluar a mano: si no, el botón se quedaba deshabilitado.
                actualizarEstadoDelBoton();
            }, c.get(Calendar.YEAR) - 25, c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            // Una fecha de nacimiento no puede ser futura.
            selector.getDatePicker().setMaxDate(System.currentTimeMillis());
            selector.show();
        });

        android.text.TextWatcher observador = observadorDeCampos();
        binding.etNombre.addTextChangedListener(observador);
        binding.etEmail.addTextChangedListener(observador);
        binding.etPassword.addTextChangedListener(observador);
        binding.etConfirmPassword.addTextChangedListener(observador);
        actualizarEstadoDelBoton();

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
        binding.tilConfirmPassword.setVisibility(modoRegistro ? View.VISIBLE : View.GONE);
        binding.tilNombre.setVisibility(modoRegistro ? View.VISIBLE : View.GONE);
        binding.tilFechaNac.setVisibility(modoRegistro ? View.VISIBLE : View.GONE);
        binding.radioGroup.setVisibility(modoRegistro ? View.VISIBLE : View.GONE);
        binding.btnOlvide.setVisibility(modoRegistro ? View.GONE : View.VISIBLE);
        binding.switchRecordar.setVisibility(modoRegistro ? View.GONE : View.VISIBLE);
        binding.btnSubmit.setText(getString(modoRegistro ? R.string.registrarse : R.string.iniciar_sesion));
        limpiarErrores();
        actualizarEstadoDelBoton();
    }

    private void onSubmit() {
        String email = binding.etEmail.getText().toString().trim();
        String pass  = binding.etPassword.getText().toString().trim();

        if (modoRegistro) {
            if (!validarRegistro(email, pass)) return;

            String nombre = binding.etNombre.getText().toString().trim();
            String rol    = binding.rbPadre.isChecked() ? "PARENT" : "THERAPIST";
            setLoading(true);
            api.register(new RegisterRequest(email, pass, nombre, fechaNacApiFormat, rol))
                    .enqueue(registroCallback(email));
        } else {
            setLoading(true);
            api.login(new LoginRequest(email, pass)).enqueue(authCallback);
        }
    }

    /**
     * Aplica las mismas reglas que el formulario web: email con formato válido,
     * contraseña de 8 caracteres con al menos una mayúscula y un número, y
     * confirmación coincidente. Evita depender del rechazo del backend.
     */
    private boolean validarRegistro(String email, String pass) {
        limpiarErrores();

        String nombre = binding.etNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            binding.tilNombre.setError(getString(R.string.error_nombre_obligatorio));
            return false;
        }
        if (email.isEmpty()) {
            binding.tilEmail.setError(getString(R.string.error_email_obligatorio));
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_email_invalido));
            return false;
        }
        if (fechaNacApiFormat.isEmpty()) {
            binding.tilFechaNacInner.setError(getString(R.string.error_fecha_obligatoria));
            return false;
        }
        if (pass.isEmpty()) {
            binding.tilPassword.setError(getString(R.string.error_password_obligatoria));
            return false;
        }
        if (pass.length() < 8) {
            binding.tilPassword.setError(getString(R.string.error_password_corta));
            return false;
        }
        if (!pass.matches("^(?=.*[A-Z])(?=.*\\d).+$")) {
            binding.tilPassword.setError(getString(R.string.error_password_patron));
            return false;
        }

        String confirmacion = binding.etConfirmPassword.getText().toString();
        if (confirmacion.isEmpty()) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_confirmar));
            return false;
        }
        if (!pass.equals(confirmacion)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_no_coincide));
            return false;
        }
        return true;
    }

    private void limpiarErrores() {
        binding.tilNombre.setError(null);
        binding.tilEmail.setError(null);
        binding.tilFechaNacInner.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
    }

    /**
     * El botón queda deshabilitado mientras falte algo, igual que en la web, donde
     * el submit lleva [disabled]="registerForm.invalid". La comprobación es silenciosa:
     * los mensajes por campo aparecen recién al intentar enviar, para no ir marcando
     * errores mientras la persona todavía está escribiendo.
     */
    private void actualizarEstadoDelBoton() {
        binding.btnSubmit.setEnabled(modoRegistro ? registroCompleto() : loginCompleto());
    }

    private boolean loginCompleto() {
        return !binding.etEmail.getText().toString().trim().isEmpty()
                && !binding.etPassword.getText().toString().isEmpty();
    }

    private boolean registroCompleto() {
        String pass = binding.etPassword.getText().toString();
        return !binding.etNombre.getText().toString().trim().isEmpty()
                && !binding.etEmail.getText().toString().trim().isEmpty()
                && !fechaNacApiFormat.isEmpty()
                && !pass.isEmpty()
                && !binding.etConfirmPassword.getText().toString().isEmpty();
    }

    /** Un observador que sólo reevalúa si el botón puede habilitarse. */
    private android.text.TextWatcher observadorDeCampos() {
        return new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                actualizarEstadoDelBoton();
            }
        };
    }

    /**
     * Extrae el campo "message" del cuerpo de error del backend, que explica el
     * motivo concreto del rechazo. Si no se puede leer, usa el texto por defecto.
     */
    private String mensajeDeError(Response<?> response, String porDefecto) {
        if (response.errorBody() == null) return porDefecto;
        try {
            String cuerpo = response.errorBody().string();
            String mensaje = new org.json.JSONObject(cuerpo).optString("message", "");
            return mensaje.isEmpty() ? porDefecto : mensaje;
        } catch (Exception e) {
            return porDefecto;
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
                prefs.saveUserAvatar(body.avatarBase64);
                prefs.saveRememberDevice(binding.switchRecordar.isChecked());
                Class<?> dest = "THERAPIST".equals(body.role)
                        ? TherapistDashboardActivity.class
                        : DashboardActivity.class;
                startActivity(new Intent(LoginActivity.this, dest));
                finish();
            } else {
                Toast.makeText(LoginActivity.this,
                        mensajeDeError(response, "Credenciales incorrectas."),
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onFailure(Call<AuthResponse> call, Throwable t) {
            setLoading(false);
            Toast.makeText(LoginActivity.this, R.string.error_red, Toast.LENGTH_LONG).show();
        }
    };

    /**
     * Crear la cuenta no inicia sesión: se vuelve al formulario de ingreso con el
     * email ya cargado, para que la persona entre con las credenciales que eligió.
     */
    private Callback<AuthResponse> registroCallback(String email) {
        return new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                setLoading(false);
                if (!response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this,
                            mensajeDeError(response, "Error al registrarse. Verificá los datos."),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                setTab(false);
                binding.etEmail.setText(email);
                binding.etPassword.setText("");
                binding.etConfirmPassword.setText("");
                binding.etNombre.setText("");
                Toast.makeText(LoginActivity.this, R.string.cuenta_creada, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, R.string.error_red, Toast.LENGTH_LONG).show();
            }
        };
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSubmit.setEnabled(!loading);
    }
}
