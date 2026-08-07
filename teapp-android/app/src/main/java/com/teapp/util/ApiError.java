package com.teapp.util;

import org.json.JSONObject;

import retrofit2.Response;

/**
 * Traduce el cuerpo de error del backend al texto que se le muestra al usuario.
 *
 * El servidor manda el motivo del rechazo en dos formas: las validaciones traen un
 * mapa "errors" con un mensaje por campo, y el resto un "message" suelto.
 */
public final class ApiError {

    private ApiError() {}

    /**
     * @param response  la respuesta fallida
     * @param porDefecto texto a usar si el cuerpo no se puede leer
     * @return el motivo concreto del rechazo
     */
    public static String mensaje(Response<?> response, String porDefecto) {
        if (response.errorBody() == null) return porDefecto;
        try {
            JSONObject cuerpo = new JSONObject(response.errorBody().string());

            // Las validaciones traen un campo por error; con mostrar el primero alcanza.
            JSONObject errores = cuerpo.optJSONObject("errors");
            if (errores != null && errores.length() > 0) {
                String campo = errores.keys().next();
                String detalle = errores.optString(campo, "");
                if (!detalle.isEmpty()) return detalle;
            }

            String mensaje = cuerpo.optString("message", "");
            return mensaje.isEmpty() ? porDefecto : mensaje;
        } catch (Exception e) {
            return porDefecto;
        }
    }
}
