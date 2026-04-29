package com.teapp.controller;

import com.teapp.dto.auth.AuthResponse;
import com.teapp.dto.auth.ChangePasswordRequest;
import com.teapp.dto.auth.UpdateProfileRequest;
import com.teapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "Perfil del usuario autenticado")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Obtener perfil del usuario autenticado")
    public ResponseEntity<AuthResponse> getMe() {
        return ResponseEntity.ok(userService.getMe());
    }

    @PutMapping("/me")
    @Operation(summary = "Actualizar nombre y fecha de nacimiento del usuario")
    public ResponseEntity<AuthResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @PutMapping("/me/avatar")
    @Operation(summary = "Actualizar avatar del usuario autenticado")
    public ResponseEntity<Void> updateAvatar(@RequestBody String avatarBase64) {
        userService.updateAvatar(avatarBase64);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/password")
    @Operation(summary = "Cambiar contrasena del usuario autenticado")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @Operation(summary = "Eliminar cuenta del usuario autenticado (soft delete)")
    public ResponseEntity<Void> deleteMe() {
        userService.deleteMe();
        return ResponseEntity.noContent().build();
    }
}
