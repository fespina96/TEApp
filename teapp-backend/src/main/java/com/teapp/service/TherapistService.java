package com.teapp.service;

import com.teapp.dto.child.ChildResponse;
import com.teapp.dto.schedule.WeeklyScheduleResponse;
import com.teapp.entity.User;
import com.teapp.enums.UserRole;
import com.teapp.exception.ResourceNotFoundException;
import com.teapp.exception.UnauthorizedException;
import com.teapp.repository.ChildRepository;
import com.teapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Servicio para gestionar la vinculación entre padres y terapeutas.
 * Los terapeutas tienen un código de invitación único; los padres lo usan para vincularse.
 */
@Service
@RequiredArgsConstructor
public class TherapistService {

    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final ChildService childService;
    private final ScheduleService scheduleService;

    /**
     * Vincula al padre autenticado con un terapeuta usando su código de invitación.
     * Solo se permite si el padre no tiene ya un terapeuta asignado.
     *
     * @param codigoInvitacion código único del terapeuta (8 caracteres alfanuméricos)
     * @throws UnauthorizedException    si el usuario actual no es padre
     * @throws IllegalArgumentException si ya está vinculado o el código es inválido
     */
    @Transactional
    public void linkToTherapist(String codigoInvitacion) {
        User padre = obtenerUsuarioActual();
        if (padre.getRole() != UserRole.PARENT) {
            throw new UnauthorizedException("Solo los padres pueden vincularse a un terapeuta");
        }
        if (!padre.getTherapists().isEmpty()) {
            throw new IllegalArgumentException("Ya estás vinculado a un terapeuta. Desvincúlate primero.");
        }
        User terapeuta = userRepository.findByInviteCode(codigoInvitacion.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Código de invitación inválido"));
        if (terapeuta.getRole() != UserRole.THERAPIST) {
            throw new IllegalArgumentException("Código de invitación inválido");
        }
        if (!terapeuta.getSupervisedParents().contains(padre)) {
            terapeuta.getSupervisedParents().add(padre);
            userRepository.save(terapeuta);
        }
    }

    /**
     * Desvincula al padre autenticado de un terapeuta específico.
     *
     * @param idTerapeuta identificador del terapeuta del que se desvincula
     */
    @Transactional
    public void unlinkFromTherapist(UUID idTerapeuta) {
        User padre = obtenerUsuarioActual();
        User terapeuta = userRepository.findById(idTerapeuta)
                .orElseThrow(() -> new ResourceNotFoundException("Terapeuta", idTerapeuta));
        terapeuta.getSupervisedParents().remove(padre);
        userRepository.save(terapeuta);
    }

    /**
     * Retorna la lista de padres supervisados por el terapeuta autenticado.
     *
     * @return lista de mapas con id, fullName, email y avatarBase64 de cada padre
     * @throws UnauthorizedException si el usuario actual no es terapeuta
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSupervisedParents() {
        User terapeuta = obtenerUsuarioActual();
        if (terapeuta.getRole() != UserRole.THERAPIST) {
            throw new UnauthorizedException("Solo los terapeutas pueden ver padres supervisados");
        }
        return terapeuta.getSupervisedParents().stream().map(padre -> {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("id", padre.getId());
            datos.put("fullName", padre.getFullName());
            datos.put("email", padre.getEmail());
            datos.put("avatarBase64", padre.getAvatarBase64());
            return datos;
        }).toList();
    }

    /**
     * Retorna la lista de terapeutas vinculados al padre autenticado.
     *
     * @return lista de mapas con id, fullName, email y avatarBase64 de cada terapeuta
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyTherapists() {
        User padre = obtenerUsuarioActual();
        return padre.getTherapists().stream().map(terapeuta -> {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("id", terapeuta.getId());
            datos.put("fullName", terapeuta.getFullName());
            datos.put("email", terapeuta.getEmail());
            datos.put("avatarBase64", terapeuta.getAvatarBase64());
            return datos;
        }).toList();
    }

    /**
     * Retorna los participantes de un padre supervisado.
     *
     * @param idPadre identificador del padre
     * @throws UnauthorizedException si el usuario actual no es un terapeuta vinculado a ese padre
     */
    @Transactional(readOnly = true)
    public List<ChildResponse> getSupervisedChildren(UUID idPadre) {
        verificarSupervision(idPadre);
        return childService.getChildrenByParentId(idPadre);
    }

    /**
     * Retorna la agenda semanal de un participante de un padre supervisado.
     *
     * @param idPadre        identificador del padre supervisado
     * @param idParticipante identificador del participante
     * @throws UnauthorizedException     si el usuario actual no es un terapeuta vinculado a ese padre
     * @throws ResourceNotFoundException si el participante no pertenece a ese padre
     */
    @Transactional(readOnly = true)
    public WeeklyScheduleResponse getSupervisedSchedule(UUID idPadre, UUID idParticipante) {
        verificarSupervision(idPadre);
        childRepository.findByIdAndUserId(idParticipante, idPadre)
                .orElseThrow(() -> new ResourceNotFoundException("Niño", idParticipante));
        return scheduleService.getWeeklySchedule(idParticipante);
    }

    /**
     * Verifica que el usuario autenticado sea un terapeuta con vínculo activo sobre el padre indicado.
     * Sin esta comprobación cualquier usuario podría leer los datos de participantes ajenos.
     */
    private void verificarSupervision(UUID idPadre) {
        User terapeuta = obtenerUsuarioActual();
        if (terapeuta.getRole() != UserRole.THERAPIST) {
            throw new UnauthorizedException("Solo los terapeutas pueden consultar datos de padres supervisados");
        }
        boolean vinculado = terapeuta.getSupervisedParents().stream()
                .anyMatch(padre -> padre.getId().equals(idPadre));
        if (!vinculado) {
            throw new UnauthorizedException("No tenés acceso a los datos de este padre");
        }
    }

    private User obtenerUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", email));
    }
}
