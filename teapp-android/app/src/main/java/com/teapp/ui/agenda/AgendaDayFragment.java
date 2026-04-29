package com.teapp.ui.agenda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.teapp.R;
import com.teapp.databinding.FragmentAgendaDayBinding;
import com.teapp.model.ScheduleEntry;
import com.teapp.ui.adapter.ScheduleEntryAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgendaDayFragment extends Fragment implements ScheduleEntryAdapter.Listener {

    private static final String ARG_DAY = "day";

    public interface HostCallback {
        void onEntrySelected(ScheduleEntry entry);
        void onEntryDelete(ScheduleEntry entry);
        void onEntrySettings(ScheduleEntry entry);
        Map<String, List<ScheduleEntry>> getEntriesForDay(String day);
    }

    private FragmentAgendaDayBinding binding;
    private String day;
    private HostCallback host;

    private final List<ScheduleEntry> manana = new ArrayList<>();
    private final List<ScheduleEntry> tarde  = new ArrayList<>();
    private final List<ScheduleEntry> noche  = new ArrayList<>();

    private ScheduleEntryAdapter adapterManana;
    private ScheduleEntryAdapter adapterTarde;
    private ScheduleEntryAdapter adapterNoche;

    public static AgendaDayFragment newInstance(String day) {
        AgendaDayFragment f = new AgendaDayFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DAY, day);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        day = getArguments() != null ? getArguments().getString(ARG_DAY) : "MONDAY";
        if (requireActivity() instanceof HostCallback) {
            host = (HostCallback) requireActivity();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAgendaDayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapterManana = new ScheduleEntryAdapter(manana, this);
        adapterTarde  = new ScheduleEntryAdapter(tarde, this);
        adapterNoche  = new ScheduleEntryAdapter(noche, this);

        binding.rvManana.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvTarde.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvNoche.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.rvManana.setAdapter(adapterManana);
        binding.rvTarde.setAdapter(adapterTarde);
        binding.rvNoche.setAdapter(adapterNoche);

        cargarEntradas();
    }

    public void cargarEntradas() {
        if (host == null || binding == null) return;
        Map<String, List<ScheduleEntry>> slots = host.getEntriesForDay(day);

        manana.clear(); tarde.clear(); noche.clear();

        if (slots != null) {
            if (slots.containsKey("MORNING")   && slots.get("MORNING")   != null) manana.addAll(slots.get("MORNING"));
            if (slots.containsKey("AFTERNOON") && slots.get("AFTERNOON") != null) tarde.addAll(slots.get("AFTERNOON"));
            if (slots.containsKey("NIGHT")     && slots.get("NIGHT")     != null) noche.addAll(slots.get("NIGHT"));
        }

        adapterManana.notifyDataSetChanged();
        adapterTarde.notifyDataSetChanged();
        adapterNoche.notifyDataSetChanged();

        boolean vacio = manana.isEmpty() && tarde.isEmpty() && noche.isEmpty();
        binding.tvVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEntryClick(ScheduleEntry entry) {
        if (host != null) host.onEntrySelected(entry);
    }

    @Override
    public void onEntryLongClick(ScheduleEntry entry) {
        if (host == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle(entry.activity != null ? entry.activity.name : "Actividad")
                .setItems(new String[]{
                        getString(R.string.editar_configuracion),
                        getString(R.string.eliminar)
                }, (d, which) -> {
                    if (which == 0) host.onEntrySettings(entry);
                    else            host.onEntryDelete(entry);
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
