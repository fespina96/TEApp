package com.teapp.service;

import com.teapp.entity.PasswordResetToken;
import com.teapp.entity.User;
import com.teapp.repository.PasswordResetTokenRepository;
import com.teapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Servicio para la recuperación de contraseña mediante email.
 * Genera tokens seguros de un solo uso con validez de 1 hora
 * y los envía al email del usuario usando la API de Resend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Solicita un reset de contraseña para el email indicado.
     * Genera un token seguro, invalida tokens anteriores y envía el email.
     * Si el email no existe, no revela esta información por seguridad.
     *
     * @param email dirección de correo del usuario que solicita el reset
     */
    @Transactional
    public void requestReset(String email) {
        Optional<User> usuarioOpcional = userRepository.findByEmail(email);
        if (usuarioOpcional.isEmpty()) {
            // Silencioso por seguridad: no revelamos si el email existe
            log.info("Reset solicitado para email inexistente: {}", email);
            return;
        }

        User usuario = usuarioOpcional.get();

        // Invalida tokens anteriores del mismo usuario
        tokenRepository.deleteByUserId(usuario.getId());

        // Genera token seguro de 36 bytes → ~48 chars base64url
        byte[] bytesAleatorios = new byte[36];
        new SecureRandom().nextBytes(bytesAleatorios);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytesAleatorios);

        PasswordResetToken tokenReset = PasswordResetToken.builder()
                .user(usuario)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        tokenRepository.save(tokenReset);
        enviarEmailReset(usuario.getEmail(), usuario.getFullName(), token);
    }

    /**
     * Valida el token de reset y actualiza la contraseña del usuario.
     *
     * @param token       token de reset recibido por email
     * @param nuevaContrasena nueva contraseña a establecer
     * @throws IllegalArgumentException si el token es inválido, expirado o ya fue usado
     */
    @Transactional
    public void resetPassword(String token, String nuevaContrasena) {
        PasswordResetToken tokenReset = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o expirado"));

        if (tokenReset.isExpired()) {
            throw new IllegalArgumentException("El enlace de recuperación expiró. Solicitá uno nuevo.");
        }
        if (tokenReset.isUsed()) {
            throw new IllegalArgumentException("Este enlace ya fue utilizado.");
        }

        User usuario = tokenReset.getUser();
        usuario.setPassword(passwordEncoder.encode(nuevaContrasena));
        userRepository.save(usuario);

        tokenReset.setUsedAt(LocalDateTime.now());
        tokenRepository.save(tokenReset);
    }

    /**
     * Envía el email de recuperación con el enlace de reset usando la API de Resend.
     *
     * @param emailDestino dirección de email del destinatario
     * @param nombreCompleto nombre completo del usuario
     * @param token          token de reset a incluir en el enlace
     */
    private void enviarEmailReset(String emailDestino, String nombreCompleto, String token) {
        String urlReset = frontendUrl + "/reset-password?token=" + token;
        String nombreCorto = nombreCompleto.split(" ")[0];

        String contenidoHtml = """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 24px;">
              <h2 style="color: #2C3E50; margin-bottom: 8px;">Recuperar contraseña</h2>
              <p style="color: #555;">Hola <strong>%s</strong>,</p>
              <p style="color: #555;">Recibimos una solicitud para restablecer la contraseña de tu cuenta en <strong>TEApp</strong>.</p>
              <div style="text-align: center; margin: 32px 0;">
                <a href="%s"
                   style="background: #A8D8EA; color: #2C3E50; padding: 14px 28px;
                          border-radius: 8px; text-decoration: none; font-weight: bold; font-size: 16px;">
                  Restablecer contraseña
                </a>
              </div>
              <p style="color: #888; font-size: 13px;">Este enlace expira en <strong>1 hora</strong>. Si no solicitaste este cambio, podés ignorar este email.</p>
            </div>
            """.formatted(nombreCorto, urlReset);

        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_JSON);
        cabeceras.setBearerAuth(resendApiKey);

        Map<String, Object> cuerpo = Map.of(
                "from", fromEmail,
                "to", new String[]{ emailDestino },
                "subject", "Restablecer contraseña - TEApp",
                "html", contenidoHtml
        );

        HttpEntity<Map<String, Object>> peticion = new HttpEntity<>(cuerpo, cabeceras);

        try {
            restTemplate.postForEntity("https://api.resend.com/emails", peticion, Map.class);
            log.info("Email de reset enviado a {}", emailDestino);
        } catch (Exception e) {
            log.error("Error enviando email de reset a {}: {}", emailDestino, e.getMessage());
            // No lanzamos excepción al cliente por seguridad
        }
    }
}
