package com.teapp.dto.activity;

import com.teapp.enums.ActivityCategory;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta con los datos de una actividad.
 *
 * @param id          identificador único
 * @param name        nombre de la actividad
 * @param description descripción
 * @param category    categoría
 * @param iconName    nombre del icono de Material Icons
 * @param color       color hexadecimal
 * @param predefined  true si es del catálogo del sistema
 * @param createdAt   fecha de creación
 */
public record ActivityResponse(
    UUID id,
    String name,
    String description,
    ActivityCategory category,
    String iconName,
    String color,
    boolean predefined,
    boolean therapistCreated,
    String imageBase64,
    String pictogramUrl,
    Integer durationMinutes,
    boolean pausable,
    int stepCount,
    LocalDateTime createdAt
) {}
