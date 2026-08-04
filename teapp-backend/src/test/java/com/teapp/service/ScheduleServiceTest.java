package com.teapp.service;

import com.teapp.dto.schedule.ScheduleEntryResponse;
import com.teapp.dto.schedule.ScheduleEntryUpdateRequest;
import com.teapp.entity.Activity;
import com.teapp.entity.Child;
import com.teapp.entity.ScheduleEntry;
import com.teapp.entity.User;
import com.teapp.enums.ActivityCategory;
import com.teapp.enums.TimeSlot;
import com.teapp.enums.UserRole;
import com.teapp.exception.ResourceNotFoundException;
import com.teapp.repository.*;
import com.teapp.util.SecurityUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService · actualización parcial de una entrada")
class ScheduleServiceTest {

    @Mock ScheduleEntryRepository scheduleEntryRepository;
    @Mock ChildRepository childRepository;
    @Mock ActivityRepository activityRepository;
    @Mock ActivityStepRepository stepRepository;
    @Mock UserRepository userRepository;
    @Mock ActivityCompletionRepository completionRepository;
    @Mock SecurityUtils securityUtils;

    @InjectMocks ScheduleService scheduleService;

    private ScheduleEntry entrada;
    private final UUID idParticipante = UUID.randomUUID();
    private final UUID idEntrada = UUID.randomUUID();

    @BeforeEach
    void setup() {
        User padre = User.builder().id(UUID.randomUUID()).email("padre@test.com").role(UserRole.PARENT).build();
        Child participante = Child.builder().id(idParticipante).user(padre).name("Tomás")
                .dateOfBirth(LocalDate.of(2018, 3, 10)).build();
        Activity actividad = Activity.builder().id(UUID.randomUUID()).name("Lectura")
                .category(ActivityCategory.EDUCATION).build();

        entrada = ScheduleEntry.builder()
                .id(idEntrada)
                .child(participante)
                .activity(actividad)
                .dayOfWeek(DayOfWeek.MONDAY)
                .timeSlot(TimeSlot.AFTERNOON)
                .durationMinutes(15)
                .notes("Antes de merendar")
                .build();
    }

    /** Deja la entrada preparada para que updateEntry la encuentre y la guarde. */
    private ScheduleEntryResponse actualizar(ScheduleEntryUpdateRequest solicitud) {
        when(scheduleEntryRepository.findByIdAndChildId(idEntrada, idParticipante))
                .thenReturn(Optional.of(entrada));
        when(scheduleEntryRepository.save(any(ScheduleEntry.class))).thenAnswer(i -> i.getArgument(0));
        when(stepRepository.countByActivityId(any())).thenReturn(0L);
        return scheduleService.updateEntry(idParticipante, idEntrada, solicitud);
    }

    private ScheduleEntryUpdateRequest soloDuracion(Integer duracion) {
        return new ScheduleEntryUpdateRequest(
                null, null, null, null, null, null, null, duracion, null, null);
    }

    @Test
    @DisplayName("una duración en 0 quita el temporizador")
    void duracionCeroDejaLaEntradaSinTemporizador() {
        actualizar(soloDuracion(0));
        assertThat(entrada.getDurationMinutes()).isNull();
    }

    @Test
    @DisplayName("una duración nula deja la que ya estaba")
    void duracionNulaNoPisaLaExistente() {
        actualizar(soloDuracion(null));
        assertThat(entrada.getDurationMinutes()).isEqualTo(15);
    }

    @Test
    @DisplayName("una duración positiva reemplaza a la anterior")
    void duracionPositivaReemplaza() {
        actualizar(soloDuracion(30));
        assertThat(entrada.getDurationMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("los campos que no se mandan quedan como estaban")
    void losCamposAusentesNoSeTocan() {
        actualizar(soloDuracion(0));
        assertThat(entrada.getNotes()).isEqualTo("Antes de merendar");
        assertThat(entrada.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(entrada.getTimeSlot()).isEqualTo(TimeSlot.AFTERNOON);
    }

    @Test
    @DisplayName("una entrada de otro participante no se encuentra")
    void entradaAjenaNoSeEncuentra() {
        when(scheduleEntryRepository.findByIdAndChildId(idEntrada, idParticipante))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.updateEntry(idParticipante, idEntrada, soloDuracion(10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
