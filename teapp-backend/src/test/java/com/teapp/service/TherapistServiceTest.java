package com.teapp.service;

import com.teapp.entity.Child;
import com.teapp.entity.User;
import com.teapp.enums.UserRole;
import com.teapp.exception.ResourceNotFoundException;
import com.teapp.exception.UnauthorizedException;
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
@DisplayName("TherapistService – acceso a datos supervisados")
class TherapistServiceTest {

    @Mock UserRepository userRepository;
    @Mock ChildRepository childRepository;
    @Mock ChildService childService;
    @Mock ScheduleService scheduleService;
    @Mock Authentication authentication;
    @Mock SecurityContext securityContext;

    @InjectMocks TherapistService therapistService;

    private User terapeuta;
    private User padreVinculado;
    private Child participante;
    private final UUID idPadreVinculado = UUID.randomUUID();
    private final UUID idPadreAjeno     = UUID.randomUUID();
    private final UUID idParticipante   = UUID.randomUUID();

    @BeforeEach
    void setup() {
        padreVinculado = User.builder().id(idPadreVinculado).email("padre@test.com").role(UserRole.PARENT).build();
        terapeuta = User.builder()
                .id(UUID.randomUUID()).email("tera@test.com").role(UserRole.THERAPIST)
                .supervisedParents(new ArrayList<>(List.of(padreVinculado)))
                .build();
        participante = Child.builder()
                .id(idParticipante).user(padreVinculado).name("Sofía")
                .dateOfBirth(LocalDate.of(2020, 3, 10)).build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void teardown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(User usuario) {
        when(authentication.getName()).thenReturn(usuario.getEmail());
        when(userRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
    }

    // getSupervisedChildren

    @Test
    @DisplayName("getSupervisedChildren: terapeuta vinculado → devuelve los participantes")
    void getSupervisedChildren_terapeutaVinculado_devuelveParticipantes() {
        autenticarComo(terapeuta);
        when(childService.getChildrenByParentId(idPadreVinculado)).thenReturn(List.of());

        assertThatCode(() -> therapistService.getSupervisedChildren(idPadreVinculado))
                .doesNotThrowAnyException();
        verify(childService).getChildrenByParentId(idPadreVinculado);
    }

    @Test
    @DisplayName("getSupervisedChildren: terapeuta NO vinculado a ese padre → lanza UnauthorizedException")
    void getSupervisedChildren_padreAjeno_lanzaUnauthorized() {
        autenticarComo(terapeuta);

        assertThatThrownBy(() -> therapistService.getSupervisedChildren(idPadreAjeno))
                .isInstanceOf(UnauthorizedException.class);
        verify(childService, never()).getChildrenByParentId(any());
    }

    @Test
    @DisplayName("getSupervisedChildren: usuario con rol PARENT → lanza UnauthorizedException")
    void getSupervisedChildren_usuarioPadre_lanzaUnauthorized() {
        autenticarComo(padreVinculado);

        assertThatThrownBy(() -> therapistService.getSupervisedChildren(idPadreVinculado))
                .isInstanceOf(UnauthorizedException.class);
        verify(childService, never()).getChildrenByParentId(any());
    }

    // getSupervisedSchedule

    @Test
    @DisplayName("getSupervisedSchedule: terapeuta vinculado y participante del padre → devuelve la agenda")
    void getSupervisedSchedule_vinculado_devuelveAgenda() {
        autenticarComo(terapeuta);
        when(childRepository.findByIdAndUserId(idParticipante, idPadreVinculado)).thenReturn(Optional.of(participante));

        assertThatCode(() -> therapistService.getSupervisedSchedule(idPadreVinculado, idParticipante))
                .doesNotThrowAnyException();
        verify(scheduleService).getWeeklySchedule(idParticipante);
    }

    @Test
    @DisplayName("getSupervisedSchedule: terapeuta NO vinculado → lanza UnauthorizedException")
    void getSupervisedSchedule_padreAjeno_lanzaUnauthorized() {
        autenticarComo(terapeuta);

        assertThatThrownBy(() -> therapistService.getSupervisedSchedule(idPadreAjeno, idParticipante))
                .isInstanceOf(UnauthorizedException.class);
        verify(scheduleService, never()).getWeeklySchedule(any());
    }

    @Test
    @DisplayName("getSupervisedSchedule: participante que no pertenece al padre → lanza ResourceNotFoundException")
    void getSupervisedSchedule_participanteAjeno_lanzaNotFound() {
        autenticarComo(terapeuta);
        when(childRepository.findByIdAndUserId(idParticipante, idPadreVinculado)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> therapistService.getSupervisedSchedule(idPadreVinculado, idParticipante))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(scheduleService, never()).getWeeklySchedule(any());
    }

    @Test
    @DisplayName("getSupervisedSchedule: usuario con rol PARENT → lanza UnauthorizedException")
    void getSupervisedSchedule_usuarioPadre_lanzaUnauthorized() {
        autenticarComo(padreVinculado);

        assertThatThrownBy(() -> therapistService.getSupervisedSchedule(idPadreVinculado, idParticipante))
                .isInstanceOf(UnauthorizedException.class);
        verify(scheduleService, never()).getWeeklySchedule(any());
    }
}
