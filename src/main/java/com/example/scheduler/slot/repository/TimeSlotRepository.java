package com.example.scheduler.slot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TimeSlotRepository extends JpaRepository<TimeSlotEntity, UUID> {
    Optional<TimeSlotEntity> findByIdAndCalendarId(UUID id, UUID calendarId);
}
