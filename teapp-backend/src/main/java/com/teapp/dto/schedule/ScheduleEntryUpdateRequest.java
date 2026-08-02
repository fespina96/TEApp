package com.teapp.dto.schedule;

import com.teapp.enums.TimeSlot;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Petición para actualizar una entrada de agenda.
 *
 * A diferencia de {@link ScheduleEntryRequest}, todos los campos son opcionales:
 * los clientes envían sólo lo que cambian (la web reordena mandando el orden y la
 * app móvil edita duración y notas). Los valores que sí llegan deben respetar los
 * mismos rangos que en el alta.
 */
public record ScheduleEntryUpdateRequest(
    UUID activityId,

    DayOfWeek dayOfWeek,

    TimeSlot timeSlot,

    LocalTime startTime,

    LocalTime endTime,

    @PositiveOrZero(message = "El orden no puede ser negativo")
    Integer sortOrder,

    @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
    String notes,

    @Min(value = 1, message = "La duración debe ser de al menos 1 minuto")
    @Max(value = 180, message = "La duración no puede superar los 180 minutos")
    Integer durationMinutes,

    Boolean pausable,

    Boolean requireFullTimer
) {}
