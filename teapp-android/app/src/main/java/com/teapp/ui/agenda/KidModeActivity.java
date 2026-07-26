package com.teapp.ui.agenda;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityKidModeBinding;
import com.teapp.model.ActivityStep;
import com.teapp.model.Child;
import com.teapp.model.CompletionRequest;
import com.teapp.model.ScheduleEntry;
import com.teapp.model.WeeklySchedule;
import com.teapp.util.Constants;
import com.teapp.model.LoginRequest;
import com.teapp.model.AuthResponse;
import com.teapp.util.PrefsManager;
import com.teapp.util.WeatherHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KidModeActivity extends AppCompatActivity {

    private static final String[] FRANJAS = {"MORNING", "AFTERNOON", "NIGHT"};
    private static final int REQ_LOCATION = 100;

    private ActivityKidModeBinding binding;
    private ApiService api;
    private String childId;
    private Child child;
    private TextToSpeech tts;
    private CountDownTimer temporizador;
    private long tiempoRestanteMs = 0;
    private long duracionTotalMs  = 0;
    private boolean pausado       = false;

    private List<ScheduleEntry> entradasHoy = new ArrayList<>();
    private int indiceCurrent = 0;

    // Adapters para las tres franjas
    private KidEntryAdapter adapterManana, adapterTarde, adapterNoche;
    private final List<ScheduleEntry> listaManana   = new ArrayList<>();
    private final List<ScheduleEntry> listaTarde    = new ArrayList<>();
    private final List<ScheduleEntry> listaNoche    = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityKidModeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        api     = ApiClient.getInstance(this).getApi();
        childId = getIntent().getStringExtra(Constants.EXTRA_CHILD_ID);
        child   = (Child) getIntent().getSerializableExtra(Constants.EXTRA_CHILD);

        // Header
        if (child != null) {
            binding.tvKidNombre.setText("¡Hola, " + child.name + "! 🌟");
            binding.tvKidInitials.setText(child.getInitials());
            try {
                binding.tvKidInitials.setBackgroundColor(
                        Color.parseColor(child.avatarColor != null ? child.avatarColor : "#A8D8EA"));
            } catch (Exception ignored) {}
        }

        // Adapters por franja
        adapterManana = new KidEntryAdapter(listaManana, this::onEntradaTapped);
        adapterTarde  = new KidEntryAdapter(listaTarde,  this::onEntradaTapped);
        adapterNoche  = new KidEntryAdapter(listaNoche,  this::onEntradaTapped);

        binding.rvMananaKid.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTardeKid.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNocheKid.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMananaKid.setAdapter(adapterManana);
        binding.rvTardeKid.setAdapter(adapterTarde);
        binding.rvNocheKid.setAdapter(adapterNoche);

        // Card Ahora tappable
        binding.cardAhora.setOnClickListener(v -> {
            if (indiceCurrent < entradasHoy.size())
                onEntradaTapped(entradasHoy.get(indiceCurrent));
        });

        binding.btnPausar.setOnClickListener(v -> togglePausa());
        binding.btnSalir.setOnClickListener(v -> pedirContrasenaSalida());

        inicializarTTS();
        cargarClima();
        cargarAgenda();
    }

    // Entrada tapeada → completar/descompletar

    private void onEntradaTapped(ScheduleEntry entry) {
        if (entry.isCompletedToday()) {
            api.unmarkCompleted(childId, entry.id, new CompletionRequest(ScheduleEntry.todayIso()))
                    .enqueue(new Callback<Void>() {
                        @Override public void onResponse(Call<Void> c, Response<Void> r) { cargarAgenda(); }
                        @Override public void onFailure(Call<Void> c, Throwable t) {}
                    });
            return;
        }
        if (entry.activity != null && entry.activity.stepCount != null && entry.activity.stepCount > 0) {
            mostrarPasos(entry);
        } else {
            marcarCompletado(entry);
        }
    }

    // Cargar agenda

    private void cargarAgenda() {
        binding.progressBar.setVisibility(View.VISIBLE);
        api.getSchedule(childId).enqueue(new Callback<WeeklySchedule>() {
            @Override
            public void onResponse(Call<WeeklySchedule> call, Response<WeeklySchedule> resp) {
                binding.progressBar.setVisibility(View.GONE);
                if (resp.isSuccessful() && resp.body() != null) procesarAgenda(resp.body());
            }
            @Override
            public void onFailure(Call<WeeklySchedule> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(KidModeActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void procesarAgenda(WeeklySchedule agenda) {
        String diaHoy = diaHoyApi();
        entradasHoy.clear();
        listaManana.clear(); listaTarde.clear(); listaNoche.clear();

        if (agenda.week != null && agenda.week.containsKey(diaHoy)) {
            Map<String, List<ScheduleEntry>> slots = agenda.week.get(diaHoy);
            if (slots != null) {
                if (slots.containsKey("MORNING")   && slots.get("MORNING")   != null) { listaManana.addAll(slots.get("MORNING")); entradasHoy.addAll(listaManana); }
                if (slots.containsKey("AFTERNOON") && slots.get("AFTERNOON") != null) { listaTarde.addAll(slots.get("AFTERNOON")); entradasHoy.addAll(listaTarde); }
                if (slots.containsKey("NIGHT")     && slots.get("NIGHT")     != null) { listaNoche.addAll(slots.get("NIGHT")); entradasHoy.addAll(listaNoche); }
            }
        }

        adapterManana.notifyDataSetChanged();
        adapterTarde.notifyDataSetChanged();
        adapterNoche.notifyDataSetChanged();

        // Calcular índice de la primera actividad pendiente
        indiceCurrent = 0;
        for (int i = 0; i < entradasHoy.size(); i++) {
            if (!entradasHoy.get(i).isCompletedToday()) { indiceCurrent = i; break; }
            if (i == entradasHoy.size() - 1) indiceCurrent = entradasHoy.size();
        }
        actualizarAhoraDespues();
    }

    private void actualizarAhoraDespues() {
        detenerTemporizador();
        pausado = false;
        binding.btnPausar.setText(R.string.pausar);

        if (entradasHoy.isEmpty() || indiceCurrent >= entradasHoy.size()) {
            binding.tvAhora.setText("Ahora");
            binding.tvNombreAhora.setText("¡Todo listo por hoy! 🎉");
            binding.imgAhora.setVisibility(View.GONE);
            binding.ivCompletadoBadge.setVisibility(View.GONE);
            binding.cardAhora.setCardBackgroundColor(Color.parseColor("#F8F9FA"));
            binding.layoutTimer.setVisibility(View.GONE);
            binding.tvNombreDespues.setText("—");
            binding.imgDespues.setVisibility(View.GONE);
            return;
        }

        ScheduleEntry ahora = entradasHoy.get(indiceCurrent);

        // Cumpleaños
        if (esCumpleanos()) binding.tvAhora.setText("🎂 ¡Feliz cumpleaños!");
        else binding.tvAhora.setText("Ahora");

        binding.tvNombreAhora.setText(ahora.activity != null ? ahora.activity.name : "—");

        // Color de fondo de la card Ahora
        if (ahora.activity != null && ahora.activity.color != null) {
            try { binding.cardAhora.setCardBackgroundColor(Color.parseColor(ahora.activity.color + "33")); }
            catch (Exception ignored) { binding.cardAhora.setCardBackgroundColor(Color.WHITE); }
        } else {
            binding.cardAhora.setCardBackgroundColor(Color.WHITE);
        }

        // Pictograma
        String imgUrl = ahora.activity != null ? (ahora.activity.pictogramUrl != null ? ahora.activity.pictogramUrl : ahora.activity.imageBase64) : null;
        if (imgUrl != null) {
            Glide.with(this).load(imgUrl).into(binding.imgAhora);
            binding.imgAhora.setVisibility(View.VISIBLE);
        } else {
            binding.imgAhora.setVisibility(View.GONE);
        }

        binding.ivCompletadoBadge.setVisibility(ahora.isCompletedToday() ? View.VISIBLE : View.GONE);

        if (ahora.activity != null) hablar(ahora.activity.name);

        // Timer
        if (ahora.durationMinutes != null && ahora.durationMinutes > 0 && !ahora.isCompletedToday()) {
            duracionTotalMs = ahora.durationMinutes * 60 * 1000L;
            iniciarTemporizador(duracionTotalMs);
        } else {
            binding.layoutTimer.setVisibility(View.GONE);
        }

        // Siguiente
        int sig = indiceCurrent + 1;
        while (sig < entradasHoy.size() && entradasHoy.get(sig).isCompletedToday()) sig++;
        if (sig < entradasHoy.size()) {
            ScheduleEntry despues = entradasHoy.get(sig);
            binding.tvNombreDespues.setText(despues.activity != null ? despues.activity.name : "—");
            String dUrl = despues.activity != null ? (despues.activity.pictogramUrl != null ? despues.activity.pictogramUrl : despues.activity.imageBase64) : null;
            if (dUrl != null) {
                Glide.with(this).load(dUrl).into(binding.imgDespues);
                binding.imgDespues.setVisibility(View.VISIBLE);
            } else {
                binding.imgDespues.setVisibility(View.GONE);
            }
        } else {
            binding.tvNombreDespues.setText("¡Todo listo!");
            binding.imgDespues.setVisibility(View.GONE);
        }
    }

    // Marcar completado

    private void mostrarPasos(ScheduleEntry entry) {
        api.getSteps(entry.activity.id).enqueue(new Callback<List<ActivityStep>>() {
            @Override
            public void onResponse(Call<List<ActivityStep>> call, Response<List<ActivityStep>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    View view = LayoutInflater.from(KidModeActivity.this).inflate(R.layout.dialog_steps, null);
                    RecyclerView rv = view.findViewById(R.id.rv_pasos);
                    ((TextView) view.findViewById(R.id.tv_titulo_actividad)).setText(entry.activity != null ? entry.activity.name : "");
                    rv.setLayoutManager(new LinearLayoutManager(KidModeActivity.this));
                    rv.setAdapter(new StepsAdapter(response.body()));
                    new AlertDialog.Builder(KidModeActivity.this)
                            .setView(view)
                            .setPositiveButton("Marcar completado", (d, w) -> marcarCompletado(entry))
                            .setNegativeButton(R.string.cancelar, null)
                            .show();
                } else {
                    marcarCompletado(entry);
                }
            }
            @Override public void onFailure(Call<List<ActivityStep>> call, Throwable t) { marcarCompletado(entry); }
        });
    }

    private void marcarCompletado(ScheduleEntry entry) {
        detenerTemporizador();
        api.markCompleted(childId, entry.id, new CompletionRequest(ScheduleEntry.todayIso()))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            // Avanzar al siguiente pendiente
                            indiceCurrent++;
                            while (indiceCurrent < entradasHoy.size() && entradasHoy.get(indiceCurrent).isCompletedToday())
                                indiceCurrent++;
                            cargarAgenda();
                        }
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(KidModeActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Timer

    private void togglePausa() {
        if (!pausado) {
            pausado = true;
            if (temporizador != null) { temporizador.cancel(); temporizador = null; }
            binding.btnPausar.setText(R.string.reanudar);
        } else {
            pausado = false;
            binding.btnPausar.setText(R.string.pausar);
            iniciarTemporizador(tiempoRestanteMs);
        }
    }

    private void iniciarTemporizador(long millis) {
        tiempoRestanteMs = millis;
        binding.layoutTimer.setVisibility(View.VISIBLE);
        temporizador = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long ms) {
                tiempoRestanteMs = ms;
                long s = ms / 1000; long m = s / 60; s = s % 60;
                binding.tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));
                binding.progressTimer.setMax((int)(duracionTotalMs / 1000));
                binding.progressTimer.setProgress((int)(ms / 1000));
            }
            @Override public void onFinish() { tiempoRestanteMs = 0; binding.tvTimer.setText("00:00"); hablar("¡Tiempo!"); }
        }.start();
    }

    private void detenerTemporizador() {
        if (temporizador != null) { temporizador.cancel(); temporizador = null; }
    }

    // Password para salir

    private void pedirContrasenaSalida() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_exit_kid_mode, null);
        com.google.android.material.textfield.TextInputEditText etPass = view.findViewById(R.id.et_contrasena_salida);
        TextView tvError = view.findViewById(R.id.tv_error_salida);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.verificar_identidad)
                .setMessage(R.string.contrasena_para_salir)
                .setView(view)
                .setPositiveButton(R.string.verificar, null)
                .setNegativeButton(R.string.cancelar, null)
                .create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String pass = etPass.getText().toString().trim();
                if (pass.isEmpty()) return;
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                tvError.setVisibility(View.GONE);
                PrefsManager prefs = new PrefsManager(KidModeActivity.this);
                api.login(new LoginRequest(prefs.getUserEmail(), pass)).enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        if (response.isSuccessful()) { dialog.dismiss(); finish(); }
                        else {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            tvError.setText(R.string.contrasena_incorrecta);
                            tvError.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        tvError.setText(R.string.error_red);
                        tvError.setVisibility(View.VISIBLE);
                    }
                });
            });
        });
        dialog.show();
    }

    // TTS

    private void inicializarTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("es", "AR"));
                tts.setSpeechRate(0.85f);
            }
        });
    }

    private void hablar(String texto) {
        if (tts != null) tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // Clima

    private void cargarClima() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }
        obtenerUbicacionYClima();
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(req, permissions, results);
        if (req == REQ_LOCATION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
            obtenerUbicacionYClima();
    }

    private void obtenerUbicacionYClima() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm == null) return;
            Location loc = null;
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                final double lat = loc.getLatitude(), lon = loc.getLongitude();
                WeatherHelper.fetch(lat, lon, new WeatherHelper.WeatherCallback() {
                    @Override
                    public void onResult(String emoji, String label, int tempC) {
                        binding.tvClima.setText(emoji + " " + tempC + "°C\n" + label);
                        binding.layoutClima.setVisibility(View.VISIBLE);
                    }
                    @Override public void onError() {}
                });
            }
        } catch (SecurityException ignored) {}
    }

    // Utilidades

    private boolean esCumpleanos() {
        if (child == null || child.dateOfBirth == null || child.dateOfBirth.isEmpty()) return false;
        try {
            String[] p = child.dateOfBirth.split("-");
            Calendar hoy = Calendar.getInstance();
            return hoy.get(Calendar.MONTH) + 1 == Integer.parseInt(p[1])
                    && hoy.get(Calendar.DAY_OF_MONTH) == Integer.parseInt(p[2]);
        } catch (Exception e) { return false; }
    }

    private String diaHoyApi() {
        switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:    return "MONDAY";
            case Calendar.TUESDAY:   return "TUESDAY";
            case Calendar.WEDNESDAY: return "WEDNESDAY";
            case Calendar.THURSDAY:  return "THURSDAY";
            case Calendar.FRIDAY:    return "FRIDAY";
            case Calendar.SATURDAY:  return "SATURDAY";
            default:                 return "SUNDAY";
        }
    }

    @Override
    protected void onDestroy() {
        detenerTemporizador();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }

    // Adapter de entradas para modo niño

    interface OnEntradaTap { void tap(ScheduleEntry e); }

    static class KidEntryAdapter extends RecyclerView.Adapter<KidEntryAdapter.VH> {
        private final List<ScheduleEntry> items;
        private final OnEntradaTap listener;
        KidEntryAdapter(List<ScheduleEntry> items, OnEntradaTap listener) {
            this.items = items; this.listener = listener;
        }
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kid_entry, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(VH h, int pos) {
            ScheduleEntry e = items.get(pos);
            h.tvName.setText(e.activity != null ? e.activity.name : "—");

            // Color de fondo del ícono
            if (e.activity != null && e.activity.color != null) {
                try { h.frameIcon.setBackgroundColor(Color.parseColor(e.activity.color + "55")); }
                catch (Exception ignored) { h.frameIcon.setBackgroundColor(Color.parseColor("#A8D8EA55")); }
            }

            // Pictograma
            String imgUrl = e.activity != null ? (e.activity.pictogramUrl != null ? e.activity.pictogramUrl : e.activity.imageBase64) : null;
            if (imgUrl != null) {
                Glide.with(h.imgPictogram.getContext()).load(imgUrl).into(h.imgPictogram);
                h.imgPictogram.setVisibility(View.VISIBLE);
            } else {
                h.imgPictogram.setVisibility(View.GONE);
            }

            // Check
            h.ivCheck.setImageResource(e.isCompletedToday()
                    ? android.R.drawable.checkbox_on_background
                    : android.R.drawable.checkbox_off_background);

            // Opacidad si completada
            h.itemView.setAlpha(e.isCompletedToday() ? 0.55f : 1.0f);

            h.itemView.setOnClickListener(v -> listener.tap(e));
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvName; FrameLayout frameIcon; ImageView imgPictogram, ivCheck;
            VH(View v) {
                super(v);
                tvName       = v.findViewById(R.id.tv_name);
                frameIcon    = v.findViewById(R.id.frame_icon);
                imgPictogram = v.findViewById(R.id.img_pictogram);
                ivCheck      = v.findViewById(R.id.iv_check);
            }
        }
    }

    // Steps adapter

    static class StepsAdapter extends RecyclerView.Adapter<StepsAdapter.VH> {
        private final List<ActivityStep> steps;
        StepsAdapter(List<ActivityStep> steps) { this.steps = steps; }
        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_step, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(VH h, int pos) {
            ActivityStep step = steps.get(pos);
            h.tvNumero.setText(String.valueOf(step.stepOrder));
            h.tvTitulo.setText(step.title);
            if (step.description != null && !step.description.isEmpty()) {
                h.tvDescripcion.setText(step.description);
                h.tvDescripcion.setVisibility(View.VISIBLE);
            }
            String imgUrl = step.pictogramUrl != null ? step.pictogramUrl : step.imageBase64;
            if (imgUrl != null) {
                Glide.with(h.imgStep.getContext()).load(imgUrl).into(h.imgStep);
                h.imgStep.setVisibility(View.VISIBLE);
            }
        }
        @Override public int getItemCount() { return steps.size(); }
        static class VH extends RecyclerView.ViewHolder {
            TextView tvNumero, tvTitulo, tvDescripcion; ImageView imgStep;
            VH(View v) {
                super(v);
                tvNumero = v.findViewById(R.id.tv_numero);
                tvTitulo = v.findViewById(R.id.tv_titulo);
                tvDescripcion = v.findViewById(R.id.tv_descripcion);
                imgStep = v.findViewById(R.id.img_step);
            }
        }
    }
}
