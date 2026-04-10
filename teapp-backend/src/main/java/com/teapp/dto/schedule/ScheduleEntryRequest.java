package com.teapp.dto.schedule;

import com.teapp.enums.TimeSlot;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Petición para crear o actualizar una entrada en la agenda semanal de un niño.
 *
 * @param activityId identificador de la actividad
 * @param dayOfWeek  día de la semana (MONDAY..SUNDAY)
 * @param timeSlot   franja horaria (MORNING, AFTERNOON, NIGHT)
 * @param startTime  hora de inicio opcional
 * @param endTime    hora de fin opcional
 * @param sortOrder  orden de visualización dentro del slot
 * @param notes      notas específicas de esta entrada
 */
public record ScheduleEntryRequest(
    @NotNull(message = "La actividad es obligatoria")
    UUID activityId,

    @NotNull(message = "El día de la semana es obligatorio")
    DayOfWeek dayOfWeek,

    @NotNull(message = "La franja horaria es obligatoria")
    TimeSlot timeSlot,

    LocalTime startTime,
    LocalTime endTime,
    Integer sortOrder,

    @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
    String notes,

    Integer durationMinutes,
    Boolean pausable,
    Boolean requireFullTimer
) {}
