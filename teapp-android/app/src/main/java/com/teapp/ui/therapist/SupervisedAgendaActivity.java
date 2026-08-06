package com.teapp.ui.therapist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayoutMediator;
import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivitySupervisedAgendaBinding;
import com.teapp.model.Child;
import com.teapp.model.ScheduleEntry;
import com.teapp.model.WeeklySchedule;
import com.teapp.ui.agenda.AgendaActivity;
import com.teapp.ui.agenda.AgendaDayFragment;
import com.teapp.util.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SupervisedAgendaActivity extends AppCompatActivity
        implements AgendaDayFragment.HostCallback {

    public static final String EXTRA_PARENT_ID = "parent_id";
    public static final String EXTRA_CHILD_ID  = "child_id";
    public static final String EXTRA_CHILD_NAME = "child_name";

    private static final String[] DIAS = {
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    };
    private static final String[] ETIQUETAS = {
            "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    };

    private ActivitySupervisedAgendaBinding binding;
    private ApiService api;
    private String parentId, childId, childName;
    private Child child;
    private WeeklySchedule agenda;
    private final List<AgendaDayFragment> fragmentos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySupervisedAgendaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        parentId = getIntent().getStringExtra(EXTRA_PARENT_ID);
        childId  = getIntent().getStringExtra(EXTRA_CHILD_ID);
        childName = getIntent().getStringExtra(EXTRA_CHILD_NAME);
        child = (Child) getIntent().getSerializableExtra(Constants.EXTRA_CHILD);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(childName != null ? childName : getString(R.string.agenda_supervisada));
            getSupportActionBar().setSubtitle(getString(R.string.agenda_supervisada));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        api = ApiClient.getInstance(this).getApi();
        configurarViewPager();
        binding.swipeRefresh.setOnRefreshListener(this::cargarAgenda);
        binding.swipeRefresh.setOnChildScrollUpCallback((padre, hijo) -> {
            AgendaDayFragment visible = diaVisible();
            return visible != null && visible.puedeDesplazarseHaciaArriba();
        });
        binding.fabGestionar.setOnClickListener(v -> abrirAgendaEditable());
        cargarAgenda();
    }

    /** La única página en estado RESUMED es la que se está viendo. */
    private AgendaDayFragment diaVisible() {
        for (Fragment f : getSupportFragmentManager().getFragments()) {
            if (f instanceof AgendaDayFragment && f.isResumed()) return (AgendaDayFragment) f;
        }
        return null;
    }

    private void configurarViewPager() {
        for (String dia : DIAS) fragmentos.add(AgendaDayFragment.newInstance(dia));

        binding.viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override public Fragment createFragment(int pos) { return fragmentos.get(pos); }
            @Override public int getItemCount() { return DIAS.length; }
        });

        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, pos) -> tab.setText(ETIQUETAS[pos])).attach();

        int hoy = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int idx = hoy == Calendar.SUNDAY ? 6 : hoy - 2;
        binding.viewPager.setCurrentItem(Math.max(0, idx), false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarAgenda();
    }

    /** Abre la misma pantalla de gestion que usa el padre sobre este participante. */
    private void abrirAgendaEditable() {
        Intent intent = new Intent(this, AgendaActivity.class);
        intent.putExtra(Constants.EXTRA_CHILD_ID, childId);
        intent.putExtra(Constants.EXTRA_CHILD_NAME, childName);
        intent.putExtra(Constants.EXTRA_CHILD, child);
        startActivity(intent);
    }

    private void cargarAgenda() {
        binding.swipeRefresh.setRefreshing(true);
        api.getSupervisedSchedule(parentId, childId).enqueue(new Callback<WeeklySchedule>() {
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
                Toast.makeText(SupervisedAgendaActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // HostCallback (read-only: no actions)

    @Override
    public Map<String, List<ScheduleEntry>> getEntriesForDay(String day) {
        if (agenda == null || agenda.week == null) return null;
        return agenda.week.get(day);
    }

    @Override public boolean permiteReordenar() { return false; }
    @Override public boolean permiteEditar()    { return false; }

    // El terapeuta consulta esta agenda sin modificarla: los tres gestos no hacen nada.
    @Override public void onEntrySelected(ScheduleEntry entry) {}
    @Override public void onEntryDelete(ScheduleEntry entry)   {}
    @Override public void onEntrySettings(ScheduleEntry entry) {}

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
