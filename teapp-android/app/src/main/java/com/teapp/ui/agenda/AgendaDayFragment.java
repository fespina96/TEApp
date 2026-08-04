package com.teapp.ui.agenda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
        /** Nuevo orden de una franja, ya reacomodado. La agenda supervisada no lo implementa. */
        default void onEntradasReordenadas(String day, String timeSlot, List<ScheduleEntry> ordenadas) {}
        /** La agenda de sólo lectura no muestra el asa de arrastre. */
        default boolean permiteReordenar() { return true; }
        /** Si no se puede editar, tocar una entrada no abre ningún menú. */
        default boolean permiteEditar() { return true; }
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

    private ItemTouchHelper touchManana, touchTarde, touchNoche;

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

        if (host == null || host.permiteReordenar()) {
            adapterManana.setReordenable(true);
            adapterTarde.setReordenable(true);
            adapterNoche.setReordenable(true);
            touchManana = engancharArrastre(binding.rvManana, adapterManana, manana, "MORNING");
            touchTarde  = engancharArrastre(binding.rvTarde,  adapterTarde,  tarde,  "AFTERNOON");
            touchNoche  = engancharArrastre(binding.rvNoche,  adapterNoche,  noche,  "NIGHT");
        }

        cargarEntradas();
    }

    /**
     * El arrastre no se dispara con una pulsación larga: eso abre el menú de la entrada.
     * Arranca al tocar el asa, y el nuevo orden se guarda recién al soltar.
     */
    private ItemTouchHelper engancharArrastre(RecyclerView rv, ScheduleEntryAdapter adapter,
                                              List<ScheduleEntry> lista, String timeSlot) {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean isLongPressDragEnabled() { return false; }

            @Override
            public boolean onMove(@NonNull RecyclerView r,
                                  @NonNull RecyclerView.ViewHolder origen,
                                  @NonNull RecyclerView.ViewHolder destino) {
                adapter.moverItem(origen.getBindingAdapterPosition(), destino.getBindingAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) { }

            @Override
            public void clearView(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder holder) {
                super.clearView(r, holder);
                if (host != null) host.onEntradasReordenadas(day, timeSlot, new ArrayList<>(lista));
            }
        });
        helper.attachToRecyclerView(rv);
        return helper;
    }

    /**
     * Si el día todavía tiene contenido por encima. El SwipeRefreshLayout lo consulta para
     * distinguir un desplazamiento normal de un gesto de recarga.
     */
    public boolean puedeDesplazarseHaciaArriba() {
        return binding != null && binding.getRoot().canScrollVertically(-1);
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
        // Un toque abre el menú con las acciones, no la edición directa: así se
        // puede elegir también eliminar sin tener que descubrir la pulsación larga.
        onEntryLongClick(entry);
    }

    @Override
    public void onEntryLongClick(ScheduleEntry entry) {
        // En la agenda supervisada el menú no tenía ninguna acción con efecto:
        // el terapeuta edita desde "Gestionar agenda", así que no se abre.
        if (host == null || !host.permiteEditar()) return;
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
    public void onStartDrag(RecyclerView.ViewHolder holder) {
        RecyclerView rv = (RecyclerView) holder.itemView.getParent();
        if (rv == binding.rvManana && touchManana != null)      touchManana.startDrag(holder);
        else if (rv == binding.rvTarde && touchTarde != null)   touchTarde.startDrag(holder);
        else if (rv == binding.rvNoche && touchNoche != null)   touchNoche.startDrag(holder);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
