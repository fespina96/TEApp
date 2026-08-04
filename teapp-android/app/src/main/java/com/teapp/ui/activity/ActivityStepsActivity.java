package com.teapp.ui.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityActivityStepsBinding;
import com.teapp.model.ActivityItem;
import com.teapp.model.ActivityStep;
import com.teapp.model.ActivityStepRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;

public class ActivityStepsActivity extends AppCompatActivity {

    public static final String EXTRA_ACTIVITY = "activity";
    private static final int REQ_IMG = 300;

    private ActivityActivityStepsBinding binding;
    private ApiService api;
    private ActivityItem activity;
    private StepsAdapter adapter;
    private final List<StepDraft> pasos = new ArrayList<>();
    private int pendingImageIdx = -1;
    private final OkHttpClient http = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityActivityStepsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        activity = (ActivityItem) getIntent().getSerializableExtra(EXTRA_ACTIVITY);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pasos: " + (activity != null ? activity.name : ""));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        api = ApiClient.getInstance(this).getApi();
        adapter = new StepsAdapter(pasos);
        binding.rvSteps.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSteps.setAdapter(adapter);

        binding.fabAgregar.setOnClickListener(v -> agregarPaso());
        binding.btnGuardar.setOnClickListener(v -> guardar());

        if (activity != null) cargarPasos();
    }

    private void cargarPasos() {
        binding.progressBar.setVisibility(View.VISIBLE);
        api.getSteps(activity.id).enqueue(new retrofit2.Callback<List<ActivityStep>>() {
            @Override
            public void onResponse(retrofit2.Call<List<ActivityStep>> call,
                                   retrofit2.Response<List<ActivityStep>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    pasos.clear();
                    for (ActivityStep s : response.body()) {
                        pasos.add(new StepDraft(s.title,
                                s.description != null ? s.description : "",
                                s.imageBase64, s.pictogramUrl));
                    }
                    actualizarVista();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<ActivityStep>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(ActivityStepsActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void agregarPaso() {
        pasos.add(new StepDraft("", "", null, null));
        actualizarVista();
        binding.rvSteps.smoothScrollToPosition(pasos.size() - 1);
    }

    private void guardar() {
        // Sync text fields from adapter views
        for (int i = 0; i < pasos.size(); i++) {
            if (pasos.get(i).titulo.trim().isEmpty()) {
                Toast.makeText(this, "Todos los pasos deben tener título.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        List<ActivityStepRequest> reqs = new ArrayList<>();
        for (StepDraft d : pasos) {
            reqs.add(new ActivityStepRequest(d.titulo.trim(), d.descripcion, d.imageBase64, d.pictogramUrl));
        }

        binding.btnGuardar.setEnabled(false);
        api.saveSteps(activity.id, reqs).enqueue(new retrofit2.Callback<List<ActivityStep>>() {
            @Override
            public void onResponse(retrofit2.Call<List<ActivityStep>> call,
                                   retrofit2.Response<List<ActivityStep>> response) {
                binding.btnGuardar.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(ActivityStepsActivity.this, "Pasos guardados.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(ActivityStepsActivity.this, "Error al guardar.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<List<ActivityStep>> call, Throwable t) {
                binding.btnGuardar.setEnabled(true);
                Toast.makeText(ActivityStepsActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    void moverArriba(int idx) {
        if (idx <= 0) return;
        StepDraft tmp = pasos.get(idx - 1);
        pasos.set(idx - 1, pasos.get(idx));
        pasos.set(idx, tmp);
        adapter.notifyItemRangeChanged(idx - 1, 2);
    }

    void moverAbajo(int idx) {
        if (idx >= pasos.size() - 1) return;
        StepDraft tmp = pasos.get(idx + 1);
        pasos.set(idx + 1, pasos.get(idx));
        pasos.set(idx, tmp);
        adapter.notifyItemRangeChanged(idx, 2);
    }

    void eliminar(int idx) {
        pasos.remove(idx);
        adapter.notifyItemRemoved(idx);
        adapter.notifyItemRangeChanged(idx, pasos.size());
        actualizarVista();
    }

    /**
     * Abre el buscador de pictogramas para un paso. Antes este botón hacía la
     * búsqueda con el título del paso y se quedaba con el primer resultado sin
     * preguntar: si no era el que servía, no había forma de cambiarlo.
     */
    void abrirArasaac(int idx) {
        pendingImageIdx = idx;

        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_arasaac_picker, null);
        android.widget.EditText etBuscar = vista.findViewById(R.id.et_buscar);
        android.widget.Button btnBuscar  = vista.findViewById(R.id.btn_buscar);
        ProgressBar progreso            = vista.findViewById(R.id.progress_arasaac);
        TextView tvSinResultados        = vista.findViewById(R.id.tv_sin_resultados);
        RecyclerView rv                 = vista.findViewById(R.id.rv_arasaac);

        List<ActivityFormActivity.ArasaacItem> resultados = new ArrayList<>();

        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle(R.string.elegir_pictograma)
                .setView(vista)
                .setNegativeButton(R.string.cancelar, null)
                .create();

        ActivityFormActivity.ArasaacAdapter adaptador =
                new ActivityFormActivity.ArasaacAdapter(resultados, item -> {
                    if (pendingImageIdx >= 0 && pendingImageIdx < pasos.size()) {
                        pasos.get(pendingImageIdx).pictogramUrl =
                                "https://static.arasaac.org/pictograms/" + item.id + "/" + item.id + "_500.png";
                        pasos.get(pendingImageIdx).imageBase64 = null;
                        adapter.notifyItemChanged(pendingImageIdx);
                    }
                    dialogo.dismiss();
                });
        rv.setLayoutManager(new GridLayoutManager(this, 4));
        rv.setAdapter(adaptador);

        Runnable buscar = () -> {
            String termino = etBuscar.getText().toString().trim();
            if (termino.isEmpty()) return;
            progreso.setVisibility(View.VISIBLE);
            tvSinResultados.setVisibility(View.GONE);
            buscarPictogramas(termino, encontrados -> {
                progreso.setVisibility(View.GONE);
                resultados.clear();
                resultados.addAll(encontrados);
                adaptador.notifyDataSetChanged();
                tvSinResultados.setVisibility(encontrados.isEmpty() ? View.VISIBLE : View.GONE);
            });
        };

        btnBuscar.setOnClickListener(v -> buscar.run());
        etBuscar.setOnEditorActionListener((tv, actionId, event) -> { buscar.run(); return true; });

        // El título del paso suele ser un buen punto de partida, pero se puede cambiar.
        String sugerencia = pasos.get(idx).titulo.trim();
        etBuscar.setText(sugerencia);
        dialogo.show();
        if (!sugerencia.isEmpty()) buscar.run();
    }

    /** Consulta la API de ARASAAC y devuelve los resultados en el hilo principal. */
    private void buscarPictogramas(String termino,
                                   java.util.function.Consumer<List<ActivityFormActivity.ArasaacItem>> alTerminar) {
        String url = "https://api.arasaac.org/v1/pictograms/es/search/" + Uri.encode(termino);
        Request req = new Request.Builder().url(url).build();
        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    alTerminar.accept(new ArrayList<>());
                    Toast.makeText(ActivityStepsActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                List<ActivityFormActivity.ArasaacItem> items = new ArrayList<>();
                try {
                    JSONArray arr = new JSONArray(response.body().string());
                    for (int i = 0; i < Math.min(arr.length(), 24); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String palabra = "";
                        if (obj.has("keywords") && obj.getJSONArray("keywords").length() > 0) {
                            palabra = obj.getJSONArray("keywords").getJSONObject(0).getString("keyword");
                        }
                        items.add(new ActivityFormActivity.ArasaacItem(obj.getInt("_id"), palabra));
                    }
                } catch (Exception e) {
                    // Una respuesta que no se puede leer no es lo mismo que una
                    // búsqueda sin resultados: conviene decirlo.
                    runOnUiThread(() -> Toast.makeText(ActivityStepsActivity.this,
                            R.string.error_red, Toast.LENGTH_SHORT).show());
                }
                runOnUiThread(() -> alTerminar.accept(items));
            }
        });
    }

    void abrirGaleria(int idx) {
        pendingImageIdx = idx;
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQ_IMG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_IMG && resultCode == RESULT_OK
                && data != null && data.getData() != null && pendingImageIdx >= 0) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                Bitmap bmp = BitmapFactory.decodeStream(is);
                int max = 512;
                float r = (float) max / Math.max(bmp.getWidth(), bmp.getHeight());
                if (r < 1) bmp = Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth()*r), (int)(bmp.getHeight()*r), true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                String b64 = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                pasos.get(pendingImageIdx).imageBase64  = b64;
                pasos.get(pendingImageIdx).pictogramUrl = null;
                adapter.notifyItemChanged(pendingImageIdx);
            } catch (Exception e) {
                Toast.makeText(this, "Error al cargar imagen.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Refresca la lista y el cartel de "sin pasos". Antes sólo tocaba el cartel,
     * así que los pasos traídos del servidor entraban en la lista pero el
     * RecyclerView nunca se enteraba y la pantalla se veía vacía.
     */
    private void actualizarVista() {
        adapter.notifyDataSetChanged();
        binding.tvVacio.setVisibility(pasos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    // Modelos

    static class StepDraft {
        String titulo, descripcion, imageBase64, pictogramUrl;
        StepDraft(String t, String d, String img, String pic) {
            titulo = t; descripcion = d; imageBase64 = img; pictogramUrl = pic;
        }
    }

    // Adapter

    class StepsAdapter extends RecyclerView.Adapter<StepsAdapter.VH> {
        private final List<StepDraft> items;
        StepsAdapter(List<StepDraft> items) { this.items = items; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_step_edit, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            StepDraft draft = items.get(position);
            h.tvNumero.setText(String.valueOf(position + 1));

            // Sincronizar texto al scroll
            h.etTitulo.setText(draft.titulo);
            h.etTitulo.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) draft.titulo = h.etTitulo.getText().toString();
            });
            h.etTitulo.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) { draft.titulo = s.toString(); }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });

            // Imagen
            String img = draft.pictogramUrl != null ? draft.pictogramUrl : draft.imageBase64;
            if (img != null) {
                Glide.with(h.imgPaso.getContext()).load(img).into(h.imgPaso);
                h.imgPaso.setVisibility(View.VISIBLE);
                h.btnQuitarImg.setVisibility(View.VISIBLE);
            } else {
                h.imgPaso.setVisibility(View.GONE);
                h.btnQuitarImg.setVisibility(View.GONE);
            }

            h.btnSubir.setOnClickListener(v -> moverArriba(h.getAdapterPosition()));
            h.btnBajar.setOnClickListener(v -> moverAbajo(h.getAdapterPosition()));
            h.btnEliminar.setOnClickListener(v -> eliminar(h.getAdapterPosition()));
            h.btnArasaac.setOnClickListener(v -> abrirArasaac(h.getAdapterPosition()));
            h.btnQuitarImg.setOnClickListener(v -> {
                int pos = h.getAdapterPosition();
                items.get(pos).imageBase64  = null;
                items.get(pos).pictogramUrl = null;
                notifyItemChanged(pos);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            android.widget.TextView tvNumero;
            TextInputEditText etTitulo;
            ImageView imgPaso;
            ImageButton btnSubir, btnBajar, btnEliminar;
            android.widget.Button btnArasaac, btnQuitarImg;

            VH(View v) {
                super(v);
                tvNumero    = v.findViewById(R.id.tv_numero);
                etTitulo    = v.findViewById(R.id.et_titulo);
                imgPaso     = v.findViewById(R.id.img_paso);
                btnSubir    = v.findViewById(R.id.btn_subir);
                btnBajar    = v.findViewById(R.id.btn_bajar);
                btnEliminar = v.findViewById(R.id.btn_eliminar);
                btnArasaac  = v.findViewById(R.id.btn_arasaac);
                btnQuitarImg = v.findViewById(R.id.btn_quitar_img);
            }
        }
    }
}
