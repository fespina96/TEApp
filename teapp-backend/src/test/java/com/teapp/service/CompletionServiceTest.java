package com.teapp.service;

import com.teapp.dto.completion.CompletionResponse;
import com.teapp.entity.*;
import com.teapp.enums.UserRole;
import com.teapp.exception.ResourceNotFoundException;
import com.teapp.repository.*;
import com.teapp.util.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @Mock SecurityUtils securityUtils;

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
    }

    // markCompleted

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
    @DisplayName("markCompleted: sin acceso al participante → no registra nada")
    void markCompleted_sinAcceso_noRegistra() {
        doThrow(new ResourceNotFoundException("Niño", childId))
                .when(securityUtils).participanteAccesible(childId);

        assertThatThrownBy(() -> completionService.markCompleted(childId, entryId, today))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(completionRepository, never()).save(any());
    }

    // unmarkCompleted

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

    // resetCurrentWeek

    @Test
    @DisplayName("resetCurrentWeek: elimina completions de lunes a domingo de la semana actual")
    void resetCurrentWeek_deletesCurrentWeekCompletions() {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        completionService.resetCurrentWeek(childId);

        verify(completionRepository).deleteByChildIdAndCompletedDateBetween(childId, monday, sunday);
    }

    @Test
    @DisplayName("resetCurrentWeek: sin acceso al participante → no borra nada")
    void resetCurrentWeek_sinAcceso_noBorra() {
        doThrow(new ResourceNotFoundException("Niño", childId))
                .when(securityUtils).participanteAccesible(childId);

        assertThatThrownBy(() -> completionService.resetCurrentWeek(childId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(completionRepository, never()).deleteByChildIdAndCompletedDateBetween(any(), any(), any());
    }
}
