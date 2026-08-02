package com.teapp.service;

import com.teapp.dto.activity.ActivityResponse;
import com.teapp.dto.schedule.ScheduleEntryRequest;
import com.teapp.dto.schedule.ScheduleEntryResponse;
import com.teapp.dto.schedule.ScheduleEntryUpdateRequest;
import com.teapp.dto.schedule.WeeklyScheduleResponse;
import com.teapp.entity.Activity;
import com.teapp.entity.Child;
import com.teapp.entity.ScheduleEntry;
import com.teapp.enums.TimeSlot;
import com.teapp.exception.ResourceNotFoundException;
import com.teapp.entity.ActivityCompletion;
import com.teapp.repository.ActivityCompletionRepository;
import com.teapp.repository.ActivityRepository;
import com.teapp.repository.ActivityStepRepository;
import com.teapp.repository.ChildRepository;
import com.teapp.repository.ScheduleEntryRepository;
import com.teapp.repository.UserRepository;
import com.teapp.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de la agenda semanal de los niños.
 * Organiza las entradas por día de la semana y franja horaria,
 * e incluye información de completitudes de la semana actual.
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleEntryRepository scheduleEntryRepository;
    private final ChildRepository childRepository;
    private final ActivityRepository activityRepository;
    private final ActivityStepRepository stepRepository;
    private final UserRepository userRepository;
    private final ActivityCompletionRepository completionRepository;
    private final SecurityUtils securityUtils;

    /**
     * Retorna la agenda semanal completa de un niño, opcionalmente filtrada por día.
     * Verifica que el niño pertenezca al padre autenticado.
     *
     * @param idParticipante identificador del niño
     * @param diaSemana      día para filtrar (null = toda la semana)
     * @return agenda organizada por día y franja horaria
     */
    @Transactional(readOnly = true)
    public WeeklyScheduleResponse getWeeklySchedule(UUID idParticipante, DayOfWeek diaSemana) {
        verificarAccesoParticipante(idParticipante);

        List<ScheduleEntry> entradas = (diaSemana == null)
            ? scheduleEntryRepository.findAllByChildIdOrderByDayOfWeekAscTimeSlotAscSortOrderAsc(idParticipante)
            : scheduleEntryRepository.findAllByChildIdAndDayOfWeekOrderByTimeSlotAscSortOrderAsc(idParticipante, diaSemana);

        return construirRespuestaSemanal(idParticipante, entradas);
    }

    /**
     * Retorna la agenda semanal completa de un niño sin verificar acceso.
     * Usado por terapeutas para consultar agendas de sus padres supervisados.
     *
     * @param idParticipante identificador del niño
     * @return agenda organizada por día y franja horaria
     */
    @Transactional(readOnly = true)
    public WeeklyScheduleResponse getWeeklySchedule(UUID idParticipante) {
        List<ScheduleEntry> entradas =
            scheduleEntryRepository.findAllByChildIdOrderByDayOfWeekAscTimeSlotAscSortOrderAsc(idParticipante);
        return construirRespuestaSemanal(idParticipante, entradas);
    }

    /**
     * Agrega una nueva entrada a la agenda de un niño.
     *
     * @param idParticipante identificador del niño
     * @param solicitud      datos de la nueva entrada
     * @return entrada creada
     */
    @Transactional
    public ScheduleEntryResponse addEntry(UUID idParticipante, ScheduleEntryRequest solicitud) {
        Child participante = verificarAccesoParticipante(idParticipante);

        Activity actividad = activityRepository.findById(solicitud.activityId())
                .orElseThrow(() -> new ResourceNotFoundException("Actividad", solicitud.activityId()));

        int siguienteOrden = solicitud.sortOrder() != null ? solicitud.sortOrder()
                : scheduleEntryRepository.findMaxSortOrder(idParticipante, solicitud.dayOfWeek(), solicitud.timeSlot()) + 1;

        ScheduleEntry entrada = ScheduleEntry.builder()
                .child(participante)
                .activity(actividad)
                .dayOfWeek(solicitud.dayOfWeek())
                .timeSlot(solicitud.timeSlot())
                .startTime(solicitud.startTime())
                .endTime(solicitud.endTime())
                .sortOrder(siguienteOrden)
                .notes(solicitud.notes())
                .durationMinutes(solicitud.durationMinutes())
                .pausable(solicitud.pausable() != null ? solicitud.pausable() : true)
                .requireFullTimer(solicitud.requireFullTimer() != null ? solicitud.requireFullTimer() : false)
                .build();

        return aRespuesta(scheduleEntryRepository.save(entrada));
    }

    /**
     * Actualiza una entrada existente de la agenda.
     *
     * @param idParticipante identificador del niño
     * @param idEntrada      identificador de la entrada a actualizar
     * @param solicitud      nuevos datos de la entrada
     * @return entrada actualizada
     */
    @Transactional
    public ScheduleEntryResponse updateEntry(UUID idParticipante, UUID idEntrada, ScheduleEntryUpdateRequest solicitud) {
        verificarAccesoParticipante(idParticipante);

        ScheduleEntry entrada = scheduleEntryRepository.findByIdAndChildId(idEntrada, idParticipante)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada de agenda", idEntrada));

        if (solicitud.activityId() != null) {
            Activity actividad = activityRepository.findById(solicitud.activityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Actividad", solicitud.activityId()));
            entrada.setActivity(actividad);
        }
        if (solicitud.dayOfWeek() != null) entrada.setDayOfWeek(solicitud.dayOfWeek());
        if (solicitud.timeSlot() != null) entrada.setTimeSlot(solicitud.timeSlot());
        if (solicitud.startTime() != null) entrada.setStartTime(solicitud.startTime());
        if (solicitud.endTime() != null) entrada.setEndTime(solicitud.endTime());
        if (solicitud.sortOrder() != null) entrada.setSortOrder(solicitud.sortOrder());
        if (solicitud.notes() != null) entrada.setNotes(solicitud.notes());
        if (solicitud.durationMinutes() != null) entrada.setDurationMinutes(solicitud.durationMinutes());
        if (solicitud.pausable() != null) entrada.setPausable(solicitud.pausable());
        if (solicitud.requireFullTimer() != null) entrada.setRequireFullTimer(solicitud.requireFullTimer());

        return aRespuesta(scheduleEntryRepository.save(entrada));
    }

    /**
     * Elimina una entrada de la agenda de un niño.
     *
     * @param idParticipante identificador del niño
     * @param idEntrada      identificador de la entrada a eliminar
     */
    @Transactional
    public void deleteEntry(UUID idParticipante, UUID idEntrada) {
        verificarAccesoParticipante(idParticipante);
        ScheduleEntry entrada = scheduleEntryRepository.findByIdAndChildId(idEntrada, idParticipante)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada de agenda", idEntrada));
        scheduleEntryRepository.delete(entrada);
    }

    // ---- Helpers privados ----

    private Child verificarAccesoParticipante(UUID idParticipante) {
        return securityUtils.participanteAccesible(idParticipante);
    }

    private WeeklyScheduleResponse construirRespuestaSemanal(UUID idParticipante, List<ScheduleEntry> entradas) {
        // Una sola query para todas las completitudes de la semana actual
        LocalDate lunes = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate domingo = lunes.plusDays(6);
        List<ActivityCompletion> completitudesSemana =
            completionRepository.findByChildIdAndCompletedDateBetween(idParticipante, lunes, domingo);

        // Mapa idEntrada → lista de fechas completadas esta semana
        Map<UUID, List<LocalDate>> completitudesPorEntrada = completitudesSemana.stream()
            .collect(Collectors.groupingBy(
                c -> c.getScheduleEntry().getId(),
                Collectors.mapping(ActivityCompletion::getCompletedDate, Collectors.toList())
            ));

        Map<DayOfWeek, Map<TimeSlot, List<ScheduleEntryResponse>>> semana = new LinkedHashMap<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            Map<TimeSlot, List<ScheduleEntryResponse>> franjas = new LinkedHashMap<>();
            for (TimeSlot franja : TimeSlot.values()) {
                franjas.put(franja, new ArrayList<>());
            }
            semana.put(dia, franjas);
        }

        for (ScheduleEntry entrada : entradas) {
            List<LocalDate> fechas = completitudesPorEntrada.getOrDefault(entrada.getId(), List.of());
            semana.get(entrada.getDayOfWeek()).get(entrada.getTimeSlot()).add(aRespuesta(entrada, fechas));
        }

        return new WeeklyScheduleResponse(idParticipante, semana);
    }

    private ScheduleEntryResponse aRespuesta(ScheduleEntry entrada, List<LocalDate> fechasCompletadas) {
        Activity actividad = entrada.getActivity();
        int cantidadPasos = (int) stepRepository.countByActivityId(actividad.getId());
        ActivityResponse respuestaActividad = new ActivityResponse(
            actividad.getId(), actividad.getName(), actividad.getDescription(), actividad.getCategory(),
            actividad.getIconName(), actividad.getColor(), actividad.isPredefined(), false,
            actividad.getImageBase64(), actividad.getPictogramUrl(),
            actividad.getDurationMinutes(), actividad.isPausable(), cantidadPasos, actividad.getCreatedAt()
        );
        return new ScheduleEntryResponse(
            entrada.getId(), respuestaActividad, entrada.getDayOfWeek(), entrada.getTimeSlot(),
            entrada.getStartTime(), entrada.getEndTime(), entrada.getSortOrder(), entrada.getNotes(),
            entrada.getDurationMinutes(), entrada.isPausable(), entrada.isRequireFullTimer(),
            fechasCompletadas
        );
    }

    /** Sobrecarga sin fechas completadas para add/update de entrada individual. */
    private ScheduleEntryResponse aRespuesta(ScheduleEntry entrada) {
        return aRespuesta(entrada, List.of());
    }
}
