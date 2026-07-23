package com.teapp.ui.therapist;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityTherapistDashboardBinding;
import com.teapp.model.Child;
import com.teapp.ui.auth.LoginActivity;
import com.teapp.ui.profile.ProfileActivity;
import com.teapp.util.ConnectionChecker;
import com.teapp.util.PrefsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TherapistDashboardActivity extends AppCompatActivity {

    private ActivityTherapistDashboardBinding binding;
    private ApiService api;
    private PrefsManager prefs;

    private final List<Map<String, Object>> padres = new ArrayList<>();
    private final List<String> nombresPadres = new ArrayList<>();
    private int padreSeleccionadoIdx = -1;

    private final List<Child> participantesActuales = new ArrayList<>();
    private ChildAdapter childAdapter;
    private ConnectionChecker connectionChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTherapistDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = new PrefsManager(this);
        api   = ApiClient.getInstance(this).getApi();
        connectionChecker = new ConnectionChecker(this);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Header
        String nombre = prefs.getUserName();
        binding.tvUsuario.setText(nombre);
        String inicial = (!nombre.isEmpty()) ? String.valueOf(nombre.charAt(0)).toUpperCase() : "T";
        binding.tvInitials.setText(inicial);
        try { binding.tvInitials.setBackgroundColor(Color.parseColor("#C9B8E8")); } catch (Exception ignored) {}

        // Código de invitación
        String codigo = prefs.getInviteCode();
        binding.tvCodigo.setText(codigo != null ? codigo : "—");

        // Botón copiar (igual que Angular)
        binding.btnCopiarCodigo.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("codigo", codigo));
            Toast.makeText(this, "Código copiado", Toast.LENGTH_SHORT).show();
        });

        // Perfil y logout
        binding.btnPerfil.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        binding.btnLogout.setOnClickListener(v -> {
            prefs.clear();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        childAdapter = new ChildAdapter(participantesActuales, child -> {
            if (padreSeleccionadoIdx < 0 || padreSeleccionadoIdx >= padres.size()) return;
            Object padreId = padres.get(padreSeleccionadoIdx).get("id");
            if (padreId == null) return;
            Intent intent = new Intent(this, SupervisedAgendaActivity.class);
            intent.putExtra(SupervisedAgendaActivity.EXTRA_PARENT_ID, padreId.toString());
            intent.putExtra(SupervisedAgendaActivity.EXTRA_CHILD_ID, child.id);
            intent.putExtra(SupervisedAgendaActivity.EXTRA_CHILD_NAME, child.name);
            startActivity(intent);
        });

        binding.rvParticipantes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvParticipantes.setAdapter(childAdapter);

        binding.btnLogout.setOnClickListener(v -> {
            prefs.clear();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        connectionChecker.check();
        cargarPadres();
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionChecker.check();
    }

    private void cargarPadres() {
        binding.progressBar.setVisibility(View.VISIBLE);
        api.getSupervisedParents().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call,
                                   Response<List<Map<String, Object>>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    padres.clear();
                    padres.addAll(response.body());
                    nombresPadres.clear();
                    for (Map<String, Object> p : padres) {
                        Object fn = p.get("fullName");
                        nombresPadres.add(fn != null ? fn.toString() : "—");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            TherapistDashboardActivity.this,
                            android.R.layout.simple_spinner_item, nombresPadres);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.spinnerPadres.setAdapter(adapter);

                    binding.tvVacio.setVisibility(padres.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.spinnerPadres.setVisibility(padres.isEmpty() ? View.GONE : View.VISIBLE);

                    binding.spinnerPadres.setOnItemSelectedListener(
                            new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> av, View v, int pos, long id) {
                                    padreSeleccionadoIdx = pos;
                                    cargarParticipantes(padres.get(pos));
                                }
                                @Override
                                public void onNothingSelected(AdapterView<?> av) {}
                            });
                }
            }
            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(TherapistDashboardActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarParticipantes(Map<String, Object> padre) {
        Object idObj = padre.get("id");
        if (idObj == null) return;

        api.getSupervisedChildren(idObj.toString()).enqueue(new Callback<List<Child>>() {
            @Override
            public void onResponse(Call<List<Child>> call, Response<List<Child>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    participantesActuales.clear();
                    participantesActuales.addAll(response.body());
                    childAdapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(Call<List<Child>> call, Throwable t) {}
        });
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    interface OnChildClick { void onClick(Child child); }

    static class ChildAdapter extends RecyclerView.Adapter<ChildAdapter.VH> {
        private final List<Child> items;
        private final OnChildClick listener;

        ChildAdapter(List<Child> items, OnChildClick listener) {
            this.items    = items;
            this.listener = listener;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_child, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            Child child = items.get(pos);
            h.tvName.setText(child.name);
            String inicial = child.name != null && !child.name.isEmpty()
                    ? String.valueOf(child.name.charAt(0)).toUpperCase() : "?";
            h.tvInitials.setText(inicial);
            h.btnAgenda.setOnClickListener(v -> listener.onClick(child));
            h.itemView.setOnClickListener(v -> listener.onClick(child));
            h.btnEdit.setVisibility(View.GONE);
            h.btnDelete.setVisibility(View.GONE);
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvInitials;
            android.widget.ImageButton btnAgenda, btnEdit, btnDelete;
            VH(View v) {
                super(v);
                tvName     = v.findViewById(R.id.tv_name);
                tvInitials = v.findViewById(R.id.tv_initials);
                btnAgenda  = v.findViewById(R.id.btn_agenda);
                btnEdit    = v.findViewById(R.id.btn_edit);
                btnDelete  = v.findViewById(R.id.btn_delete);
            }
        }
    }
}
