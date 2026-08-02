package com.teapp.controller;

import com.teapp.dto.schedule.ScheduleEntryRequest;
import com.teapp.dto.schedule.ScheduleEntryResponse;
import com.teapp.dto.schedule.ScheduleEntryUpdateRequest;
import com.teapp.dto.schedule.WeeklyScheduleResponse;
import com.teapp.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.UUID;

/**
 * Controlador REST para la gestión de la agenda semanal de los niños.
 */
@RestController
@RequestMapping("/api/v1/children/{childId}/schedule")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Agenda", description = "Gestión de la agenda semanal de los niños")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    @Operation(summary = "Obtener agenda semanal",
               description = "Retorna la agenda completa organizada por día y franja horaria. " +
                             "Filtrable por día con ?day=MONDAY")
    public ResponseEntity<WeeklyScheduleResponse> getSchedule(
            @PathVariable UUID childId,
            @RequestParam(required = false) DayOfWeek day) {
        return ResponseEntity.ok(scheduleService.getWeeklySchedule(childId, day));
    }

    @PostMapping
    @Operation(summary = "Agregar actividad a la agenda")
    public ResponseEntity<ScheduleEntryResponse> addEntry(
            @PathVariable UUID childId,
            @Valid @RequestBody ScheduleEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.addEntry(childId, request));
    }

    @PutMapping("/{entryId}")
    @Operation(summary = "Actualizar entrada de la agenda")
    public ResponseEntity<ScheduleEntryResponse> updateEntry(
            @PathVariable UUID childId,
            @PathVariable UUID entryId,
            @Valid @RequestBody ScheduleEntryUpdateRequest request) {
        return ResponseEntity.ok(scheduleService.updateEntry(childId, entryId, request));
    }

    @DeleteMapping("/{entryId}")
    @Operation(summary = "Eliminar entrada de la agenda")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable UUID childId,
            @PathVariable UUID entryId) {
        scheduleService.deleteEntry(childId, entryId);
        return ResponseEntity.noContent().build();
    }
}
