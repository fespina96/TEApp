package com.teapp.dto.activity;

import com.teapp.enums.ActivityCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Petición para crear o actualizar una actividad personalizada.
 *
 * @param name        nombre de la actividad
 * @param description descripción opcional
 * @param category    categoría de la actividad
 * @param iconName    nombre del icono de Material Icons
 * @param color       color hexadecimal representativo
 */
public record ActivityRequest(
    @NotBlank(message = "El nombre de la actividad es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    String name,

    @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
    String description,

    @NotNull(message = "La categoría es obligatoria")
    ActivityCategory category,

    @Size(max = 50, message = "El nombre del icono no puede superar 50 caracteres")
    String iconName,

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser un hexadecimal válido")
    String color,

    String imageBase64,

    String pictogramUrl,

    Integer durationMinutes,

    Boolean pausable
) {}
