package com.teapp.controller;

import com.teapp.service.TherapistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controlador REST para la vinculación y supervisión entre padres y terapeutas.
 * Los padres se vinculan usando el código de invitación único del terapeuta.
 * Los terapeutas pueden consultar las agendas de sus padres supervisados.
 * Todos los endpoints requieren autenticación JWT.
 */
@RestController
@RequestMapping("/api/v1/therapist")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Terapeutas", description = "Vinculación y supervisión entre padres y terapeutas")
public class TherapistController {

    private final TherapistService therapistService;

    @PostMapping("/link")
    @Operation(summary = "Vincular padre a terapeuta",
               description = "El padre usa el código de invitación del terapeuta para vincularse.")
    public ResponseEntity<Void> vincularATerapeuta(@RequestBody Map<String, String> cuerpo) {
        therapistService.linkToTherapist(cuerpo.get("inviteCode"));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/link/{therapistId}")
    @Operation(summary = "Desvincular padre de terapeuta")
    public ResponseEntity<Void> desvincularDeTerapeuta(@PathVariable UUID therapistId) {
        therapistService.unlinkFromTherapist(therapistId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/supervised")
    @Operation(summary = "Obtener padres supervisados",
               description = "El terapeuta obtiene la lista de padres que lo tienen vinculado.")
    public ResponseEntity<List<Map<String, Object>>> obtenerPadresSupervisados() {
        return ResponseEntity.ok(therapistService.getSupervisedParents());
    }

    @GetMapping("/my-therapists")
    @Operation(summary = "Obtener terapeutas vinculados",
               description = "El padre obtiene la lista de terapeutas a los que está vinculado.")
    public ResponseEntity<List<Map<String, Object>>> obtenerMisTerapeutas() {
        return ResponseEntity.ok(therapistService.getMyTherapists());
    }

    @GetMapping("/supervised/{parentId}/children")
    @Operation(summary = "Obtener participantes de un padre supervisado")
    public ResponseEntity<?> obtenerParticipantesDePadre(@PathVariable UUID parentId) {
        return ResponseEntity.ok(therapistService.getSupervisedChildren(parentId));
    }

    @GetMapping("/supervised/{parentId}/children/{childId}/schedule")
    @Operation(summary = "Obtener agenda de un participante supervisado")
    public ResponseEntity<?> obtenerAgendaSupervisada(
            @PathVariable UUID parentId,
            @PathVariable UUID childId) {
        return ResponseEntity.ok(therapistService.getSupervisedSchedule(parentId, childId));
    }
}
