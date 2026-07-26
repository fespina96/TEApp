package com.teapp.service;

import com.teapp.entity.User;
import com.teapp.enums.UserRole;
import com.teapp.repository.UserRepository;
import com.teapp.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock PasswordEncoder passwordEncoder;
    @Mock Authentication authentication;
    @Mock SecurityContext securityContext;

    @InjectMocks UserService userService;

    private User currentUser;

    @BeforeEach
    void setup() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .email("padre@test.com")
                .password("$2a$hashed_current")
                .fullName("Juan García")
                .role(UserRole.PARENT)
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("padre@test.com");
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmail("padre@test.com")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void teardown() {
        SecurityContextHolder.clearContext();
    }

    // changePassword

    @Test
    @DisplayName("changePassword: contraseña actual correcta → actualiza contraseña")
    void changePassword_correctCurrentPassword_updatesPassword() {
        when(passwordEncoder.matches("CurrentPass1", "$2a$hashed_current")).thenReturn(true);
        when(passwordEncoder.encode("NewPass1")).thenReturn("$2a$hashed_new");

        userService.changePassword("CurrentPass1", "NewPass1");

        assertThat(currentUser.getPassword()).isEqualTo("$2a$hashed_new");
        verify(userRepository).save(currentUser);
    }

    @Test
    @DisplayName("changePassword: contraseña actual incorrecta → lanza 400")
    void changePassword_wrongCurrentPassword_throws400() {
        when(passwordEncoder.matches("WrongPass", "$2a$hashed_current")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("WrongPass", "NewPass1"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("incorrecta");
                });

        verify(userRepository, never()).save(any());
    }

    // updateAvatar

    @Test
    @DisplayName("updateAvatar: base64 válido → guarda avatar")
    void updateAvatar_validBase64_savesAvatar() {
        userService.updateAvatar("data:image/png;base64,abc123");

        assertThat(currentUser.getAvatarBase64()).isEqualTo("data:image/png;base64,abc123");
        verify(userRepository).save(currentUser);
    }

    @Test
    @DisplayName("updateAvatar: string vacío → limpia el avatar")
    void updateAvatar_emptyString_clearsAvatar() {
        currentUser.setAvatarBase64("foto_previa");

        userService.updateAvatar("");

        assertThat(currentUser.getAvatarBase64()).isNull();
        verify(userRepository).save(currentUser);
    }
}
