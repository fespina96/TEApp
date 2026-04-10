package com.teapp.repository;

import com.teapp.entity.ScheduleEntry;
import com.teapp.enums.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de acceso a datos para la entidad {@link ScheduleEntry}.
 */
@Repository
public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, UUID> {

    List<ScheduleEntry> findAllByChildIdOrderByDayOfWeekAscTimeSlotAscSortOrderAsc(UUID childId);

    List<ScheduleEntry> findAllByChildIdAndDayOfWeekOrderByTimeSlotAscSortOrderAsc(UUID childId, DayOfWeek dayOfWeek);

    List<ScheduleEntry> findAllByChildIdAndDayOfWeekAndTimeSlotOrderBySortOrderAsc(
        UUID childId, DayOfWeek dayOfWeek, TimeSlot timeSlot);

    Optional<ScheduleEntry> findByIdAndChildId(UUID id, UUID childId);

    @Query("SELECT COALESCE(MAX(se.sortOrder), -1) FROM ScheduleEntry se WHERE se.child.id = :childId AND se.dayOfWeek = :day AND se.timeSlot = :slot")
    int findMaxSortOrder(@Param("childId") UUID childId, @Param("day") DayOfWeek day, @Param("slot") TimeSlot slot);

    boolean existsByActivityId(UUID activityId);
}
