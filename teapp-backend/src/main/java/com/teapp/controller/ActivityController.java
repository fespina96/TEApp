package com.teapp.controller;

import com.teapp.dto.activity.ActivityRequest;
import com.teapp.dto.activity.ActivityResponse;
import com.teapp.dto.activity.ActivityStepRequest;
import com.teapp.dto.activity.ActivityStepResponse;
import com.teapp.enums.ActivityCategory;
import com.teapp.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión del catálogo de actividades.
 */
@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Actividades", description = "Catálogo de actividades predefinidas y personalizadas")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    @Operation(summary = "Listar actividades disponibles",
               description = "Retorna actividades predefinidas del sistema más las personalizadas del padre. " +
                             "Filtrable por categoría con el parámetro ?category=HYGIENE")
    public ResponseEntity<List<ActivityResponse>> getAvailable(
            @RequestParam(required = false) ActivityCategory category) {
        return ResponseEntity.ok(activityService.getAvailable(category));
    }

    @GetMapping("/predefined")
    @Operation(summary = "Listar actividades predefinidas", description = "Retorna únicamente el catálogo del sistema")
    public ResponseEntity<List<ActivityResponse>> getPredefined() {
        return ResponseEntity.ok(activityService.getPredefined());
    }

    @PostMapping
    @Operation(summary = "Crear actividad personalizada")
    public ResponseEntity<ActivityResponse> create(@Valid @RequestBody ActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.create(request));
    }

    @PutMapping("/{activityId}")
    @Operation(summary = "Actualizar actividad personalizada")
    public ResponseEntity<ActivityResponse> update(@PathVariable UUID activityId,
                                                    @Valid @RequestBody ActivityRequest request) {
        return ResponseEntity.ok(activityService.update(activityId, request));
    }

    @DeleteMapping("/{activityId}")
    @Operation(summary = "Eliminar actividad personalizada",
               description = "Solo se puede eliminar si no está siendo usada en ninguna agenda")
    public ResponseEntity<Void> delete(@PathVariable UUID activityId) {
        activityService.delete(activityId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{activityId}/steps")
    @Operation(summary = "Obtener pasos de una actividad")
    public ResponseEntity<List<ActivityStepResponse>> getSteps(@PathVariable UUID activityId) {
        return ResponseEntity.ok(activityService.getSteps(activityId));
    }

    @PutMapping("/{activityId}/steps")
    @Operation(summary = "Guardar pasos de una actividad (reemplaza todos los pasos)")
    public ResponseEntity<List<ActivityStepResponse>> saveSteps(
            @PathVariable UUID activityId,
            @Valid @RequestBody List<ActivityStepRequest> steps) {
        return ResponseEntity.ok(activityService.saveSteps(activityId, steps));
    }
}
