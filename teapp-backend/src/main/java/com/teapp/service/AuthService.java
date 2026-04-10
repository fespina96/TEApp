package com.teapp.service;

import com.teapp.dto.auth.AuthResponse;
import com.teapp.dto.auth.LoginRequest;
import com.teapp.dto.auth.RegisterRequest;
import com.teapp.entity.User;
import com.teapp.enums.UserRole;
import com.teapp.exception.ResourceNotFoundException;
import com.teapp.repository.UserRepository;
import com.teapp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * Servicio de autenticación y registro de usuarios.
 * Gestiona el alta de nuevos padres/terapeutas y la generación de tokens JWT.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /**
     * Registra un nuevo usuario (padre o terapeuta) y retorna su token JWT.
     * Si el rol es THERAPIST, genera un código de invitación único.
     *
     * @param solicitud datos de registro del nuevo usuario
     * @return respuesta con token JWT y datos del perfil creado
     * @throws IllegalArgumentException si ya existe una cuenta con el email indicado
     */
    @Transactional
    public AuthResponse register(RegisterRequest solicitud) {
        if (userRepository.existsByEmail(solicitud.email())) {
            throw new IllegalArgumentException("Ya existe una cuenta con el email: " + solicitud.email());
        }

        UserRole rol = solicitud.role() != null ? solicitud.role() : UserRole.PARENT;
        String codigoInvitacion = null;
        if (rol == UserRole.THERAPIST) {
            codigoInvitacion = generarCodigoInvitacion();
        }

        User usuario = User.builder()
                .email(solicitud.email())
                .password(passwordEncoder.encode(solicitud.password()))
                .fullName(solicitud.fullName())
                .dateOfBirth(solicitud.dateOfBirth())
                .role(rol)
                .inviteCode(codigoInvitacion)
                .build();
        usuario = userRepository.save(usuario);

        UserDetails detallesUsuario = new org.springframework.security.core.userdetails.User(
                usuario.getEmail(), usuario.getPassword(), java.util.List.of()
        );
        String token = jwtTokenProvider.generateToken(detallesUsuario);

        return new AuthResponse(
            token, jwtTokenProvider.getJwtExpiration(),
            usuario.getId(), usuario.getEmail(), usuario.getFullName(),
            usuario.getAvatarBase64(), usuario.getRole().name(),
            usuario.getInviteCode(), usuario.getDateOfBirth()
        );
    }

    /**
     * Autentica un usuario con email y contraseña, y retorna su token JWT.
     *
     * @param solicitud credenciales de inicio de sesión
     * @return respuesta con token JWT y datos del perfil
     * @throws org.springframework.security.core.AuthenticationException si las credenciales son incorrectas
     */
    public AuthResponse login(LoginRequest solicitud) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(solicitud.email(), solicitud.password())
        );

        User usuario = userRepository.findByEmail(solicitud.email())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", solicitud.email()));

        UserDetails detallesUsuario = new org.springframework.security.core.userdetails.User(
                usuario.getEmail(), usuario.getPassword(), java.util.List.of()
        );
        String token = jwtTokenProvider.generateToken(detallesUsuario);

        return new AuthResponse(
            token, jwtTokenProvider.getJwtExpiration(),
            usuario.getId(), usuario.getEmail(), usuario.getFullName(),
            usuario.getAvatarBase64(), usuario.getRole().name(),
            usuario.getInviteCode(), usuario.getDateOfBirth()
        );
    }

    /**
     * Genera un código de invitación aleatorio de 8 caracteres alfanuméricos
     * (sin caracteres ambiguos como O, 0, I, 1).
     *
     * @return código de invitación en mayúsculas
     */
    private String generarCodigoInvitacion() {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom generador = new SecureRandom();
        StringBuilder codigo = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            codigo.append(caracteres.charAt(generador.nextInt(caracteres.length())));
        }
        return codigo.toString();
    }
}
