package com.teapp.service;

import com.teapp.dto.auth.AuthResponse;
import com.teapp.dto.auth.UpdateProfileRequest;
import com.teapp.entity.User;
import com.teapp.exception.ResourceNotFoundException;
import com.teapp.repository.UserRepository;
import com.teapp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio para gestionar el perfil del usuario autenticado.
 * Permite consultar datos, actualizar avatar y cambiar contraseña.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Retorna los datos del usuario autenticado junto con un token JWT fresco.
     *
     * @return respuesta con token y datos del perfil actual
     */
    @Transactional(readOnly = true)
    public AuthResponse getMe() {
        User usuario = obtenerUsuarioActual();
        UserDetails detallesUsuario = new org.springframework.security.core.userdetails.User(
                usuario.getEmail(), usuario.getPassword(), java.util.List.of()
        );
        String token = jwtTokenProvider.generateToken(detallesUsuario);
        return new AuthResponse(
            token,
            jwtTokenProvider.getJwtExpiration(),
            usuario.getId(),
            usuario.getEmail(),
            usuario.getFullName(),
            usuario.getAvatarBase64(),
            usuario.getRole().name(),
            usuario.getInviteCode(),
            usuario.getDateOfBirth(),
            usuario.getCreatedAt()
        );
    }

    /**
     * Actualiza la foto de perfil del usuario autenticado.
     * Si {@code avatarBase64} es nulo o vacío, elimina el avatar actual.
     *
     * @param avatarBase64 imagen en formato base64, o null para eliminar
     */
    @Transactional
    public void updateAvatar(String avatarBase64) {
        User usuario = obtenerUsuarioActual();
        usuario.setAvatarBase64(avatarBase64 == null || avatarBase64.isBlank() ? null : avatarBase64);
        userRepository.save(usuario);
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     * Verifica que la contraseña actual sea correcta antes de actualizar.
     *
     * @param contrasenaActual contraseña vigente del usuario
     * @param nuevaContrasena  nueva contraseña a establecer
     * @throws ResponseStatusException 400 si la contraseña actual no coincide
     */
    @Transactional
    public void changePassword(String contrasenaActual, String nuevaContrasena) {
        User usuario = obtenerUsuarioActual();
        if (!passwordEncoder.matches(contrasenaActual, usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }
        usuario.setPassword(passwordEncoder.encode(nuevaContrasena));
        userRepository.save(usuario);
    }

    /**
     * Actualiza el nombre completo y fecha de nacimiento del usuario autenticado.
     *
     * @param solicitud datos del perfil a actualizar
     */
    @Transactional
    public AuthResponse updateProfile(UpdateProfileRequest solicitud) {
        User usuario = obtenerUsuarioActual();
        usuario.setFullName(solicitud.fullName());
        if (solicitud.dateOfBirth() != null) {
            usuario.setDateOfBirth(solicitud.dateOfBirth());
        }
        userRepository.save(usuario);
        return getMe();
    }

    /**
     * Elimina lógicamente la cuenta del usuario autenticado (soft delete).
     * El usuario no podrá iniciar sesión después de esta operación.
     */
    @Transactional
    public void deleteMe() {
        User usuario = obtenerUsuarioActual();
        usuario.setDeletedAt(LocalDateTime.now());
        userRepository.save(usuario);
    }

    private User obtenerUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", email));
    }
}
