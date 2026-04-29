package com.teapp.dto.auth;

import com.teapp.enums.UserRole;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Petición de registro de un nuevo usuario en el sistema.
 *
 * @param email    correo electrónico único
 * @param password contraseña (mínimo 6 caracteres)
 * @param fullName nombre completo del padre/tutor o terapeuta
 * @param role     rol del usuario (PARENT o THERAPIST), opcional (por defecto PARENT)
 */
public record RegisterRequest(
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene formato válido")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
        message = "La contraseña debe contener al menos una mayúscula y un número"
    )
    String password,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    String fullName,

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser en el pasado")
    LocalDate dateOfBirth,

    UserRole role
) {}
