package com.teapp.dto.schedule;

import com.teapp.enums.TimeSlot;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Respuesta completa de la agenda semanal de un niño.
 * Organizada como mapa de día → franja horaria → lista de entradas.
 *
 * @param childId identificador del niño
 * @param week    mapa con la agenda organizada por día y franja horaria
 */
public record WeeklyScheduleResponse(
    UUID childId,
    Map<DayOfWeek, Map<TimeSlot, List<ScheduleEntryResponse>>> week
) {}
