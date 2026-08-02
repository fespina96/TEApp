package com.teapp.util;

import com.teapp.entity.Child;
import com.teapp.entity.User;
import com.teapp.enums.UserRole;
import com.teapp.exception.ResourceNotFoundException;
import com.teapp.repository.ChildRepository;
import com.teapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;
    private final ChildRepository childRepository;

    public UUID idUsuarioActual() {
        return usuarioActual().getId();
    }

    public User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", email));
    }

    /**
     * Devuelve el participante si el usuario autenticado puede operar sobre él:
     * su padre/tutor, o un terapeuta vinculado a ese padre.
     *
     * <p>Cuando no hay acceso lanza la misma excepción que si el participante no
     * existiera, para no revelar los identificadores de perfiles ajenos.
     *
     * @param idParticipante identificador del participante
     * @return el participante, si el usuario autenticado tiene acceso
     */
    public Child participanteAccesible(UUID idParticipante) {
        Child participante = childRepository.findById(idParticipante)
                .orElseThrow(() -> new ResourceNotFoundException("Niño", idParticipante));

        User actual = usuarioActual();
        UUID idPadre = participante.getUser().getId();

        if (idPadre.equals(actual.getId())) {
            return participante;
        }
        if (actual.getRole() == UserRole.THERAPIST
                && actual.getSupervisedParents().stream()
                         .anyMatch(padre -> padre.getId().equals(idPadre))) {
            return participante;
        }
        throw new ResourceNotFoundException("Niño", idParticipante);
    }
}
