package com.teapp.service;

import com.teapp.dto.activity.ActivityRequest;
import com.teapp.dto.activity.ActivityResponse;
import com.teapp.dto.activity.ActivityStepRequest;
import com.teapp.dto.activity.ActivityStepResponse;
import com.teapp.entity.Activity;
import com.teapp.entity.ActivityStep;
import com.teapp.entity.User;
import com.teapp.enums.ActivityCategory;
import com.teapp.enums.UserRole;
import com.teapp.exception.ResourceNotFoundException;
import com.teapp.exception.UnauthorizedException;
import com.teapp.repository.ActivityRepository;
import com.teapp.repository.ActivityStepRepository;
import com.teapp.repository.ScheduleEntryRepository;
import com.teapp.repository.UserRepository;
import com.teapp.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio para la gestión del catálogo de actividades.
 * Los padres pueden ver las actividades predefinidas más las propias y crear actividades personalizadas.
 */
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityStepRepository stepRepository;
    private final ScheduleEntryRepository scheduleEntryRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Retorna las actividades disponibles para el usuario autenticado
     * (predefinidas + las propias), con filtro opcional por categoría.
     *
     * @param categoria categoría para filtrar, o null para traer todas
     * @return lista de actividades disponibles
     */
    @Transactional(readOnly = true)
    public List<ActivityResponse> getAvailable(ActivityCategory categoria) {
        UUID idUsuario = securityUtils.idUsuarioActual();
        List<Activity> actividades = (categoria == null)
            ? activityRepository.findAvailableForUser(idUsuario)
            : activityRepository.findAvailableForUserAndCategory(idUsuario, categoria);
        return actividades.stream().map(this::aRespuesta).toList();
    }

    /**
     * Retorna únicamente las actividades predefinidas del sistema.
     *
     * @return lista de actividades predefinidas
     */
    @Transactional(readOnly = true)
    public List<ActivityResponse> getPredefined() {
        return activityRepository.findByPredefinedTrue().stream()
                .map(this::aRespuesta).toList();
    }

    /**
     * Crea una nueva actividad personalizada para el padre autenticado.
     *
     * @param solicitud datos de la nueva actividad
     * @return actividad creada
     */
    @Transactional
    public ActivityResponse create(ActivityRequest solicitud) {
        UUID idUsuario = securityUtils.idUsuarioActual();
        User usuario = userRepository.getReferenceById(idUsuario);

        Activity actividad = Activity.builder()
                .name(solicitud.name())
                .description(solicitud.description())
                .category(solicitud.category())
                .iconName(solicitud.iconName())
                .color(solicitud.color() != null ? solicitud.color() : "#F9D8C0")
                .imageBase64(solicitud.imageBase64())
                .pictogramUrl(solicitud.pictogramUrl())
                .durationMinutes(solicitud.durationMinutes())
                .pausable(solicitud.pausable() != null ? solicitud.pausable() : true)
                .predefined(false)
                .createdBy(usuario)
                .build();

        return aRespuesta(activityRepository.save(actividad));
    }

    /**
     * Actualiza una actividad personalizada del padre autenticado.
     *
     * @param idActividad identificador de la actividad a actualizar
     * @param solicitud   nuevos datos de la actividad
     * @return actividad actualizada
     * @throws UnauthorizedException si se intenta modificar una actividad predefinida del sistema
     */
    @Transactional
    public ActivityResponse update(UUID idActividad, ActivityRequest solicitud) {
        UUID idUsuario = securityUtils.idUsuarioActual();
        Activity actividad = activityRepository.findByIdAndCreatedById(idActividad, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad", idActividad));

        if (actividad.isPredefined()) {
            throw new UnauthorizedException("No se pueden modificar actividades predefinidas del sistema");
        }

        actividad.setName(solicitud.name());
        actividad.setDescription(solicitud.description());
        actividad.setCategory(solicitud.category());
        // El ícono sólo se pisa si viene informado: los clientes que no lo envían
        // (la app Android no tiene selector de íconos) no deben borrar el existente.
        if (solicitud.iconName() != null) actividad.setIconName(solicitud.iconName());
        if (solicitud.color() != null) actividad.setColor(solicitud.color());
        actividad.setImageBase64(solicitud.imageBase64());
        actividad.setPictogramUrl(solicitud.pictogramUrl());
        actividad.setDurationMinutes(solicitud.durationMinutes());
        if (solicitud.pausable() != null) actividad.setPausable(solicitud.pausable());

        return aRespuesta(activityRepository.save(actividad));
    }

    /**
     * Elimina una actividad personalizada si no está siendo usada en ninguna agenda.
     *
     * @param idActividad identificador de la actividad a eliminar
     * @throws IllegalArgumentException si la actividad está en uso en alguna agenda
     */
    @Transactional
    public void delete(UUID idActividad) {
        UUID idUsuario = securityUtils.idUsuarioActual();
        Activity actividad = activityRepository.findByIdAndCreatedById(idActividad, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad", idActividad));

        if (scheduleEntryRepository.existsByActivityId(idActividad)) {
            throw new IllegalArgumentException("No se puede eliminar la actividad porque está siendo usada en una agenda");
        }

        activityRepository.delete(actividad);
    }

    /**
     * Retorna los pasos de una actividad ordenados por posición.
     *
     * @param idActividad identificador de la actividad
     * @return lista de pasos ordenados
     */
    @Transactional(readOnly = true)
    public List<ActivityStepResponse> getSteps(UUID idActividad) {
        activityRepository.findById(idActividad)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad", idActividad));
        return stepRepository.findByActivityIdOrderByStepOrderAsc(idActividad)
                .stream().map(this::aPasoRespuesta).toList();
    }

    /**
     * Reemplaza todos los pasos de una actividad con los nuevos datos proporcionados.
     * Solo el dueño de la actividad o cualquier usuario para actividades predefinidas puede hacerlo.
     *
     * @param idActividad   identificador de la actividad
     * @param solicitudes   lista de nuevos pasos (en orden)
     * @return lista de pasos guardados
     */
    @Transactional
    public List<ActivityStepResponse> saveSteps(UUID idActividad, List<ActivityStepRequest> solicitudes) {
        UUID idUsuario = securityUtils.idUsuarioActual();
        Activity actividad = activityRepository.findById(idActividad)
                .orElseThrow(() -> new ResourceNotFoundException("Actividad", idActividad));

        boolean esDueno = actividad.getCreatedBy() != null
                && actividad.getCreatedBy().getId().equals(idUsuario);
        if (!esDueno && !actividad.isPredefined()) {
            throw new UnauthorizedException("No tienes permiso para editar los pasos de esta actividad");
        }

        stepRepository.deleteByActivityId(idActividad);
        List<ActivityStep> pasos = new java.util.ArrayList<>();
        for (int i = 0; i < solicitudes.size(); i++) {
            ActivityStepRequest solicitudPaso = solicitudes.get(i);
            pasos.add(ActivityStep.builder()
                    .activity(actividad)
                    .stepOrder(i)
                    .title(solicitudPaso.title())
                    .description(solicitudPaso.description())
                    .imageBase64(solicitudPaso.imageBase64())
                    .pictogramUrl(solicitudPaso.pictogramUrl())
                    .build());
        }
        return stepRepository.saveAll(pasos).stream().map(this::aPasoRespuesta).toList();
    }

    // ---- Helpers privados ----

    private ActivityStepResponse aPasoRespuesta(ActivityStep paso) {
        return new ActivityStepResponse(
            paso.getId(), paso.getStepOrder(), paso.getTitle(),
            paso.getDescription(), paso.getImageBase64(), paso.getPictogramUrl()
        );
    }

    private ActivityResponse aRespuesta(Activity actividad) {
        boolean creadaPorTerapeuta = actividad.getCreatedBy() != null
                && actividad.getCreatedBy().getRole() == UserRole.THERAPIST;
        int cantidadPasos = (int) stepRepository.countByActivityId(actividad.getId());
        return new ActivityResponse(
            actividad.getId(),
            actividad.getName(),
            actividad.getDescription(),
            actividad.getCategory(),
            actividad.getIconName(),
            actividad.getColor(),
            actividad.isPredefined(),
            creadaPorTerapeuta,
            actividad.getImageBase64(),
            actividad.getPictogramUrl(),
            actividad.getDurationMinutes(),
            actividad.isPausable(),
            cantidadPasos,
            actividad.getCreatedAt()
        );
    }
}
