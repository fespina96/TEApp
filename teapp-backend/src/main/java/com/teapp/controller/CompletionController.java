package com.teapp.controller;

import com.teapp.dto.completion.CompletionRequest;
import com.teapp.dto.completion.CompletionResponse;
import com.teapp.service.CompletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controlador REST para registrar completitudes de actividades.
 * Permite marcar, desmarcar y resetear actividades completadas por un participante.
 * Todos los endpoints requieren autenticación JWT.
 */
@RestController
@RequestMapping("/api/v1/children/{childId}")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Completitud", description = "Marcar actividades como completadas")
public class CompletionController {

    private final CompletionService completionService;

    @PostMapping("/schedule/{entryId}/completions")
    @Operation(summary = "Marcar actividad como completada",
               description = "Registra que una actividad fue completada en la fecha indicada. Operación idempotente.")
    public ResponseEntity<CompletionResponse> marcarCompletada(
            @PathVariable UUID childId,
            @PathVariable UUID entryId,
            @Valid @RequestBody CompletionRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(completionService.markCompleted(childId, entryId, solicitud.date()));
    }

    @DeleteMapping("/schedule/{entryId}/completions")
    @Operation(summary = "Desmarcar actividad completada",
               description = "Elimina el registro de completitud de una actividad en la fecha indicada.")
    public ResponseEntity<Void> desmarcarCompletada(
            @PathVariable UUID childId,
            @PathVariable UUID entryId,
            @Valid @RequestBody CompletionRequest solicitud) {
        completionService.unmarkCompleted(childId, entryId, solicitud.date());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/completions/current-week")
    @Operation(summary = "Resetear completitudes de la semana actual",
               description = "Borra todas las completitudes del participante para la semana en curso (lunes a domingo).")
    public ResponseEntity<Void> resetearSemanaActual(@PathVariable UUID childId) {
        completionService.resetCurrentWeek(childId);
        return ResponseEntity.noContent().build();
    }
}
