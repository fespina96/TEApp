package com.teapp.ui.agenda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.Chip;
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
    /** Orden de los chips de día, en el mismo orden en que se muestran. */
    private static final String[] DIAS = {
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    };

    /** Franjas en el mismo orden en que se muestran los chips. */
    private static final String[] FRANJAS = {"MORNING", "AFTERNOON", "NIGHT"};

    /** Mismas categorías y en el mismo orden que el catálogo. */
    private static final String[] CAT_KEYS = {
            "HYGIENE", "MEAL", "EDUCATION", "PLAY", "THERAPY",
            "REST", "OUTDOOR", "CUSTOM", "SPECIAL_EVENT"
    };
    private static final String[] CAT_LABELS = {
            "Higiene", "Comidas", "Educación", "Juego", "Terapia",
            "Descanso", "Aire libre", "Personalizada", "Evento especial"
    };

    /** La lista completa que llega del servidor y la que se muestra ya filtrada. */
    private final List<ActivityItem> actividadesTodas = new ArrayList<>();
    private String categoriaSeleccionada;

    private Chip[] chipsDia;
    private Chip[] chipsFranja;
    /** La marcada en la grilla, a la espera del botón "Agregar". */
    private ActivityItem actividadSeleccionada;
    private final List<ActivityItem> actividades = new ArrayList<>();
    private ActivityAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityActivityPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api              = ApiClient.getInstance(this).getApi();
        childId          = getIntent().getStringExtra("child_id");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Agregar actividad");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        adapter = new ActivityAdapter(actividades, this);
        adapter.setMostrarDistintivoPredefinida(false);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter);

        chipsDia = new Chip[]{
                binding.chipLun, binding.chipMar, binding.chipMie, binding.chipJue,
                binding.chipVie, binding.chipSab, binding.chipDom
        };
        // Viene marcado el día desde el que se abrió la agenda; se pueden sumar otros.
        String diaDeOrigen = getIntent().getStringExtra("day");
        for (int i = 0; i < DIAS.length; i++) {
            chipsDia[i].setChecked(DIAS[i].equals(diaDeOrigen));
        }

        // Ninguna franja viene marcada: al admitir varias, dejar una puesta agregaba
        // una entrada a la mañana sin que el usuario la hubiera pedido.
        chipsFranja = new Chip[]{binding.chipManana, binding.chipTarde, binding.chipNoche};

        construirChipsDeCategoria();
        binding.etBuscar.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) { aplicarFiltro(); }
        });

        binding.btnAgregar.setOnClickListener(v -> confirmarAlta());

        cargarActividades();
    }

    private void cargarActividades() {
        binding.progressBar.setVisibility(View.VISIBLE);
        api.getActivities().enqueue(new Callback<List<ActivityItem>>() {
            @Override
            public void onResponse(Call<List<ActivityItem>> call, Response<List<ActivityItem>> resp) {
                binding.progressBar.setVisibility(View.GONE);
                if (resp.isSuccessful() && resp.body() != null) {
                    actividadesTodas.clear();
                    actividadesTodas.addAll(resp.body());
                    aplicarFiltro();
                } else {
                    Toast.makeText(ActivityPickerActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<ActivityItem>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(ActivityPickerActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void construirChipsDeCategoria() {
        for (int i = 0; i < CAT_KEYS.length; i++) {
            final String clave = CAT_KEYS[i];
            Chip chip = new Chip(this);
            chip.setText(CAT_LABELS[i]);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.primary_variant);
            chip.setOnCheckedChangeListener((btn, marcado) -> {
                categoriaSeleccionada = marcado ? clave : null;
                if (marcado) {
                    // El grupo es de selección única, pero se desmarca a mano por
                    // si alguien le saca esa propiedad al XML.
                    for (int j = 0; j < binding.chipGroupCategorias.getChildCount(); j++) {
                        View otro = binding.chipGroupCategorias.getChildAt(j);
                        if (otro instanceof Chip && otro != btn) ((Chip) otro).setChecked(false);
                    }
                }
                aplicarFiltro();
            });
            binding.chipGroupCategorias.addView(chip);
        }
    }

    /**
     * Deja visibles las que coinciden con el texto buscado y con la categoría.
     * Si la marcada deja de estar en pantalla se desmarca, para no agregar algo
     * que no se ve.
     */
    private void aplicarFiltro() {
        String texto = binding.etBuscar.getText().toString().trim().toLowerCase();

        actividades.clear();
        for (ActivityItem a : actividadesTodas) {
            boolean coincideCategoria = categoriaSeleccionada == null
                    || categoriaSeleccionada.equals(a.category);
            boolean coincideTexto = texto.isEmpty()
                    || (a.name != null && a.name.toLowerCase().contains(texto));
            if (coincideCategoria && coincideTexto) actividades.add(a);
        }
        adapter.notifyDataSetChanged();

        // Se compara por id y no con contains(): ActivityItem no define equals, así
        // que contains() compararía por referencia y dejaría de funcionar en cuanto
        // la lista se recargue del servidor.
        if (actividadSeleccionada != null && !estaVisible(actividadSeleccionada.id)) {
            actividadSeleccionada = null;
            adapter.setSeleccionada(null);
            binding.btnAgregar.setEnabled(false);
        }
    }

    private boolean estaVisible(String idActividad) {
        for (ActivityItem a : actividades) {
            if (a.id != null && a.id.equals(idActividad)) return true;
        }
        return false;
    }

    /** Días marcados, en el orden en que aparecen los chips. */
    private List<String> diasElegidos() {
        return marcados(chipsDia, DIAS);
    }

    /** Franjas marcadas, en el orden en que aparecen los chips. */
    private List<String> franjasElegidas() {
        return marcados(chipsFranja, FRANJAS);
    }

    private List<String> marcados(Chip[] chips, String[] valores) {
        List<String> elegidos = new ArrayList<>();
        for (int i = 0; i < valores.length; i++) {
            if (chips[i].isChecked()) elegidos.add(valores[i]);
        }
        return elegidos;
    }

    /** Los rótulos de los chips marcados, separados por coma. */
    private String rotulosMarcados(Chip[] chips) {
        StringBuilder sb = new StringBuilder();
        for (Chip chip : chips) {
            if (!chip.isChecked()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(chip.getText());
        }
        return sb.toString();
    }

    /** Deja a la vista qué se va a crear: los días, los momentos y cuántas entradas salen. */
    private String resumenDelAlta() {
        return resumen(R.plurals.resumen_alta);
    }

    private String resumen(int plural) {
        int cantidad = diasElegidos().size() * franjasElegidas().size();
        return getResources().getQuantityString(plural, cantidad,
                rotulosMarcados(chipsDia), rotulosMarcados(chipsFranja), cantidad);
    }

    /** Tocar una actividad sólo la marca; se agrega con el botón de abajo. */
    @Override
    public void onSelect(ActivityItem activity) {
        boolean yaEstaba = actividadSeleccionada != null
                && activity.id != null && activity.id.equals(actividadSeleccionada.id);
        actividadSeleccionada = yaEstaba ? null : activity;
        adapter.setSeleccionada(actividadSeleccionada != null ? actividadSeleccionada.id : null);
        binding.btnAgregar.setEnabled(actividadSeleccionada != null);
    }

    /** Pide los ajustes de la entrada y recién ahí da de alta. */
    private void confirmarAlta() {
        if (actividadSeleccionada == null) return;
        ActivityItem activity = actividadSeleccionada;

        if (diasElegidos().isEmpty()) {
            Toast.makeText(this, R.string.error_sin_dias, Toast.LENGTH_SHORT).show();
            return;
        }
        if (franjasElegidas().isEmpty()) {
            Toast.makeText(this, R.string.error_sin_franjas, Toast.LENGTH_SHORT).show();
            return;
        }
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
                .setMessage(resumenDelAlta())
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
        List<String> dias    = diasElegidos();
        List<String> franjas = franjasElegidas();

        // Una entrada por cada combinación de día y franja marcados.
        AtomicInteger pendientes = new AtomicInteger(dias.size() * franjas.size());
        AtomicBoolean huboError  = new AtomicBoolean(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        for (String dia : dias) {
            for (String franja : franjas) {
                ScheduleEntryRequest req = new ScheduleEntryRequest();
                req.activityId = activity.id;
                req.dayOfWeek  = dia;
                req.timeSlot   = franja;
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
    }

    private void alTerminarAlta(AtomicInteger pendientes, AtomicBoolean huboError) {
        if (pendientes.decrementAndGet() > 0) return;
        binding.progressBar.setVisibility(View.GONE);
        if (huboError.get()) {
            Toast.makeText(this, "Error al agregar actividad.", Toast.LENGTH_SHORT).show();
        } else {
            // La entrada nueva va al final de su franja, que puede quedar fuera de la
            // pantalla: sin este aviso no hay forma de saber que el alta salió bien.
            Toast.makeText(this, resumen(R.plurals.alta_hecha), Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
