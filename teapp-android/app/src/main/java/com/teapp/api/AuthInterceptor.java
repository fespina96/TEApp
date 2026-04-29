package com.teapp.api;

import com.teapp.util.PrefsManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final PrefsManager prefs;

    public AuthInterceptor(PrefsManager prefs) {
        this.prefs = prefs;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String token = prefs.getToken();
        Request original = chain.request();
        if (token == null) {
            return chain.proceed(original);
        }
        Request request = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(request);
    }
}
