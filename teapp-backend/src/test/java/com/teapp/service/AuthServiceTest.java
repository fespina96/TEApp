package com.teapp.service;

import com.teapp.dto.auth.AuthResponse;
import com.teapp.dto.auth.LoginRequest;
import com.teapp.dto.auth.RegisterRequest;
import com.teapp.entity.User;
import com.teapp.enums.UserRole;
import com.teapp.repository.UserRepository;
import com.teapp.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(jwtTokenProvider.generateToken(any())).thenReturn("mocked.jwt.token");
        when(jwtTokenProvider.getJwtExpiration()).thenReturn(86400000L);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
    }

    // register

    @Test
    @DisplayName("register: email nuevo → crea usuario y devuelve token")
    void register_newEmail_createsUserAndReturnsToken() {
        when(userRepository.existsByEmail("padre@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(userId);
            return u;
        });

        RegisterRequest req = new RegisterRequest("padre@test.com", "Pass1234", "Juan García", java.time.LocalDate.of(1990, 1, 15), UserRole.PARENT);
        AuthResponse resp = authService.register(req);

        assertThat(resp.token()).isEqualTo("mocked.jwt.token");
        assertThat(resp.email()).isEqualTo("padre@test.com");
        assertThat(resp.fullName()).isEqualTo("Juan García");
        assertThat(resp.role()).isEqualTo("PARENT");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: el email se guarda en minúsculas y sin espacios")
    void register_normalizaElEmail() {
        when(userRepository.existsByEmail("Padre@Test.com ".trim().toLowerCase())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(userId);
            return u;
        });

        RegisterRequest req = new RegisterRequest("  Padre@Test.com  ", "Pass1234", "Juan García",
                java.time.LocalDate.of(1990, 1, 15), UserRole.PARENT);
        AuthResponse resp = authService.register(req);

        assertThat(resp.email()).isEqualTo("padre@test.com");
        verify(userRepository).existsByEmail("padre@test.com");
    }

    @Test
    @DisplayName("register: un email que ya existe con otras mayúsculas se rechaza")
    void register_emailDuplicadoConOtrasMayusculas() {
        when(userRepository.existsByEmail("padre@test.com")).thenReturn(true);

        RegisterRequest req = new RegisterRequest("PADRE@TEST.COM", "Pass1234", "Otro",
                java.time.LocalDate.of(1990, 1, 15), UserRole.PARENT);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("register: email duplicado → lanza IllegalArgumentException")
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("duplicado@test.com")).thenReturn(true);

        RegisterRequest req = new RegisterRequest("duplicado@test.com", "Pass1234", "Ana", java.time.LocalDate.of(1985, 6, 20), null);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicado@test.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: rol THERAPIST → genera código de invitación")
    void register_therapistRole_generatesInviteCode() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(userId);
            return u;
        });

        RegisterRequest req = new RegisterRequest("terapeuta@test.com", "Pass1234", "Dra. López", java.time.LocalDate.of(1980, 3, 10), UserRole.THERAPIST);
        AuthResponse resp = authService.register(req);

        assertThat(resp.inviteCode()).isNotNull().hasSize(8);
    }

    @Test
    @DisplayName("register: rol null → asigna PARENT por defecto")
    void register_nullRole_defaultsToParent() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(userId);
            return u;
        });

        RegisterRequest req = new RegisterRequest("padre@test.com", "Pass1234", "Carlos", java.time.LocalDate.of(1992, 8, 5), null);
        AuthResponse resp = authService.register(req);

        assertThat(resp.role()).isEqualTo("PARENT");
        assertThat(resp.inviteCode()).isNull();
    }

    // login

    @Test
    @DisplayName("login: credenciales correctas → devuelve token")
    void login_validCredentials_returnsToken() {
        User user = User.builder()
                .id(userId).email("padre@test.com")
                .password("hashed").fullName("Juan García")
                .role(UserRole.PARENT).build();

        when(userRepository.findByEmail("padre@test.com")).thenReturn(Optional.of(user));

        AuthResponse resp = authService.login(new LoginRequest("padre@test.com", "Pass1234"));

        assertThat(resp.token()).isEqualTo("mocked.jwt.token");
        assertThat(resp.email()).isEqualTo("padre@test.com");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: credenciales incorrectas → lanza BadCredentialsException")
    void login_badCredentials_throwsException() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(new LoginRequest("padre@test.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
