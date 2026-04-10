package com.teapp.repository;

import com.teapp.entity.ActivityStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityStepRepository extends JpaRepository<ActivityStep, UUID> {
    List<ActivityStep> findByActivityIdOrderByStepOrderAsc(UUID activityId);
    long countByActivityId(UUID activityId);
    void deleteByActivityId(UUID activityId);
}
