package com.teapp.service;

import com.teapp.dto.completion.CompletionResponse;
import com.teapp.entity.ActivityCompletion;
import com.teapp.entity.Child;
import com.teapp.entity.ScheduleEntry;
import com.teapp.exception.ResourceNotFoundException;
import com.teapp.exception.UnauthorizedException;
import com.teapp.repository.ActivityCompletionRepository;
import com.teapp.repository.ChildRepository;
import com.teapp.repository.ScheduleEntryRepository;
import com.teapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Servicio para registrar y gestionar las completitudes de actividades.
 * Permite marcar, desmarcar y resetear las actividades completadas por un niño.
 */
@Service
@RequiredArgsConstructor
public class CompletionService {

    private final ActivityCompletionRepository completionRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;

    /**
     * Marca una entrada de agenda como completada en una fecha específica.
     * La operación es idempotente: si ya estaba marcada, devuelve el registro existente.
     *
     * @param idParticipante identificador del niño
     * @param idEntrada      identificador de la entrada de agenda
     * @param fecha          fecha en que se realizó la actividad
     * @return datos de la completitud registrada
     */
    @Transactional
    public CompletionResponse markCompleted(UUID idParticipante, UUID idEntrada, LocalDate fecha) {
        verificarAccesoParticipante(idParticipante);

        ScheduleEntry entrada = scheduleEntryRepository.findById(idEntrada)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada de agenda", idEntrada));

        if (!entrada.getChild().getId().equals(idParticipante)) {
            throw new UnauthorizedException("Esta entrada no pertenece al perfil indicado");
        }

        // Idempotente: si ya existe la completitud, devolver la existente
        return completionRepository
                .findByScheduleEntryIdAndCompletedDate(idEntrada, fecha)
                .map(this::aRespuesta)
                .orElseGet(() -> {
                    Child participante = childRepository.getReferenceById(idParticipante);
                    ActivityCompletion completitud = ActivityCompletion.builder()
                            .scheduleEntry(entrada)
                            .child(participante)
                            .completedDate(fecha)
                            .build();
                    return aRespuesta(completionRepository.save(completitud));
                });
    }

    /**
     * Desmarca una entrada de agenda que estaba completada en una fecha específica.
     *
     * @param idParticipante identificador del niño
     * @param idEntrada      identificador de la entrada de agenda
     * @param fecha          fecha de la completitud a eliminar
     */
    @Transactional
    public void unmarkCompleted(UUID idParticipante, UUID idEntrada, LocalDate fecha) {
        verificarAccesoParticipante(idParticipante);
        completionRepository.findByScheduleEntryIdAndCompletedDate(idEntrada, fecha)
                .ifPresent(completionRepository::delete);
    }

    /**
     * Borra todas las completitudes del participante para la semana actual (lunes a domingo).
     *
     * @param idParticipante identificador del niño
     */
    @Transactional
    public void resetCurrentWeek(UUID idParticipante) {
        verificarAccesoParticipante(idParticipante);
        LocalDate lunes = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate domingo = lunes.plusDays(6);
        completionRepository.deleteByChildIdAndCompletedDateBetween(idParticipante, lunes, domingo);
    }

    // ---- Helpers privados ----

    private void verificarAccesoParticipante(UUID idParticipante) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID idUsuario = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", email))
                .getId();
        Child participante = childRepository.findById(idParticipante)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil", idParticipante));
        if (!participante.getUser().getId().equals(idUsuario)) {
            throw new UnauthorizedException("No tenés acceso a este perfil");
        }
    }

    private CompletionResponse aRespuesta(ActivityCompletion completitud) {
        return new CompletionResponse(
            completitud.getId(),
            completitud.getScheduleEntry().getId(),
            completitud.getCompletedDate()
        );
    }
}
