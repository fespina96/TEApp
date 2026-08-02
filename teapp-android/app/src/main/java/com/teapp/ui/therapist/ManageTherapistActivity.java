package com.teapp.ui.therapist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityManageTherapistBinding;
import com.teapp.model.TherapistInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageTherapistActivity extends AppCompatActivity {

    private ActivityManageTherapistBinding binding;
    private ApiService api;
    private final List<TherapistInfo> terapeutas = new ArrayList<>();
    private TherapistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManageTherapistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.mis_terapeutas);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        api = ApiClient.getInstance(this).getApi();

        adapter = new TherapistAdapter(terapeutas, this::desvincular);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        binding.btnVincular.setOnClickListener(v -> vincular());
        cargarTerapeutas();
    }

    private void cargarTerapeutas() {
        binding.progressBar.setVisibility(View.VISIBLE);
        api.getMyTherapists().enqueue(new Callback<List<TherapistInfo>>() {
            @Override
            public void onResponse(Call<List<TherapistInfo>> call, Response<List<TherapistInfo>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    terapeutas.clear();
                    terapeutas.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    binding.tvVacio.setVisibility(terapeutas.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override
            public void onFailure(Call<List<TherapistInfo>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageTherapistActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void vincular() {
        String codigo = binding.etCodigo.getText().toString().trim().toUpperCase();
        if (codigo.isEmpty()) {
            binding.tilCodigo.setError("Ingresá el código");
            return;
        }
        binding.tilCodigo.setError(null);
        binding.progressBar.setVisibility(View.VISIBLE);

        Map<String, String> body = new HashMap<>();
        body.put("inviteCode", codigo);

        api.linkTherapist(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    binding.etCodigo.setText("");
                    Toast.makeText(ManageTherapistActivity.this,
                            "Terapeuta vinculado correctamente.", Toast.LENGTH_SHORT).show();
                    cargarTerapeutas();
                } else if (response.code() == 400) {
                    binding.tilCodigo.setError("Código no encontrado");
                } else {
                    Toast.makeText(ManageTherapistActivity.this,
                            "Error al vincular.", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(ManageTherapistActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void desvincular(TherapistInfo t) {
        api.unlinkTherapist(t.id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ManageTherapistActivity.this,
                            "Terapeuta desvinculado.", Toast.LENGTH_SHORT).show();
                    cargarTerapeutas();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t2) {
                Toast.makeText(ManageTherapistActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }

    // Adapter

    interface OnDesvinculo { void desvincular(TherapistInfo t); }

    static class TherapistAdapter extends RecyclerView.Adapter<TherapistAdapter.VH> {
        private final List<TherapistInfo> items;
        private final OnDesvinculo listener;

        TherapistAdapter(List<TherapistInfo> items, OnDesvinculo listener) {
            this.items    = items;
            this.listener = listener;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_therapist, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            TherapistInfo t = items.get(pos);
            h.tvNombre.setText(t.fullName);
            h.tvEmail.setText(t.email);
            h.btnDesvincular.setOnClickListener(v -> listener.desvincular(t));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvNombre, tvEmail;
            android.widget.Button btnDesvincular;
            VH(View v) {
                super(v);
                tvNombre      = v.findViewById(R.id.tv_nombre);
                tvEmail       = v.findViewById(R.id.tv_email);
                btnDesvincular = v.findViewById(R.id.btn_desvincular);
            }
        }
    }
}
