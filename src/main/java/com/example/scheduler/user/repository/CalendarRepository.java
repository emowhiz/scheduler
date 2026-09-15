package com.example.scheduler.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CalendarRepository extends JpaRepository<CalendarEntity, UUID> {
    Optional<CalendarEntity> findByUserId(UUID userId);
}
