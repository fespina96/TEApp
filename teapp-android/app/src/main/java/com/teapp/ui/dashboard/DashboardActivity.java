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

import com.bumptech.glide.Glide;
import com.teapp.R;
import com.teapp.api.ApiClient;
import com.teapp.api.ApiService;
import com.teapp.databinding.ActivityDashboardBinding;
import com.teapp.model.Child;
import com.teapp.ui.activity.ActivityCatalogActivity;
import com.teapp.ui.adapter.ChildAdapter;
import com.teapp.ui.agenda.AgendaActivity;
import com.teapp.ui.auth.LoginActivity;
import com.teapp.ui.child.ChildFormActivity;
import com.teapp.ui.profile.ProfileActivity;
import com.teapp.util.Constants;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        prefs = new PrefsManager(this);
        api   = ApiClient.getInstance(this).getApi();

        String nombre = prefs.getUserName();
        binding.tvUsuario.setText("¡Hola, " + (nombre.isEmpty() ? "!" : nombre + "! 👋"));

        adapter = new ChildAdapter(participantes, this);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter);

        binding.fabAgregar.setOnClickListener(v ->
                startActivity(new Intent(this, ChildFormActivity.class)));


        // Menú popup (igual que mat-menu de Angular)
        binding.btnMenu.setOnClickListener(v -> mostrarMenu(v));

        binding.swipeRefresh.setOnRefreshListener(this::cargarParticipantes);
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
        cargarParticipantes();
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
                Glide.with(this).load(b64).circleCrop().into(binding.imgAvatarUsuario);
                String json = "{\"avatarBase64\":\"" + b64 + "\"}";
                RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
                api.updateUserAvatar(body).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> r2) {
                        if (r2.isSuccessful())
                            Toast.makeText(DashboardActivity.this, "Foto actualizada.", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
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
}
