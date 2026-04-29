package com.teapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.teapp.ui.dashboard.DashboardActivity;
import com.teapp.ui.therapist.TherapistDashboardActivity;
import com.teapp.util.PrefsManager;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefsManager prefs = new PrefsManager(this);
        if (prefs.isLoggedIn() && !prefs.isRememberDevice()) {
            prefs.clearSession();
        }
        if (prefs.isLoggedIn()) {
            Class<?> dest = prefs.isTherapist()
                    ? TherapistDashboardActivity.class
                    : DashboardActivity.class;
            startActivity(new Intent(this, dest));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}
