package com.teapp.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.chip.Chip;

import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityActivityCatalogBinding;
import com.teapp.model.ActivityItem;
import com.teapp.ui.adapter.ActivityAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityCatalogActivity extends AppCompatActivity implements ActivityAdapter.Listener {

    private static final int REQUEST_FORM  = 1;
    private static final int REQUEST_STEPS = 2;

    private static final String[] CAT_KEYS   = {"HYGIENE","MEAL","EDUCATION","PLAY","THERAPY","CHORES","SOCIAL","REST","SPECIAL_EVENT","OTHER"};
    private static final String[] CAT_LABELS = {"Higiene","Comidas","Educación","Juego","Terapia","Tareas","Social","Descanso","Evento especial","Otro"};

    private ActivityActivityCatalogBinding binding;
    private ApiService api;
    private final List<ActivityItem> actividades = new ArrayList<>();
    private final List<ActivityItem> actividadesFiltradas = new ArrayList<>();
    private ActivityAdapter adapter;
    private String categoriaSeleccionada = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityActivityCatalogBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.catalogo_actividades);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        api = ApiClient.getInstance(this).getApi();

        adapter = new ActivityAdapter(actividadesFiltradas, this);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(this::cargarActividades);
        binding.fabNueva.setOnClickListener(v ->
                startActivityForResult(new Intent(this, ActivityFormActivity.class), REQUEST_FORM));

        construirChips();
        cargarActividades();
    }

    private void construirChips() {
        for (int i = 0; i < CAT_KEYS.length; i++) {
            final String key = CAT_KEYS[i];
            Chip chip = new Chip(this);
            chip.setText(CAT_LABELS[i]);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.primary_variant);
            chip.setOnCheckedChangeListener((btn, checked) -> {
                categoriaSeleccionada = checked ? key : null;
                // Deselect other chips
                if (checked) {
                    for (int j = 0; j < binding.chipGroupCategorias.getChildCount(); j++) {
                        View v = binding.chipGroupCategorias.getChildAt(j);
                        if (v instanceof Chip && v != btn) ((Chip) v).setChecked(false);
                    }
                }
                aplicarFiltro();
            });
            binding.chipGroupCategorias.addView(chip);
        }
    }

    private void aplicarFiltro() {
        actividadesFiltradas.clear();
        if (categoriaSeleccionada == null) {
            actividadesFiltradas.addAll(actividades);
        } else {
            for (ActivityItem a : actividades) {
                if (categoriaSeleccionada.equals(a.category)) actividadesFiltradas.add(a);
            }
        }
        adapter.notifyDataSetChanged();
        binding.tvVacio.setVisibility(actividadesFiltradas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void cargarActividades() {
        binding.swipeRefresh.setRefreshing(true);
        api.getActivities().enqueue(new Callback<List<ActivityItem>>() {
            @Override
            public void onResponse(Call<List<ActivityItem>> call, Response<List<ActivityItem>> resp) {
                binding.swipeRefresh.setRefreshing(false);
                if (resp.isSuccessful() && resp.body() != null) {
                    actividades.clear();
                    actividades.addAll(resp.body());
                    aplicarFiltro();
                }
            }
            @Override
            public void onFailure(Call<List<ActivityItem>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(ActivityCatalogActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSelect(ActivityItem activity) {
        // Long-press: solo actividades personalizadas (no predefinidas)
        if (activity.predefined) {
            Toast.makeText(this, "Las actividades predefinidas no se pueden editar.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(activity.name)
                .setItems(new String[]{"Editar", "Gestionar pasos", "Eliminar"}, (d, which) -> {
                    if (which == 0)      editarActividad(activity);
                    else if (which == 1) gestionarPasos(activity);
                    else                 eliminarActividad(activity);
                })
                .show();
    }

    private void gestionarPasos(ActivityItem activity) {
        Intent intent = new Intent(this, ActivityStepsActivity.class);
        intent.putExtra(ActivityStepsActivity.EXTRA_ACTIVITY, activity);
        startActivityForResult(intent, REQUEST_STEPS);
    }

    private void editarActividad(ActivityItem activity) {
        Intent intent = new Intent(this, ActivityFormActivity.class);
        intent.putExtra(ActivityFormActivity.EXTRA_ACTIVITY, activity);
        startActivityForResult(intent, REQUEST_FORM);
    }

    private void eliminarActividad(ActivityItem activity) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar actividad")
                .setMessage("¿Eliminar \"" + activity.name + "\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) ->
                        api.deleteActivity(activity.id).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(ActivityCatalogActivity.this,
                                            "Actividad eliminada.", Toast.LENGTH_SHORT).show();
                                    cargarActividades();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(ActivityCatalogActivity.this,
                                        R.string.error_red, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_FORM || requestCode == REQUEST_STEPS) && resultCode == RESULT_OK)
            cargarActividades();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
