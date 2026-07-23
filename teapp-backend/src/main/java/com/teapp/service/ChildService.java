package com.teapp.service;

import com.teapp.dto.child.ChildRequest;
import com.teapp.dto.child.ChildResponse;
import com.teapp.entity.Child;
import com.teapp.entity.User;
import com.teapp.exception.ResourceNotFoundException;
import com.teapp.repository.ChildRepository;
import com.teapp.repository.UserRepository;
import com.teapp.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio para la gestión de perfiles de participantes (niños).
 * Todas las operaciones están acotadas al usuario padre autenticado.
 */
@Service
@RequiredArgsConstructor
public class ChildService {

    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Retorna todos los participantes del padre autenticado.
     *
     * @return lista de perfiles de participantes
     */
    @Transactional(readOnly = true)
    public List<ChildResponse> getAll() {
        UUID idUsuario = securityUtils.idUsuarioActual();
        return childRepository.findAllByUserId(idUsuario).stream()
                .map(this::aRespuesta)
                .toList();
    }

    /**
     * Retorna el perfil de un participante específico, verificando que pertenezca al padre autenticado.
     *
     * @param idParticipante identificador del participante
     * @return datos del perfil
     */
    @Transactional(readOnly = true)
    public ChildResponse getById(UUID idParticipante) {
        return aRespuesta(buscarParticipantePropio(idParticipante));
    }

    /**
     * Crea un nuevo perfil de participante asociado al padre autenticado.
     *
     * @param solicitud datos del nuevo participante
     * @return perfil creado
     */
    @Transactional
    public ChildResponse create(ChildRequest solicitud) {
        UUID idUsuario = securityUtils.idUsuarioActual();
        User padre = userRepository.getReferenceById(idUsuario);

        Child participante = Child.builder()
                .user(padre)
                .name(solicitud.name())
                .dateOfBirth(solicitud.dateOfBirth())
                .avatarColor(solicitud.avatarColor() != null ? solicitud.avatarColor() : "#A8D8EA")
                .notes(solicitud.notes())
                .build();

        return aRespuesta(childRepository.save(participante));
    }

    /**
     * Actualiza el perfil de un participante existente.
     *
     * @param idParticipante identificador del participante
     * @param solicitud      nuevos datos del perfil
     * @return perfil actualizado
     */
    @Transactional
    public ChildResponse update(UUID idParticipante, ChildRequest solicitud) {
        Child participante = buscarParticipantePropio(idParticipante);
        participante.setName(solicitud.name());
        participante.setDateOfBirth(solicitud.dateOfBirth());
        if (solicitud.avatarColor() != null) participante.setAvatarColor(solicitud.avatarColor());
        participante.setNotes(solicitud.notes());
        return aRespuesta(childRepository.save(participante));
    }

    /**
     * Elimina el perfil de un participante y todas sus entradas de agenda en cascada.
     *
     * @param idParticipante identificador del participante a eliminar
     */
    @Transactional
    public void delete(UUID idParticipante) {
        Child participante = buscarParticipantePropio(idParticipante);
        childRepository.delete(participante);
    }

    /**
     * Actualiza la foto de avatar de un participante.
     * Si {@code avatarBase64} es nulo o vacío, elimina el avatar actual.
     *
     * @param idParticipante identificador del participante
     * @param avatarBase64   imagen en formato base64, o null para eliminar
     */
    @Transactional
    public void updateAvatar(UUID idParticipante, String avatarBase64) {
        Child participante = buscarParticipantePropio(idParticipante);
        participante.setAvatarBase64(avatarBase64 == null || avatarBase64.isBlank() ? null : avatarBase64);
        childRepository.save(participante);
    }

    /**
     * Retorna todos los participantes de un padre específico por su ID.
     * Usado por terapeutas para consultar participantes de padres supervisados.
     *
     * @param idPadre identificador del padre
     * @return lista de perfiles de participantes
     */
    @Transactional(readOnly = true)
    public List<ChildResponse> getChildrenByParentId(UUID idPadre) {
        return childRepository.findAllByUserId(idPadre).stream()
                .map(this::aRespuesta)
                .toList();
    }

    // ---- Helpers privados ----

    private Child buscarParticipantePropio(UUID idParticipante) {
        UUID idUsuario = securityUtils.idUsuarioActual();
        return childRepository.findByIdAndUserId(idParticipante, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Niño", idParticipante));
    }

    private ChildResponse aRespuesta(Child participante) {
        return new ChildResponse(
            participante.getId(),
            participante.getName(),
            participante.getDateOfBirth(),
            participante.getAvatarColor(),
            participante.getAvatarBase64(),
            participante.getNotes(),
            participante.getCreatedAt()
        );
    }
}
