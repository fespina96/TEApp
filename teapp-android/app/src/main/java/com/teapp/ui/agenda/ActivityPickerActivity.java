package com.teapp.ui.agenda;

import android.os.Bundle;
import android.view.View;
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

    @Override
    public void onSelect(ActivityItem activity) {
        ScheduleEntryRequest req = new ScheduleEntryRequest();
        req.activityId = activity.id;
        req.dayOfWeek  = diaSeleccionado;
        req.timeSlot   = franjaSeleccionada;
        req.durationMinutes = activity.durationMinutes;
        req.pausable       = activity.pausable;

        api.addEntry(childId, req).enqueue(new Callback<com.teapp.model.ScheduleEntry>() {
            @Override
            public void onResponse(Call<com.teapp.model.ScheduleEntry> call,
                                   Response<com.teapp.model.ScheduleEntry> response) {
                if (response.isSuccessful()) {
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(ActivityPickerActivity.this,
                            "Error al agregar actividad.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<com.teapp.model.ScheduleEntry> call, Throwable t) {
                Toast.makeText(ActivityPickerActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
