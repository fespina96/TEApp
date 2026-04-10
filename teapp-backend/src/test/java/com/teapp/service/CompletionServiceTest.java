package com.teapp.service;

import com.teapp.dto.completion.CompletionResponse;
import com.teapp.entity.*;
import com.teapp.enums.UserRole;
import com.teapp.exception.UnauthorizedException;
import com.teapp.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompletionService")
class CompletionServiceTest {

    @Mock ActivityCompletionRepository completionRepository;
    @Mock ScheduleEntryRepository scheduleEntryRepository;
    @Mock ChildRepository childRepository;
    @Mock UserRepository userRepository;
    @Mock Authentication authentication;
    @Mock SecurityContext securityContext;

    @InjectMocks CompletionService completionService;

    private User parent;
    private Child child;
    private ScheduleEntry entry;
    private final UUID childId = UUID.randomUUID();
    private final UUID entryId = UUID.randomUUID();
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setup() {
        parent = User.builder().id(UUID.randomUUID()).email("padre@test.com").role(UserRole.PARENT).build();
        child  = Child.builder().id(childId).user(parent).name("Tomás").dateOfBirth(LocalDate.of(2018, 3, 10)).build();
        entry  = ScheduleEntry.builder().id(entryId).child(child).build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("padre@test.com");
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("padre@test.com")).thenReturn(Optional.of(parent));
        when(childRepository.findById(childId)).thenReturn(Optional.of(child));
    }

    @AfterEach
    void teardown() {
        SecurityContextHolder.clearContext();
    }

    // ─── markCompleted ────────────────────────────────────────────────────────

    @Test
    @DisplayName("markCompleted: actividad no completada → crea registro y lo devuelve")
    void markCompleted_newCompletion_createsAndReturns() {
        when(scheduleEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(completionRepository.findByScheduleEntryIdAndCompletedDate(entryId, today)).thenReturn(Optional.empty());
        when(childRepository.getReferenceById(childId)).thenReturn(child);

        ActivityCompletion saved = ActivityCompletion.builder()
                .id(UUID.randomUUID()).scheduleEntry(entry).child(child).completedDate(today).build();
        when(completionRepository.save(any())).thenReturn(saved);

        CompletionResponse resp = completionService.markCompleted(childId, entryId, today);

        assertThat(resp.completedDate()).isEqualTo(today);
        assertThat(resp.scheduleEntryId()).isEqualTo(entryId);
        verify(completionRepository).save(any(ActivityCompletion.class));
    }

    @Test
    @DisplayName("markCompleted: actividad ya completada → devuelve la existente (idempotente)")
    void markCompleted_alreadyCompleted_returnsExisting() {
        when(scheduleEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        ActivityCompletion existing = ActivityCompletion.builder()
                .id(UUID.randomUUID()).scheduleEntry(entry).child(child).completedDate(today).build();
        when(completionRepository.findByScheduleEntryIdAndCompletedDate(entryId, today)).thenReturn(Optional.of(existing));

        completionService.markCompleted(childId, entryId, today);

        verify(completionRepository, never()).save(any());
    }

    @Test
    @DisplayName("markCompleted: padre no es dueño del perfil → lanza UnauthorizedException")
    void markCompleted_notOwner_throwsUnauthorized() {
        User otherParent = User.builder().id(UUID.randomUUID()).email("padre@test.com").build();
        Child childOfOther = Child.builder().id(childId).user(otherParent).name("Pedro").dateOfBirth(LocalDate.now()).build();

        // Override setup: el childId pertenece a otro padre
        when(childRepository.findById(childId)).thenReturn(Optional.of(childOfOther));
        // El usuario autenticado es "padre@test.com" pero su ID es diferente al dueño del child
        User authUser = User.builder().id(UUID.randomUUID()).email("padre@test.com").build();
        when(userRepository.findByEmail("padre@test.com")).thenReturn(Optional.of(authUser));

        assertThatThrownBy(() -> completionService.markCompleted(childId, entryId, today))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ─── unmarkCompleted ──────────────────────────────────────────────────────

    @Test
    @DisplayName("unmarkCompleted: existe registro → lo elimina")
    void unmarkCompleted_existingCompletion_deletes() {
        ActivityCompletion existing = ActivityCompletion.builder()
                .id(UUID.randomUUID()).scheduleEntry(entry).child(child).completedDate(today).build();
        when(completionRepository.findByScheduleEntryIdAndCompletedDate(entryId, today)).thenReturn(Optional.of(existing));

        completionService.unmarkCompleted(childId, entryId, today);

        verify(completionRepository).delete(existing);
    }

    @Test
    @DisplayName("unmarkCompleted: no existe registro → no hace nada (idempotente)")
    void unmarkCompleted_noCompletion_doesNothing() {
        when(completionRepository.findByScheduleEntryIdAndCompletedDate(entryId, today)).thenReturn(Optional.empty());

        completionService.unmarkCompleted(childId, entryId, today);

        verify(completionRepository, never()).delete(any());
    }

    // ─── resetCurrentWeek ─────────────────────────────────────────────────────

    @Test
    @DisplayName("resetCurrentWeek: elimina completions de lunes a domingo de la semana actual")
    void resetCurrentWeek_deletesCurrentWeekCompletions() {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        completionService.resetCurrentWeek(childId);

        verify(completionRepository).deleteByChildIdAndCompletedDateBetween(childId, monday, sunday);
    }
}
