package com.teapp.ui.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.teapp.R;
import com.teapp.util.AvatarUtils;
import com.teapp.util.AvatarEmoji;
import com.teapp.ui.common.AvatarCatalogo;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityProfileBinding;
import com.teapp.model.AuthResponse;
import com.teapp.ui.auth.ChangePasswordActivity;
import com.teapp.ui.auth.LoginActivity;
import com.teapp.util.PrefsManager;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQ_FOTO = 500;

    private ActivityProfileBinding binding;
    private ApiService api;
    private PrefsManager prefs;
    private String fechaApiFormat = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mi perfil");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = new PrefsManager(this);
        api   = ApiClient.getInstance(this).getApi();

        configurarFechaPicker();
        binding.btnElegirAvatar.setOnClickListener(v -> AvatarCatalogo.mostrar(this, elegido -> {
            String dataUri = AvatarEmoji.aDataUri(elegido[0], elegido[1]);
            guardarAvatar(dataUri);
            enviarAvatarAlServidor(dataUri);
        }));
        binding.btnCambiarFoto.setOnClickListener(v -> abrirGaleria());
        binding.btnGuardar.setOnClickListener(v -> guardarPerfil());
        binding.btnContrasena.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class)));
        binding.btnEliminarCuenta.setOnClickListener(v -> confirmarEliminarCuenta());

        cargarPerfil();
    }

    private void cargarPerfil() {
        api.getMe().enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    poblarVista(response.body());
                }
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Mostrar datos locales si falla la red
                binding.tvEmail.setText(prefs.getUserEmail());
                binding.etNombre.setText(prefs.getUserName());
                binding.tvRol.setText("THERAPIST".equals(prefs.getUserRole()) ? "Terapeuta" : "Padre / Tutor");
            }
        });
    }

    private void poblarVista(AuthResponse r) {
        binding.tvEmail.setText(r.email);
        binding.etNombre.setText(r.fullName);
        binding.tvRol.setText("THERAPIST".equals(r.role) ? "Terapeuta" : "Padre / Tutor");

        // Fecha de alta
        if (r.createdAt != null && !r.createdAt.isEmpty()) {
            try {
                String fecha = r.createdAt.substring(0, 10); // YYYY-MM-DD
                String[] p = fecha.split("-");
                binding.tvMiembroDesde.setText("Miembro desde " + p[2] + "/" + p[1] + "/" + p[0]);
            } catch (Exception ignored) {}
        }

        // Fecha de nacimiento
        if (r.dateOfBirth != null && !r.dateOfBirth.isEmpty()) {
            fechaApiFormat = r.dateOfBirth;
            String[] p = r.dateOfBirth.split("-");
            binding.etFecha.setText(p[2] + "/" + p[1] + "/" + p[0]);
        }

        // Avatar: emoji del catálogo, foto subida, o la inicial del nombre
        String inicial = r.fullName != null && !r.fullName.isEmpty()
                ? String.valueOf(r.fullName.charAt(0)).toUpperCase() : "?";
        AvatarUtils.mostrarAvatar(binding.tvInitials, binding.imgAvatar,
                r.avatarBase64, "#A8D8EA", inicial);

    }

    private void configurarFechaPicker() {
        binding.etFecha.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int y = c.get(Calendar.YEAR) - 30;
            int m = c.get(Calendar.MONTH);
            int d = c.get(Calendar.DAY_OF_MONTH);
            if (!fechaApiFormat.isEmpty()) {
                try {
                    String[] p = fechaApiFormat.split("-");
                    y = Integer.parseInt(p[0]);
                    m = Integer.parseInt(p[1]) - 1;
                    d = Integer.parseInt(p[2]);
                } catch (Exception ignored) {}
            }
            DatePickerDialog selector = new DatePickerDialog(this, (dp, yr, mo, da) -> {
                fechaApiFormat = String.format("%04d-%02d-%02d", yr, mo + 1, da);
                binding.etFecha.setText(String.format("%02d/%02d/%04d", da, mo + 1, yr));
            }, y, m, d);
            // Una fecha de nacimiento no puede ser futura.
            selector.getDatePicker().setMaxDate(System.currentTimeMillis());
            selector.show();
        });
    }

    private void guardarPerfil() {
        String nombre = binding.etNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            binding.tilNombre.setError("El nombre es obligatorio");
            return;
        }
        binding.tilNombre.setError(null);
        binding.progressGuardar.setVisibility(View.VISIBLE);
        binding.btnGuardar.setEnabled(false);

        try {
            JSONObject json = new JSONObject();
            json.put("fullName", nombre);
            if (!fechaApiFormat.isEmpty()) json.put("dateOfBirth", fechaApiFormat);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), json.toString());

            api.updateProfile(body).enqueue(new Callback<AuthResponse>() {
                @Override
                public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                    binding.progressGuardar.setVisibility(View.GONE);
                    binding.btnGuardar.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        prefs.saveUser(
                            response.body().id,
                            response.body().email,
                            response.body().fullName,
                            response.body().role,
                            response.body().inviteCode
                        );
                        Toast.makeText(ProfileActivity.this, "Perfil actualizado.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Error al guardar.", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<AuthResponse> call, Throwable t) {
                    binding.progressGuardar.setVisibility(View.GONE);
                    binding.btnGuardar.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            binding.progressGuardar.setVisibility(View.GONE);
            binding.btnGuardar.setEnabled(true);
        }
    }

    private void abrirGaleria() {
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setType("image/*");
        startActivityForResult(pick, REQ_FOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FOTO && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                Bitmap bmp = BitmapFactory.decodeStream(is);
                float r = 512f / Math.max(bmp.getWidth(), bmp.getHeight());
                if (r < 1) bmp = Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth()*r), (int)(bmp.getHeight()*r), true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                String b64 = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                guardarAvatar(b64);

                RequestBody body = RequestBody.create(MediaType.parse("text/plain"), b64);
                api.updateUserAvatar(body).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful())
                            Toast.makeText(ProfileActivity.this, "Foto actualizada.", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(ProfileActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Toast.makeText(this, "Error al cargar la imagen.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void confirmarEliminarCuenta() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar cuenta")
                .setMessage("¿Estás seguro? Se borrarán todos tus datos de forma permanente. Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> eliminarCuenta())
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void eliminarCuenta() {
        api.deleteMe().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    prefs.clear();
                    Toast.makeText(ProfileActivity.this, "Cuenta eliminada.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(ProfileActivity.this, "Error al eliminar la cuenta.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    /** Dibuja el avatar en pantalla y lo recuerda para el resto de la app. */
    private void guardarAvatar(String avatarBase64) {
        String nombre = binding.etNombre.getText().toString().trim();
        String inicial = !nombre.isEmpty() ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?";
        AvatarUtils.mostrarAvatar(binding.tvInitials, binding.imgAvatar,
                avatarBase64, "#A8D8EA", inicial);
        new PrefsManager(this).saveUserAvatar(avatarBase64);
    }

    private void enviarAvatarAlServidor(String avatarBase64) {
        RequestBody body = RequestBody.create(MediaType.parse("text/plain"), avatarBase64);
        api.updateUserAvatar(body).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful())
                    Toast.makeText(ProfileActivity.this, "Avatar actualizado.", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
