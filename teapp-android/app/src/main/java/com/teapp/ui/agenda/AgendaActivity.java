package com.teapp.ui.agenda;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayoutMediator;
import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityAgendaBinding;
import com.teapp.model.Child;
import com.teapp.model.CompletionRequest;
import com.teapp.model.ScheduleEntry;
import com.teapp.model.ScheduleEntryRequest;
import com.teapp.model.WeeklySchedule;
import com.teapp.util.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.android.material.textfield.TextInputEditText;

public class AgendaActivity extends AppCompatActivity
        implements AgendaDayFragment.HostCallback {

    private static final int REQ_PICK_ACTIVITY = 100;
    private static final String[] DIAS = {
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    };
    private static final String[] ETIQUETAS = {
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    };

    private ActivityAgendaBinding binding;
    private ApiService api;
    private String childId;
    private Child child;
    private WeeklySchedule agenda;
    private final List<AgendaDayFragment> fragmentos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAgendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api     = ApiClient.getInstance(this).getApi();
        childId = getIntent().getStringExtra(Constants.EXTRA_CHILD_ID);
        child   = (Child) getIntent().getSerializableExtra(Constants.EXTRA_CHILD);

        String nombre = getIntent().getStringExtra(Constants.EXTRA_CHILD_NAME);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(nombre != null ? nombre : "Agenda");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        configurarViewPager();

        binding.fabAgregar.setOnClickListener(v -> abrirSelectorActividad());
        binding.btnModoNino.setOnClickListener(v -> abrirModoNino());
        binding.btnReset.setOnClickListener(v -> confirmarResetSemana());
        binding.swipeRefresh.setOnRefreshListener(this::cargarAgenda);

        cargarAgenda();
    }

    private void configurarViewPager() {
        for (String dia : DIAS) fragmentos.add(AgendaDayFragment.newInstance(dia));

        binding.viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public Fragment createFragment(int position) { return fragmentos.get(position); }
            @Override
            public int getItemCount() { return DIAS.length; }
        });

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, pos) -> tab.setText(ETIQUETAS[pos])).attach();

        int hoy = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int idx  = hoy == Calendar.SUNDAY ? 6 : hoy - 2;
        if (idx < 0) idx = 0;
        binding.viewPager.setCurrentItem(idx, false);
    }

    private void cargarAgenda() {
        binding.swipeRefresh.setRefreshing(true);
        api.getSchedule(childId).enqueue(new Callback<WeeklySchedule>() {
            @Override
            public void onResponse(Call<WeeklySchedule> call, Response<WeeklySchedule> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    agenda = response.body();
                    for (AgendaDayFragment f : fragmentos) f.cargarEntradas();
                }
            }
            @Override
            public void onFailure(Call<WeeklySchedule> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(AgendaActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void abrirSelectorActividad() {
        int pos = binding.viewPager.getCurrentItem();
        String diaActual = DIAS[pos];
        Intent intent = new Intent(this, ActivityPickerActivity.class);
        intent.putExtra("child_id", childId);
        intent.putExtra("day", diaActual);
        startActivityForResult(intent, REQ_PICK_ACTIVITY);
    }

    private void abrirModoNino() {
        Intent intent = new Intent(this, KidModeActivity.class);
        intent.putExtra(Constants.EXTRA_CHILD_ID, childId);
        intent.putExtra(Constants.EXTRA_CHILD, child);
        startActivity(intent);
    }

    private void confirmarResetSemana() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.resetear_semana)
                .setMessage(R.string.confirmar_reset)
                .setPositiveButton("Resetear", (d, w) -> resetearSemana())
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void resetearSemana() {
        api.resetWeek(childId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AgendaActivity.this, "Semana reseteada.", Toast.LENGTH_SHORT).show();
                    cargarAgenda();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AgendaActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK_ACTIVITY && resultCode == RESULT_OK) cargarAgenda();
    }

    // HostCallback

    @Override
    public Map<String, List<ScheduleEntry>> getEntriesForDay(String day) {
        if (agenda == null || agenda.week == null) return null;
        return agenda.week.get(day);
    }

    @Override
    public void onEntrySelected(ScheduleEntry entry) {
        if (entry.isCompletedToday()) {
            api.unmarkCompleted(childId, entry.id, new CompletionRequest(ScheduleEntry.todayIso()))
                    .enqueue(simpleCallback());
        } else {
            api.markCompleted(childId, entry.id, new CompletionRequest(ScheduleEntry.todayIso()))
                    .enqueue(simpleCallback());
        }
    }

    @Override
    public void onEntryDelete(ScheduleEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar actividad")
                .setMessage("¿Eliminar \"" + entry.activity.name + "\" de la agenda?")
                .setPositiveButton("Eliminar", (d, w) ->
                        api.deleteEntry(childId, entry.id).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> c, Response<Void> r) {
                                if (r.isSuccessful()) cargarAgenda();
                                else Toast.makeText(AgendaActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onFailure(Call<Void> c, Throwable t) {
                                Toast.makeText(AgendaActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    /**
     * Guarda el orden que quedó tras arrastrar: una actualización por entrada con su
     * nueva posición. Si alguna falla se recarga la agenda, que vuelve al orden real.
     */
    @Override
    public void onEntradasReordenadas(String day, String timeSlot, List<ScheduleEntry> ordenadas) {
        if (ordenadas.isEmpty()) return;
        final int total = ordenadas.size();
        final int[] respuestas = {0};
        final boolean[] huboError = {false};

        for (int i = 0; i < total; i++) {
            ScheduleEntry entrada = ordenadas.get(i);
            if (entrada.sortOrder == i) {
                // esta entrada ya estaba en su lugar: no hace falta actualizarla
                if (++respuestas[0] == total) terminarReorden(huboError[0]);
                continue;
            }
            ScheduleEntryRequest req = new ScheduleEntryRequest();
            req.sortOrder = i;
            api.updateEntry(childId, entrada.id, req).enqueue(new Callback<ScheduleEntry>() {
                @Override
                public void onResponse(Call<ScheduleEntry> c, Response<ScheduleEntry> r) {
                    if (!r.isSuccessful()) huboError[0] = true;
                    if (++respuestas[0] == total) terminarReorden(huboError[0]);
                }
                @Override
                public void onFailure(Call<ScheduleEntry> c, Throwable t) {
                    huboError[0] = true;
                    if (++respuestas[0] == total) terminarReorden(true);
                }
            });
        }
    }

    private void terminarReorden(boolean huboError) {
        if (!huboError) return;
        Toast.makeText(this, R.string.error_reordenar, Toast.LENGTH_SHORT).show();
        cargarAgenda();
    }

    @Override
    public void onEntrySettings(ScheduleEntry entry) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_entry_settings, null);

        TextInputEditText etNotas    = dialogView.findViewById(R.id.et_notas);
        TextInputEditText etDuracion = dialogView.findViewById(R.id.et_duracion);
        SwitchMaterial swPausable    = dialogView.findViewById(R.id.switch_pausable);
        SwitchMaterial swRequire     = dialogView.findViewById(R.id.switch_require_full);

        if (entry.notes != null)        etNotas.setText(entry.notes);
        if (entry.durationMinutes != null) etDuracion.setText(String.valueOf(entry.durationMinutes));
        swPausable.setChecked(Boolean.TRUE.equals(entry.pausable));
        swRequire.setChecked(Boolean.TRUE.equals(entry.requireFullTimer));

        new AlertDialog.Builder(this)
                .setTitle(R.string.configurar_entrada)
                .setView(dialogView)
                .setPositiveButton(R.string.guardar, (d, w) -> {
                    ScheduleEntryRequest req = new ScheduleEntryRequest();
                    req.notes = etNotas.getText().toString().trim();
                    String durStr = etDuracion.getText().toString().trim();
                    if (!durStr.isEmpty()) {
                        try { req.durationMinutes = Integer.parseInt(durStr); } catch (Exception ignored) {}
                        if (req.durationMinutes == null || req.durationMinutes < 1 || req.durationMinutes > 180) {
                            Toast.makeText(this, R.string.error_duracion, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    req.pausable        = swPausable.isChecked();
                    req.requireFullTimer = swRequire.isChecked();
                    api.updateEntry(childId, entry.id, req).enqueue(new Callback<ScheduleEntry>() {
                        @Override
                        public void onResponse(Call<ScheduleEntry> call, Response<ScheduleEntry> response) {
                            if (response.isSuccessful()) cargarAgenda();
                        }
                        @Override
                        public void onFailure(Call<ScheduleEntry> call, Throwable t) {
                            Toast.makeText(AgendaActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private Callback<Void> simpleCallback() {
        return new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) { cargarAgenda(); }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AgendaActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
