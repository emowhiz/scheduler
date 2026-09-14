package com.example.scheduler.user.repository;

import com.example.scheduler.slot.repository.TimeSlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CalendarRepository extends JpaRepository<CalendarEntity, UUID> {
}
