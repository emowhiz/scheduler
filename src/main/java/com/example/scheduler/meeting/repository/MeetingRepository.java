package com.example.scheduler.meeting.repository;

import com.example.scheduler.user.repository.CalendarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MeetingRepository extends JpaRepository<CalendarEntity, UUID> {
}
