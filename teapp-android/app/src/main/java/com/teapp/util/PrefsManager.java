package com.teapp.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {
    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        prefs = context.getSharedPreferences(Constants.PREF_FILE, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(Constants.PREF_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(Constants.PREF_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void saveUser(String id, String email, String name, String role, String inviteCode) {
        prefs.edit()
                .putString(Constants.PREF_USER_ID, id)
                .putString(Constants.PREF_USER_EMAIL, email)
                .putString(Constants.PREF_USER_NAME, name)
                .putString(Constants.PREF_USER_ROLE, role)
                .putString(Constants.PREF_USER_INVITE_CODE, inviteCode)
                .apply();
    }

    public String getUserName()   { return prefs.getString(Constants.PREF_USER_NAME, ""); }
    public String getUserRole()   { return prefs.getString(Constants.PREF_USER_ROLE, "PARENT"); }
    public String getUserId()     { return prefs.getString(Constants.PREF_USER_ID, ""); }
    public String getUserEmail()  { return prefs.getString(Constants.PREF_USER_EMAIL, ""); }
    public String getInviteCode() { return prefs.getString(Constants.PREF_USER_INVITE_CODE, ""); }
    public String getUserAvatar() { return prefs.getString(Constants.PREF_USER_AVATAR, ""); }

    /** Se guarda aparte porque el avatar cambia sin volver a iniciar sesión. */
    public void saveUserAvatar(String avatarBase64) {
        prefs.edit().putString(Constants.PREF_USER_AVATAR, avatarBase64).apply();
    }

    public boolean isTherapist() { return "THERAPIST".equals(getUserRole()); }

    public void saveRememberDevice(boolean remember) {
        prefs.edit().putBoolean("remember_device", remember).apply();
    }
    public boolean isRememberDevice() {
        return prefs.getBoolean("remember_device", true);
    }

    public void clearSession() {
        prefs.edit()
                .remove(Constants.PREF_TOKEN)
                .remove(Constants.PREF_USER_ID)
                .remove(Constants.PREF_USER_EMAIL)
                .remove(Constants.PREF_USER_NAME)
                .remove(Constants.PREF_USER_ROLE)
                .remove(Constants.PREF_USER_INVITE_CODE)
                .apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
