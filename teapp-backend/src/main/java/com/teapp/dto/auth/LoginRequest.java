package com.teapp.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Petición de inicio de sesión del padre.
 *
 * @param email    correo electrónico del padre
 * @param password contraseña del padre
 */
public record LoginRequest(
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene formato válido")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    String password
) {}
