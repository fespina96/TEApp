package com.teapp.controller;

import com.teapp.dto.child.ChildRequest;
import com.teapp.dto.child.ChildResponse;
import com.teapp.service.ChildService;
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
 * Controlador REST para la gestión de perfiles de niños.
 * Todos los endpoints requieren autenticación JWT.
 */
@RestController
@RequestMapping("/api/v1/children")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Niños", description = "Gestión de perfiles de niños")
public class ChildController {

    private final ChildService childService;

    @GetMapping
    @Operation(summary = "Listar mis hijos", description = "Retorna todos los perfiles de niños del padre autenticado")
    public ResponseEntity<List<ChildResponse>> getAll() {
        return ResponseEntity.ok(childService.getAll());
    }

    @GetMapping("/{childId}")
    @Operation(summary = "Obtener un hijo", description = "Retorna el perfil de un niño específico")
    public ResponseEntity<ChildResponse> getById(@PathVariable UUID childId) {
        return ResponseEntity.ok(childService.getById(childId));
    }

    @PostMapping
    @Operation(summary = "Crear perfil de niño", description = "Crea un nuevo perfil de niño asociado al padre")
    public ResponseEntity<ChildResponse> create(@Valid @RequestBody ChildRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(childService.create(request));
    }

    @PutMapping("/{childId}")
    @Operation(summary = "Actualizar perfil de niño")
    public ResponseEntity<ChildResponse> update(@PathVariable UUID childId,
                                                 @Valid @RequestBody ChildRequest request) {
        return ResponseEntity.ok(childService.update(childId, request));
    }

    @PutMapping("/{childId}/avatar")
    @Operation(summary = "Actualizar avatar del participante")
    public ResponseEntity<Void> updateAvatar(@PathVariable UUID childId,
                                              @RequestBody String avatarBase64) {
        childService.updateAvatar(childId, avatarBase64);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{childId}")
    @Operation(summary = "Eliminar perfil de niño", description = "Elimina el perfil y toda su agenda en cascada")
    public ResponseEntity<Void> delete(@PathVariable UUID childId) {
        childService.delete(childId);
        return ResponseEntity.noContent().build();
    }
}
