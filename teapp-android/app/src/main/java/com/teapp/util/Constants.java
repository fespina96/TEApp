package com.teapp.util;

public final class Constants {
    // Cambiar a 10.0.2.2 para emulador, o a la IP local de la PC para dispositivo físico
    public static final String BASE_URL = "http://192.168.1.34:8080/api/v1/";

    public static final String PREF_FILE = "teapp_prefs";
    public static final String PREF_TOKEN = "teapp_token";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_EMAIL = "user_email";
    public static final String PREF_USER_NAME = "user_name";
    public static final String PREF_USER_ROLE = "user_role";
    public static final String PREF_USER_INVITE_CODE = "user_invite_code";

    public static final String EXTRA_CHILD_ID = "child_id";
    public static final String EXTRA_CHILD_NAME = "child_name";
    public static final String EXTRA_CHILD = "child";

    private Constants() {}
}
