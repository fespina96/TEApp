package com.teapp.ui.child;

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
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityChildFormBinding;
import com.teapp.model.Child;
import com.teapp.model.ChildRequest;
import com.teapp.util.Constants;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChildFormActivity extends AppCompatActivity {

    private static final int REQ_PICK_IMAGE = 200;

    private ActivityChildFormBinding binding;
    private ApiService api;
    private Child participanteExistente;
    private String colorSeleccionado = "#A8D8EA";
    private String fechaApiFormat    = "";
    private String avatarBase64Nueva = null;

    private static final String[] COLORES = {
            "#A8D8EA", "#B8E0C8", "#FAF0BE", "#C9B8E8", "#F9D8C0", "#A8E0DA"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChildFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.getInstance(this).getApi();
        participanteExistente = (Child) getIntent().getSerializableExtra(Constants.EXTRA_CHILD);

        boolean modoEdicion = participanteExistente != null;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(modoEdicion
                    ? R.string.editar_participante : R.string.nuevo_participante);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (modoEdicion) {
            binding.etNombre.setText(participanteExistente.name);
            if (participanteExistente.dateOfBirth != null && !participanteExistente.dateOfBirth.isEmpty()) {
                fechaApiFormat = participanteExistente.dateOfBirth;
                binding.etFecha.setText(apiADisplay(fechaApiFormat));
            }
            binding.etNotas.setText(participanteExistente.notes);
            colorSeleccionado = participanteExistente.avatarColor != null
                    ? participanteExistente.avatarColor : colorSeleccionado;
            // Cargar avatar existente
            if (participanteExistente.avatarBase64 != null && !participanteExistente.avatarBase64.isEmpty()) {
                Glide.with(this).load(participanteExistente.avatarBase64).circleCrop()
                        .into(binding.imgAvatar);
            } else {
                String inicial = participanteExistente.name != null && !participanteExistente.name.isEmpty()
                        ? String.valueOf(participanteExistente.name.charAt(0)).toUpperCase() : "?";
                mostrarInicialEnAvatar(inicial, colorSeleccionado);
            }
        }

        configurarSelectorFecha();
        configurarSelectorColor();
        binding.btnCambiarFoto.setOnClickListener(v -> abrirGaleria());
        binding.btnGuardar.setOnClickListener(v -> onSubmit());
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQ_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap original = BitmapFactory.decodeStream(is);
                Bitmap scaled   = escalarBitmap(original, 512);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                byte[] bytes = baos.toByteArray();
                avatarBase64Nueva = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);
                Glide.with(this).load(avatarBase64Nueva).circleCrop().into(binding.imgAvatar);
            } catch (Exception e) {
                Toast.makeText(this, "Error al cargar la imagen.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap escalarBitmap(Bitmap original, int maxSide) {
        int w = original.getWidth();
        int h = original.getHeight();
        if (w <= maxSide && h <= maxSide) return original;
        float ratio = (float) maxSide / Math.max(w, h);
        return Bitmap.createScaledBitmap(original, (int)(w * ratio), (int)(h * ratio), true);
    }

    private void mostrarInicialEnAvatar(String inicial, String color) {
        try {
            int c = android.graphics.Color.parseColor(color);
            binding.imgAvatar.setBackgroundColor(c);
        } catch (Exception ignored) {}
    }

    private void configurarSelectorFecha() {
        binding.etFecha.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            int anoInicial = c.get(Calendar.YEAR) - 5;
            int mesInicial = c.get(Calendar.MONTH);
            int diaInicial = c.get(Calendar.DAY_OF_MONTH);
            if (!fechaApiFormat.isEmpty()) {
                try {
                    String[] partes = fechaApiFormat.split("-");
                    anoInicial = Integer.parseInt(partes[0]);
                    mesInicial = Integer.parseInt(partes[1]) - 1;
                    diaInicial = Integer.parseInt(partes[2]);
                } catch (Exception ignored) {}
            }
            new DatePickerDialog(this, (dp, y, m, d) -> {
                fechaApiFormat = String.format("%04d-%02d-%02d", y, m + 1, d);
                binding.etFecha.setText(String.format("%02d/%02d/%04d", d, m + 1, y));
            }, anoInicial, mesInicial, diaInicial).show();
        });
    }

    private String apiADisplay(String apiDate) {
        try {
            String[] p = apiDate.split("-");
            return String.format("%s/%s/%s", p[2], p[1], p[0]);
        } catch (Exception e) {
            return apiDate;
        }
    }

    private void configurarSelectorColor() {
        View[] botones = {
                binding.color1, binding.color2, binding.color3,
                binding.color4, binding.color5, binding.color6
        };
        for (int i = 0; i < botones.length; i++) {
            final int idx = i;
            try { botones[i].setBackgroundColor(android.graphics.Color.parseColor(COLORES[i])); }
            catch (Exception ignored) {}
            botones[i].setOnClickListener(v -> {
                colorSeleccionado = COLORES[idx];
                for (View b : botones) b.setScaleX(1f);
                botones[idx].setScaleX(1.3f);
                if (avatarBase64Nueva == null) mostrarInicialEnAvatar("?", colorSeleccionado);
            });
        }
    }

    private void onSubmit() {
        String nombre = binding.etNombre.getText().toString().trim();
        String notas  = binding.etNotas.getText().toString().trim();

        if (nombre.isEmpty() || fechaApiFormat.isEmpty()) {
            Toast.makeText(this, R.string.campos_requeridos, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        ChildRequest req = new ChildRequest(nombre, fechaApiFormat, colorSeleccionado, notas);

        Callback<Child> callback = new Callback<Child>() {
            @Override
            public void onResponse(Call<Child> call, Response<Child> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (avatarBase64Nueva != null) {
                        subirAvatar(response.body().id);
                    } else {
                        setLoading(false);
                        Toast.makeText(ChildFormActivity.this, "Guardado correctamente.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    setLoading(false);
                    Toast.makeText(ChildFormActivity.this, "Error al guardar.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Child> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ChildFormActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        };

        if (participanteExistente != null) {
            api.updateChild(participanteExistente.id, req).enqueue(callback);
        } else {
            api.createChild(req).enqueue(callback);
        }
    }

    private void subirAvatar(String childId) {
        String json = "{\"avatarBase64\":\"" + avatarBase64Nueva + "\"}";
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"), json);

        api.updateChildAvatar(childId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                setLoading(false);
                Toast.makeText(ChildFormActivity.this, "Guardado correctamente.", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ChildFormActivity.this, "Guardado, pero no se pudo subir la foto.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnGuardar.setEnabled(!loading);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
