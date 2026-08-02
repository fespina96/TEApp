package com.teapp.ui.agenda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityActivityPickerBinding;
import com.teapp.model.ActivityItem;
import com.teapp.model.ScheduleEntryRequest;
import com.teapp.ui.adapter.ActivityAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityPickerActivity extends AppCompatActivity implements ActivityAdapter.Listener {

    private ActivityActivityPickerBinding binding;
    private ApiService api;
    private String childId;
    private String diaSeleccionado;
    private String franjaSeleccionada = "MORNING";
    private final List<ActivityItem> actividades = new ArrayList<>();
    private ActivityAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityActivityPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api              = ApiClient.getInstance(this).getApi();
        childId          = getIntent().getStringExtra("child_id");
        diaSeleccionado  = getIntent().getStringExtra("day");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Agregar actividad");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new ActivityAdapter(actividades, this);
        adapter.setMostrarDistintivoPredefinida(false);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter);

        binding.chipManana.setOnClickListener(v -> franjaSeleccionada = "MORNING");
        binding.chipTarde.setOnClickListener(v  -> franjaSeleccionada = "AFTERNOON");
        binding.chipNoche.setOnClickListener(v  -> franjaSeleccionada = "NIGHT");
        binding.chipManana.setChecked(true);

        cargarActividades();
    }

    private void cargarActividades() {
        binding.progressBar.setVisibility(View.VISIBLE);
        api.getActivities().enqueue(new Callback<List<ActivityItem>>() {
            @Override
            public void onResponse(Call<List<ActivityItem>> call, Response<List<ActivityItem>> resp) {
                binding.progressBar.setVisibility(View.GONE);
                if (resp.isSuccessful() && resp.body() != null) {
                    actividades.clear();
                    actividades.addAll(resp.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<ActivityItem>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(ActivityPickerActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Antes de agregar se ofrece ajustar duración, pausa y exigencia del temporizador,
     * igual que el diálogo de la versión web.
     */
    @Override
    public void onSelect(ActivityItem activity) {
        View vista = LayoutInflater.from(this).inflate(R.layout.dialog_entry_settings, null);
        TextInputEditText etNotas    = vista.findViewById(R.id.et_notas);
        TextInputEditText etDuracion = vista.findViewById(R.id.et_duracion);
        SwitchMaterial swPausable    = vista.findViewById(R.id.switch_pausable);
        SwitchMaterial swRequire     = vista.findViewById(R.id.switch_require_full);

        if (activity.durationMinutes != null) etDuracion.setText(String.valueOf(activity.durationMinutes));
        swPausable.setChecked(!Boolean.FALSE.equals(activity.pausable));
        swRequire.setChecked(false);

        new AlertDialog.Builder(this)
                .setTitle(activity.name)
                .setView(vista)
                .setPositiveButton(R.string.agregar, (d, w) -> {
                    Integer duracion = null;
                    String durStr = etDuracion.getText().toString().trim();
                    if (!durStr.isEmpty()) {
                        try { duracion = Integer.parseInt(durStr); } catch (NumberFormatException ignored) {}
                        if (duracion == null || duracion < 1 || duracion > 180) {
                            Toast.makeText(this, R.string.error_duracion, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    String notas = etNotas.getText().toString().trim();
                    agregarEntradas(activity, duracion, swPausable.isChecked(),
                            swRequire.isChecked(), notas.isEmpty() ? null : notas);
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void agregarEntradas(ActivityItem activity, Integer duracion, boolean pausable,
                                 boolean requiereTemporizador, String notas) {
        String[] dias = binding.switchTodosLosDias.isChecked()
                ? new String[]{"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}
                : new String[]{diaSeleccionado};

        // Se espera a que terminen todas las altas antes de cerrar la pantalla.
        AtomicInteger pendientes = new AtomicInteger(dias.length);
        AtomicBoolean huboError  = new AtomicBoolean(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        for (String dia : dias) {
            ScheduleEntryRequest req = new ScheduleEntryRequest();
            req.activityId = activity.id;
            req.dayOfWeek  = dia;
            req.timeSlot   = franjaSeleccionada;
            req.durationMinutes   = duracion;
            req.pausable          = pausable;
            req.requireFullTimer  = requiereTemporizador;
            req.notes             = notas;

            api.addEntry(childId, req).enqueue(new Callback<com.teapp.model.ScheduleEntry>() {
                @Override
                public void onResponse(Call<com.teapp.model.ScheduleEntry> call,
                                       Response<com.teapp.model.ScheduleEntry> response) {
                    if (!response.isSuccessful()) huboError.set(true);
                    alTerminarAlta(pendientes, huboError);
                }
                @Override
                public void onFailure(Call<com.teapp.model.ScheduleEntry> call, Throwable t) {
                    huboError.set(true);
                    alTerminarAlta(pendientes, huboError);
                }
            });
        }
    }

    private void alTerminarAlta(AtomicInteger pendientes, AtomicBoolean huboError) {
        if (pendientes.decrementAndGet() > 0) return;
        binding.progressBar.setVisibility(View.GONE);
        if (huboError.get()) {
            Toast.makeText(this, "Error al agregar actividad.", Toast.LENGTH_SHORT).show();
        } else {
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
