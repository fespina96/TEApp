package com.teapp.dto.schedule;

import com.teapp.dto.activity.ActivityResponse;
import com.teapp.enums.TimeSlot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta con los datos de una entrada de la agenda semanal.
 *
 * @param id        identificador único de la entrada
 * @param activity  datos de la actividad asociada
 * @param dayOfWeek día de la semana
 * @param timeSlot  franja horaria
 * @param startTime hora de inicio
 * @param endTime   hora de fin
 * @param sortOrder orden dentro del slot
 * @param notes     notas de la entrada
 */
public record ScheduleEntryResponse(
    UUID id,
    ActivityResponse activity,
    DayOfWeek dayOfWeek,
    TimeSlot timeSlot,
    LocalTime startTime,
    LocalTime endTime,
    int sortOrder,
    String notes,
    Integer durationMinutes,
    boolean pausable,
    boolean requireFullTimer,
    List<LocalDate> completedDates
) {}
