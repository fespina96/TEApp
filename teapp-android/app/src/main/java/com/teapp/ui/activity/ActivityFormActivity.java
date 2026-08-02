package com.teapp.ui.activity;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityActivityFormBinding;
import com.teapp.model.ActivityItem;
import com.teapp.model.ActivityRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;

public class ActivityFormActivity extends AppCompatActivity {

    public static final String EXTRA_ACTIVITY = "activity";

    // Deben coincidir con el enum ActivityCategory del backend.
    private static final String[] CATEGORIAS_KEYS = {
            "HYGIENE", "MEAL", "EDUCATION", "PLAY", "THERAPY",
            "REST", "OUTDOOR", "CUSTOM", "SPECIAL_EVENT"
    };
    private static final String[] CATEGORIAS_LABELS = {
            "Higiene", "Comidas", "Educación", "Juego", "Terapia",
            "Descanso", "Aire libre", "Personalizada", "Evento especial"
    };
    // Mismos colores por categoría que usa la web (CATEGORY_COLORS).
    private static final String[] CATEGORIAS_COLORS = {
            "#A8D8EA", "#FAF0BE", "#B8E0C8", "#C9B8E8", "#D4E8C8",
            "#E0D8F0", "#C8E8D0", "#F9D8C0", "#FFE082"
    };

    private ActivityActivityFormBinding binding;
    private ApiService api;
    private ActivityItem actividadExistente;
    private String pictogramUrl   = null;
    private String colorActividad = "#F9D8C0";

    private final List<ArasaacItem> resultadosArasaac = new ArrayList<>();
    private ArasaacAdapter arasaacAdapter;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityActivityFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        actividadExistente = (ActivityItem) getIntent().getSerializableExtra(EXTRA_ACTIVITY);
        boolean modoEdicion = actividadExistente != null;

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(modoEdicion
                    ? R.string.editar_actividad : R.string.nueva_actividad);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        api = ApiClient.getInstance(this).getApi();

        configurarSpinner();
        configurarArasaac();

        // Auto-color al cambiar categoría (igual que Angular)
        binding.spinnerCategoria.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                if (actividadExistente == null) colorActividad = CATEGORIAS_COLORS[pos];
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        if (modoEdicion) prepoblarFormulario();
        else colorActividad = CATEGORIAS_COLORS[0];

        binding.btnGuardar.setOnClickListener(v -> onGuardar());
        binding.btnQuitarPictograma.setOnClickListener(v -> {
            pictogramUrl = null;
            binding.layoutPictogramaSeleccionado.setVisibility(View.GONE);
        });
    }

    private void configurarSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CATEGORIAS_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategoria.setAdapter(adapter);
    }

    private void configurarArasaac() {
        arasaacAdapter = new ArasaacAdapter(resultadosArasaac, item -> {
            // Se guarda la versión de 500 px, igual que la web; la de 300 es sólo para la grilla.
            pictogramUrl = "https://static.arasaac.org/pictograms/" + item.id + "/" + item.id + "_500.png";
            Glide.with(this).load(pictogramUrl).into(binding.imgPictogramaSeleccionado);
            binding.layoutPictogramaSeleccionado.setVisibility(View.VISIBLE);
        });
        binding.rvArasaac.setLayoutManager(new GridLayoutManager(this, 4));
        binding.rvArasaac.setAdapter(arasaacAdapter);

        binding.btnBuscar.setOnClickListener(v -> buscarArasaac());
        binding.etBuscarArasaac.setOnEditorActionListener((tv, actionId, event) -> {
            buscarArasaac();
            return true;
        });
    }

    private void buscarArasaac() {
        String termino = binding.etBuscarArasaac.getText().toString().trim();
        if (termino.isEmpty()) return;

        binding.progressArasaac.setVisibility(View.VISIBLE);
        String url = "https://api.arasaac.org/v1/pictograms/es/search/" + Uri.encode(termino);

        Request request = new Request.Builder().url(url).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> binding.progressArasaac.setVisibility(View.GONE));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String body = response.body().string();
                    JSONArray arr = new JSONArray(body);
                    List<ArasaacItem> items = new ArrayList<>();
                    for (int i = 0; i < Math.min(arr.length(), 24); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        int id = obj.getInt("_id");
                        String keyword = "";
                        if (obj.has("keywords") && obj.getJSONArray("keywords").length() > 0) {
                            keyword = obj.getJSONArray("keywords")
                                    .getJSONObject(0).getString("keyword");
                        }
                        items.add(new ArasaacItem(id, keyword));
                    }
                    runOnUiThread(() -> {
                        binding.progressArasaac.setVisibility(View.GONE);
                        resultadosArasaac.clear();
                        resultadosArasaac.addAll(items);
                        arasaacAdapter.notifyDataSetChanged();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> binding.progressArasaac.setVisibility(View.GONE));
                }
            }
        });
    }

    private void prepoblarFormulario() {
        binding.etNombre.setText(actividadExistente.name);
        binding.etDescripcion.setText(actividadExistente.description);
        if (actividadExistente.durationMinutes != null) {
            binding.etDuracion.setText(String.valueOf(actividadExistente.durationMinutes));
        }
        binding.switchPausable.setChecked(actividadExistente.pausable != null && actividadExistente.pausable);

        for (int i = 0; i < CATEGORIAS_KEYS.length; i++) {
            if (CATEGORIAS_KEYS[i].equals(actividadExistente.category)) {
                binding.spinnerCategoria.setSelection(i);
                break;
            }
        }

        if (actividadExistente.pictogramUrl != null) {
            pictogramUrl = actividadExistente.pictogramUrl;
            Glide.with(this).load(pictogramUrl).into(binding.imgPictogramaSeleccionado);
            binding.layoutPictogramaSeleccionado.setVisibility(View.VISIBLE);
        }
    }

    private void onGuardar() {
        String nombre = binding.etNombre.getText().toString().trim();
        if (nombre.isEmpty()) {
            binding.tilNombre.setError("El nombre es requerido");
            return;
        }
        binding.tilNombre.setError(null);

        String descripcion = binding.etDescripcion.getText().toString().trim();
        String categoria   = CATEGORIAS_KEYS[binding.spinnerCategoria.getSelectedItemPosition()];
        boolean pausable   = binding.switchPausable.isChecked();

        Integer duracion = null;
        String durStr = binding.etDuracion.getText().toString().trim();
        if (!durStr.isEmpty()) {
            try { duracion = Integer.parseInt(durStr); } catch (NumberFormatException ignored) {}
            if (duracion == null || duracion < 1 || duracion > 180) {
                binding.tilDuracion.setError(getString(R.string.error_duracion));
                return;
            }
        }
        binding.tilDuracion.setError(null);

        ActivityRequest req = new ActivityRequest(nombre, descripcion, categoria,
                colorActividad, pictogramUrl, duracion, pausable);

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnGuardar.setEnabled(false);

        retrofit2.Callback<ActivityItem> callback = new retrofit2.Callback<ActivityItem>() {
            @Override
            public void onResponse(retrofit2.Call<ActivityItem> call, retrofit2.Response<ActivityItem> response) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnGuardar.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(ActivityFormActivity.this, "Actividad guardada.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(ActivityFormActivity.this,
                            "Error al guardar la actividad.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ActivityItem> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnGuardar.setEnabled(true);
                Toast.makeText(ActivityFormActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        };

        if (actividadExistente != null) {
            api.updateActivity(actividadExistente.id, req).enqueue(callback);
        } else {
            api.createActivity(req).enqueue(callback);
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    // Modelos internos

    static class ArasaacItem {
        int id;
        String keyword;
        ArasaacItem(int id, String keyword) { this.id = id; this.keyword = keyword; }
    }

    // Adapter ARASAAC

    interface OnArasaacSelect { void select(ArasaacItem item); }

    static class ArasaacAdapter extends RecyclerView.Adapter<ArasaacAdapter.VH> {
        private final List<ArasaacItem> items;
        private final OnArasaacSelect listener;

        ArasaacAdapter(List<ArasaacItem> items, OnArasaacSelect listener) {
            this.items    = items;
            this.listener = listener;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_arasaac, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            ArasaacItem item = items.get(pos);
            String url = "https://static.arasaac.org/pictograms/" + item.id + "/" + item.id + "_300.png";
            Glide.with(h.imgPictogram.getContext()).load(url).into(h.imgPictogram);
            h.tvKeyword.setText(item.keyword);
            h.itemView.setOnClickListener(v -> listener.select(item));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView imgPictogram;
            TextView tvKeyword;
            VH(View v) {
                super(v);
                imgPictogram = v.findViewById(R.id.img_pictogram);
                tvKeyword    = v.findViewById(R.id.tv_keyword);
            }
        }
    }
}
