package com.teapp.repository;

import com.teapp.entity.Activity;
import com.teapp.enums.ActivityCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para la entidad {@link Activity}.
 */
@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    @Query("""
        SELECT DISTINCT a FROM Activity a LEFT JOIN FETCH a.createdBy cb
        WHERE a.predefined = true
           OR cb.id = :userId
           OR (cb.role = com.teapp.enums.UserRole.THERAPIST AND EXISTS (
               SELECT 1 FROM User t JOIN t.supervisedParents p
               WHERE t.id = cb.id AND p.id = :userId))
        """)
    List<Activity> findAvailableForUser(@Param("userId") UUID userId);

    @Query("""
        SELECT DISTINCT a FROM Activity a LEFT JOIN FETCH a.createdBy cb
        WHERE (a.predefined = true
           OR cb.id = :userId
           OR (cb.role = com.teapp.enums.UserRole.THERAPIST AND EXISTS (
               SELECT 1 FROM User t JOIN t.supervisedParents p
               WHERE t.id = cb.id AND p.id = :userId)))
          AND a.category = :category
        """)
    List<Activity> findAvailableForUserAndCategory(@Param("userId") UUID userId, @Param("category") ActivityCategory category);

    List<Activity> findByPredefinedTrue();

    Optional<Activity> findByIdAndCreatedById(UUID id, UUID userId);
}
