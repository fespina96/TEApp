package com.teapp.repository;

import com.teapp.entity.ActivityCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivityCompletionRepository extends JpaRepository<ActivityCompletion, UUID> {

    Optional<ActivityCompletion> findByScheduleEntryIdAndCompletedDate(
            UUID scheduleEntryId, LocalDate date);

    List<ActivityCompletion> findByChildIdAndCompletedDateBetween(
            UUID childId, LocalDate from, LocalDate to);

    List<ActivityCompletion> findByScheduleEntryIdIn(List<UUID> entryIds);

    void deleteByChildIdAndCompletedDateBetween(
            UUID childId, LocalDate from, LocalDate to);
}
