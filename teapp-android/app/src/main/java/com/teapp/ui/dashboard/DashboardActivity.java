package com.teapp.ui.dashboard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityDashboardBinding;
import com.teapp.model.Child;
import com.teapp.ui.activity.ActivityCatalogActivity;
import com.teapp.ui.adapter.ChildAdapter;
import com.teapp.ui.agenda.AgendaActivity;
import com.teapp.model.TherapistInfo;
import com.teapp.ui.auth.LoginActivity;
import com.teapp.ui.child.ChildFormActivity;
import com.teapp.ui.profile.ProfileActivity;
import com.teapp.ui.therapist.ManageTherapistActivity;
import com.teapp.util.ConnectionChecker;
import com.teapp.util.Constants;
import com.teapp.util.AvatarUtils;
import com.teapp.util.PrefsManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity implements ChildAdapter.Listener {

    private static final int REQ_AVATAR = 400;

    private ActivityDashboardBinding binding;
    private PrefsManager prefs;
    private ApiService api;
    private final List<Child> participantes = new ArrayList<>();
    private ChildAdapter adapter;
    private TerapeutaAdapter terapeutaAdapter;
    private final List<TherapistInfo> terapeutas = new ArrayList<>();
    private ConnectionChecker connectionChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        prefs = new PrefsManager(this);
        api   = ApiClient.getInstance(this).getApi();
        connectionChecker = new ConnectionChecker(this);

        String nombre = prefs.getUserName();
        binding.tvUsuario.setText("¡Hola, " + (nombre.isEmpty() ? "!" : nombre + "!"));
        mostrarAvatarUsuario(prefs.getUserAvatar(), nombre);

        adapter = new ChildAdapter(participantes, this);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter);

        binding.btnNuevoParticipante.setOnClickListener(v ->
                startActivity(new Intent(this, ChildFormActivity.class)));


        binding.btnMenu.setOnClickListener(v -> mostrarMenu(v));
        binding.btnVincularTerapeuta.setOnClickListener(v ->
                startActivity(new Intent(this, ManageTherapistActivity.class)));

        // Adapter terapeutas
        terapeutaAdapter = new TerapeutaAdapter(terapeutas, this::confirmarDesvinculacion);
        binding.rvTerapeutas.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTerapeutas.setAdapter(terapeutaAdapter);

        binding.swipeRefresh.setOnRefreshListener(this::cargarTodo);
    }

    private void mostrarMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_dashboard, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_actividades) {
                startActivity(new Intent(this, ActivityCatalogActivity.class));
            } else if (id == R.id.action_perfil) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.action_logout) {
                cerrarSesion();
            }
            return true;
        });
        popup.show();
    }

    private void abrirGaleriaAvatar() {
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setType("image/*");
        startActivityForResult(pick, REQ_AVATAR);
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectionChecker.check();
        cargarTodo();
    }

    private void cargarTodo() {
        cargarParticipantes();
        cargarTerapeutas();
    }

    private void confirmarDesvinculacion(TherapistInfo terapeuta) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.desvincular_terapeuta)
                .setMessage(getString(R.string.confirmar_desvincular, terapeuta.fullName))
                .setPositiveButton(R.string.desvincular, (d, w) ->
                        api.unlinkTherapist(terapeuta.id).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(DashboardActivity.this,
                                            R.string.terapeuta_desvinculado, Toast.LENGTH_SHORT).show();
                                    cargarTerapeutas();
                                } else {
                                    Toast.makeText(DashboardActivity.this,
                                            R.string.error_red, Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(DashboardActivity.this,
                                        R.string.error_red, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void cargarTerapeutas() {
        api.getMyTherapists().enqueue(new Callback<List<TherapistInfo>>() {
            @Override
            public void onResponse(Call<List<TherapistInfo>> call, Response<List<TherapistInfo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    terapeutas.clear();
                    terapeutas.addAll(response.body());
                    terapeutaAdapter.notifyDataSetChanged();
                    binding.tvSinTerapeutas.setVisibility(terapeutas.isEmpty() ? View.VISIBLE : View.GONE);
                    // Sólo se admite un terapeuta a la vez: mientras haya uno vinculado
                    // se oculta la opción de vincular, hasta que se desvincule.
                    binding.btnVincularTerapeuta.setVisibility(terapeutas.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void onFailure(Call<List<TherapistInfo>> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_AVATAR && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            try {
                InputStream is = getContentResolver().openInputStream(data.getData());
                Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                float r = 512f / Math.max(bmp.getWidth(), bmp.getHeight());
                if (r < 1) bmp = Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth()*r), (int)(bmp.getHeight()*r), true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                String b64 = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                prefs.saveUserAvatar(b64);
                mostrarAvatarUsuario(b64, prefs.getUserName());
                RequestBody body = RequestBody.create(MediaType.parse("text/plain"), b64);
                api.updateUserAvatar(body).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> r2) {
                        if (r2.isSuccessful())
                            Toast.makeText(DashboardActivity.this, "Foto actualizada.", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(DashboardActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Toast.makeText(this, "Error al cargar la imagen.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cargarParticipantes() {
        binding.swipeRefresh.setRefreshing(true);
        api.getChildren().enqueue(new Callback<List<Child>>() {
            @Override
            public void onResponse(Call<List<Child>> call, Response<List<Child>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    participantes.clear();
                    participantes.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    binding.tvVacio.setVisibility(participantes.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
            @Override
            public void onFailure(Call<List<Child>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(DashboardActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAgenda(Child child) {
        Intent intent = new Intent(this, AgendaActivity.class);
        intent.putExtra(Constants.EXTRA_CHILD_ID, child.id);
        intent.putExtra(Constants.EXTRA_CHILD_NAME, child.name);
        intent.putExtra(Constants.EXTRA_CHILD, child);
        startActivity(intent);
    }

    @Override
    public void onEdit(Child child) {
        Intent intent = new Intent(this, ChildFormActivity.class);
        intent.putExtra(Constants.EXTRA_CHILD, child);
        startActivity(intent);
    }

    @Override
    public void onDelete(Child child) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar participante")
                .setMessage("¿Eliminar a " + child.name + "? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> eliminarParticipante(child))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void eliminarParticipante(Child child) {
        api.deleteChild(child.id).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> r) { if (r.isSuccessful()) cargarParticipantes(); }
            @Override public void onFailure(Call<Void> call, Throwable t) { Toast.makeText(DashboardActivity.this, R.string.error_red, Toast.LENGTH_SHORT).show(); }
        });
    }

    private void cerrarSesion() {
        prefs.clear();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    static class TerapeutaAdapter extends RecyclerView.Adapter<TerapeutaAdapter.VH> {
        interface Listener { void onDesvincular(TherapistInfo terapeuta); }

        private final List<TherapistInfo> items;
        private final Listener listener;
        TerapeutaAdapter(List<TherapistInfo> items, Listener listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_therapist, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            TherapistInfo t = items.get(pos);
            h.tvNombre.setText(t.fullName);
            h.tvEmail.setText(t.email);
            h.btnDesvincular.setVisibility(android.view.View.VISIBLE);
            h.btnDesvincular.setOnClickListener(v -> listener.onDesvincular(t));
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.TextView tvNombre, tvEmail;
            android.widget.Button btnDesvincular;
            VH(android.view.View v) {
                super(v);
                tvNombre      = v.findViewById(R.id.tv_nombre);
                tvEmail       = v.findViewById(R.id.tv_email);
                btnDesvincular = v.findViewById(R.id.btn_desvincular);
            }
        }
    }

    /** Emoji del catálogo, foto subida o la inicial del nombre, en ese orden. */
    private void mostrarAvatarUsuario(String avatarBase64, String nombre) {
        String inicial = nombre != null && !nombre.isEmpty()
                ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?";
        AvatarUtils.mostrarAvatar(binding.tvInicialesUsuario, binding.imgAvatarUsuario,
                avatarBase64, "#A8D8EA", inicial);
    }
}
