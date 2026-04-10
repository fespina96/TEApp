package com.teapp.repository;

import com.teapp.entity.Child;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para la entidad {@link Child}.
 */
@Repository
public interface ChildRepository extends JpaRepository<Child, UUID> {

    List<Child> findAllByUserId(UUID userId);

    Optional<Child> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
