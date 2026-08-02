package com.teapp.util;

import com.teapp.entity.Child;
import com.teapp.entity.User;
import com.teapp.enums.UserRole;
import com.teapp.exception.ResourceNotFoundException;
import com.teapp.repository.ChildRepository;
import com.teapp.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityUtils – quién puede operar sobre un participante")
class SecurityUtilsTest {

    @Mock UserRepository userRepository;
    @Mock ChildRepository childRepository;
    @Mock Authentication authentication;
    @Mock SecurityContext securityContext;

    @InjectMocks SecurityUtils securityUtils;

    private User padreDueno;
    private User terapeutaVinculado;
    private User terapeutaAjeno;
    private User otroPadre;
    private Child participante;
    private final UUID idParticipante = UUID.randomUUID();

    @BeforeEach
    void setup() {
        padreDueno = User.builder()
                .id(UUID.randomUUID()).email("dueno@test.com").role(UserRole.PARENT).build();
        otroPadre = User.builder()
                .id(UUID.randomUUID()).email("otro@test.com").role(UserRole.PARENT).build();
        terapeutaVinculado = User.builder()
                .id(UUID.randomUUID()).email("vinculado@test.com").role(UserRole.THERAPIST)
                .supervisedParents(new ArrayList<>(List.of(padreDueno)))
                .build();
        terapeutaAjeno = User.builder()
                .id(UUID.randomUUID()).email("ajeno@test.com").role(UserRole.THERAPIST)
                .supervisedParents(new ArrayList<>(List.of(otroPadre)))
                .build();
        participante = Child.builder()
                .id(idParticipante).user(padreDueno).name("Sofía")
                .dateOfBirth(LocalDate.of(2020, 3, 10)).build();

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void teardown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(User usuario) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(usuario.getEmail());
        when(userRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
    }

    @Test
    @DisplayName("El padre dueño accede a su participante")
    void padreDueno_accede() {
        when(childRepository.findById(idParticipante)).thenReturn(Optional.of(participante));
        autenticarComo(padreDueno);

        assertThat(securityUtils.participanteAccesible(idParticipante)).isEqualTo(participante);
    }

    @Test
    @DisplayName("El terapeuta vinculado al padre accede al participante")
    void terapeutaVinculado_accede() {
        when(childRepository.findById(idParticipante)).thenReturn(Optional.of(participante));
        autenticarComo(terapeutaVinculado);

        assertThat(securityUtils.participanteAccesible(idParticipante)).isEqualTo(participante);
    }

    @Test
    @DisplayName("Un terapeuta no vinculado a ese padre no accede")
    void terapeutaAjeno_noAccede() {
        when(childRepository.findById(idParticipante)).thenReturn(Optional.of(participante));
        autenticarComo(terapeutaAjeno);

        assertThatThrownBy(() -> securityUtils.participanteAccesible(idParticipante))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Otro padre no accede al participante ajeno")
    void otroPadre_noAccede() {
        when(childRepository.findById(idParticipante)).thenReturn(Optional.of(participante));
        autenticarComo(otroPadre);

        assertThatThrownBy(() -> securityUtils.participanteAccesible(idParticipante))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Participante inexistente da el mismo error que uno sin acceso")
    void participanteInexistente_mismoError() {
        UUID idDesconocido = UUID.randomUUID();
        when(childRepository.findById(idDesconocido)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> securityUtils.participanteAccesible(idDesconocido))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
