package com.teapp.dto.child;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Petición para crear o actualizar el perfil de un niño.
 *
 * @param name        nombre del niño
 * @param dateOfBirth fecha de nacimiento
 * @param avatarColor color hexadecimal para el avatar (p.ej. #A8D8EA)
 * @param notes       notas adicionales del padre
 */
public record ChildRequest(
    @NotBlank(message = "El nombre del niño es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    String name,

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    LocalDate dateOfBirth,

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser un hexadecimal válido (ej: #A8D8EA)")
    String avatarColor,

    @Size(max = 2000, message = "Las notas no pueden superar 2000 caracteres")
    String notes
) {}
